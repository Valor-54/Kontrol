package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.GamepadRepository
import com.example.input.InputDispatchEngine
import com.example.model.GamepadProfile
import com.example.ui.components.FloatingOverlayToolbar
import com.example.ui.components.VirtualButton
import com.example.ui.components.VirtualJoystick
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class GamepadOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "GamepadOverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "flexpad_overlay_channel"

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_RESTORE_CONTROLS = "com.example.service.action.RESTORE_CONTROLS"

        private var _isRunning = false
        val isRunning: Boolean get() = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun restoreControls(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_RESTORE_CONTROLS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private lateinit var repository: GamepadRepository
    private lateinit var inputEngine: InputDispatchEngine

    private var leftJoyView: View? = null
    private var rightJoyView: View? = null
    private var toolbarView: View? = null
    private var toolbarParams: WindowManager.LayoutParams? = null
    private val buttonViews = mutableListOf<View>()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = GamepadRepository.getInstance(applicationContext)
        inputEngine = InputDispatchEngine(applicationContext)

        _isRunning = true
        repository.setOverlayRunning(true)
        repository.setControlsVisibility(true)
        startForegroundNotification()

        serviceScope.launch {
            repository.activeProfile.collectLatest { profile ->
                rebuildModularControls(profile)
            }
        }

        serviceScope.launch {
            repository.areControlsVisible.collectLatest { visible ->
                val vis = if (visible) View.VISIBLE else View.GONE
                leftJoyView?.visibility = vis
                rightJoyView?.visibility = vis
                buttonViews.forEach { it.visibility = vis }
                if (!visible) {
                    inputEngine.releaseAll("Kontroller gizlendi")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESTORE_CONTROLS -> {
                // Restore controls without restarting service
                repository.setControlsVisibility(true)
                restoreOrRebuildControls(forceRebuild = true)
            }
            else -> {
                // Normal start or open call
                repository.setControlsVisibility(true)
                if (leftJoyView == null && buttonViews.isEmpty()) {
                    restoreOrRebuildControls(forceRebuild = true)
                } else {
                    restoreOrRebuildControls(forceRebuild = false)
                }
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "Screen configuration changed (orientation/size), updating control coordinates")
        // Delay slightly for window manager bounds to update, then adapt controls
        serviceScope.launch {
            delay(150)
            rebuildModularControls(repository.activeProfile.value)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _isRunning = false
        repository.setOverlayRunning(false)
        removeAllViewsSafely(keepToolbar = false)
        serviceScope.cancel()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        appViewModelStore.clear()

        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val channelName = "FlexPad Gamepad Overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aktif sanal oyun kumandası katmanı"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlexPad Kumandası Aktif")
            .setContentText("Yuvarlak yüzen balon ve kontroller ekranda çalışıyor")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getCurrentScreenDimensions(): Pair<Float, Float> {
        val wm = windowManager ?: return Pair(
            resources.displayMetrics.widthPixels.toFloat(),
            resources.displayMetrics.heightPixels.toFloat()
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width().toFloat(), bounds.height().toFloat())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels.toFloat(), dm.heightPixels.toFloat())
        }
    }

    /**
     * Restores control layer visibility and re-attaches any missing views.
     */
    fun restoreOrRebuildControls(forceRebuild: Boolean = false) {
        val controlsVisible = repository.areControlsVisible.value
        if (forceRebuild || leftJoyView == null || buttonViews.isEmpty()) {
            rebuildModularControls(repository.activeProfile.value)
        } else {
            val vis = if (controlsVisible) View.VISIBLE else View.GONE
            leftJoyView?.visibility = vis
            rightJoyView?.visibility = vis
            buttonViews.forEach { it.visibility = vis }
            toolbarView?.visibility = View.VISIBLE
        }
    }

    /**
     * Builds modular floating views so only the controls occupy screen space.
     * ZERO invisible square layer blocking the game!
     * Using strict circular clipping and exact pixel bounds.
     */
    private fun rebuildModularControls(profile: GamepadProfile) {
        removeAllViewsSafely(keepToolbar = true)

        val wm = windowManager ?: return
        val density = resources.displayMetrics.density
        val (screenWidth, screenHeight) = getCurrentScreenDimensions()

        val isVisible = repository.areControlsVisible.value

        // 1. Left Joystick Window (Strictly circular, exact bounds, zero square layer)
        if (profile.leftJoystick.enabled) {
            val joySizePx = (profile.leftJoystick.sizeDp * density).roundToInt()
            val joyCenterXPx = profile.leftJoystick.centerXRatio * screenWidth
            val joyCenterYPx = profile.leftJoystick.centerYRatio * screenHeight
            val leftX = (joyCenterXPx - joySizePx / 2f).roundToInt()
                .coerceIn(0, (screenWidth - joySizePx).toInt().coerceAtLeast(0))
            val leftY = (joyCenterYPx - joySizePx / 2f).roundToInt()
                .coerceIn(0, (screenHeight - joySizePx).toInt().coerceAtLeast(0))
            val actualCenterX = leftX + joySizePx / 2f
            val actualCenterY = leftY + joySizePx / 2f

            val leftParams = createLayoutParams(joySizePx, joySizePx, leftX, leftY)
            val lView = createComposeView {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .size(profile.leftJoystick.sizeDp.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        VirtualJoystick(
                            config = profile.leftJoystick,
                            isLeft = true,
                            isEditMode = false,
                            screenWidthPx = profile.leftJoystick.sizeDp * density,
                            screenHeightPx = profile.leftJoystick.sizeDp * density,
                            engine = inputEngine,
                            globalAlpha = profile.globalAlpha,
                            applyScreenOffset = false,
                            actualScreenCenterX = actualCenterX,
                            actualScreenCenterY = actualCenterY
                        )
                    }
                }
            }

            lView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            lView.clipToOutline = true
            lView.visibility = if (isVisible) View.VISIBLE else View.GONE

            tryAddView(lView, leftParams)
            leftJoyView = lView
        }

        // 2. Right Joystick Window
        if (profile.rightJoystick.enabled) {
            val joySizePx = (profile.rightJoystick.sizeDp * density).roundToInt()
            val joyCenterXPx = profile.rightJoystick.centerXRatio * screenWidth
            val joyCenterYPx = profile.rightJoystick.centerYRatio * screenHeight
            val rightX = (joyCenterXPx - joySizePx / 2f).roundToInt()
                .coerceIn(0, (screenWidth - joySizePx).toInt().coerceAtLeast(0))
            val rightY = (joyCenterYPx - joySizePx / 2f).roundToInt()
                .coerceIn(0, (screenHeight - joySizePx).toInt().coerceAtLeast(0))
            val actualCenterX = rightX + joySizePx / 2f
            val actualCenterY = rightY + joySizePx / 2f

            val rightParams = createLayoutParams(joySizePx, joySizePx, rightX, rightY)
            val rView = createComposeView {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .size(profile.rightJoystick.sizeDp.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        VirtualJoystick(
                            config = profile.rightJoystick,
                            isLeft = false,
                            isEditMode = false,
                            screenWidthPx = profile.rightJoystick.sizeDp * density,
                            screenHeightPx = profile.rightJoystick.sizeDp * density,
                            engine = inputEngine,
                            globalAlpha = profile.globalAlpha,
                            applyScreenOffset = false,
                            actualScreenCenterX = actualCenterX,
                            actualScreenCenterY = actualCenterY
                        )
                    }
                }
            }

            rView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            rView.clipToOutline = true
            rView.visibility = if (isVisible) View.VISIBLE else View.GONE

            tryAddView(rView, rightParams)
            rightJoyView = rView
        }

        // 3. Modular Action Buttons (Individual tight floating windows)
        profile.buttons.forEach { button ->
            val btnSizePx = (button.sizeDp * density).roundToInt()
            val btnCenterXPx = button.xRatio * screenWidth
            val btnCenterYPx = button.yRatio * screenHeight
            val btnX = (btnCenterXPx - btnSizePx / 2f).roundToInt()
                .coerceIn(0, (screenWidth - btnSizePx).toInt().coerceAtLeast(0))
            val btnY = (btnCenterYPx - btnSizePx / 2f).roundToInt()
                .coerceIn(0, (screenHeight - btnSizePx).toInt().coerceAtLeast(0))
            val actualCenterX = btnX + btnSizePx / 2f
            val actualCenterY = btnY + btnSizePx / 2f

            val btnParams = createLayoutParams(btnSizePx, btnSizePx, btnX, btnY)
            val bView = createComposeView {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier.size(button.sizeDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VirtualButton(
                            button = button,
                            isEditMode = false,
                            screenWidthPx = button.sizeDp * density,
                            screenHeightPx = button.sizeDp * density,
                            engine = inputEngine,
                            globalAlpha = profile.globalAlpha,
                            applyScreenOffset = false,
                            actualScreenCenterX = actualCenterX,
                            actualScreenCenterY = actualCenterY
                        )
                    }
                }
            }
            bView.visibility = if (isVisible) View.VISIBLE else View.GONE
            tryAddView(bView, btnParams)
            buttonViews.add(bView)
        }

        // 4. Floating Round Balloon (Küçük Yuvarlak Balon)
        val balloonSizePx = (52 * density).roundToInt()
        val curX = toolbarParams?.x ?: (screenWidth * 0.05f).roundToInt()
            .coerceIn(0, (screenWidth - balloonSizePx).toInt().coerceAtLeast(0))
        val curY = toolbarParams?.y ?: (screenHeight * 0.12f).roundToInt()
            .coerceIn(0, (screenHeight - balloonSizePx).toInt().coerceAtLeast(0))

        if (toolbarView == null) {
            val tbParams = createLayoutParams(
                balloonSizePx,
                balloonSizePx,
                curX,
                curY
            )
            toolbarParams = tbParams

            val tView = createComposeView {
                MyApplicationTheme {
                    val controlsVisible by repository.areControlsVisible.collectAsState()
                    FloatingOverlayToolbar(
                        activeProfileId = profile.id,
                        areControlsVisible = controlsVisible,
                        onToggleControlsVisibility = { repository.toggleControlsVisibility() },
                        onSelectProfile1 = { repository.selectProfile("profile_1_dual_gamepad") },
                        onSelectProfile2 = { repository.selectProfile("profile_2_fps_shooter") },
                        isEditMode = false,
                        onToggleEditMode = {
                            val appIntent = Intent(this@GamepadOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(appIntent)
                        },
                        onAddButton = {},
                        onCloseOverlay = { stopSelf() },
                        onDragDelta = { dx, dy ->
                            toolbarParams?.let { params ->
                                val (sw, sh) = getCurrentScreenDimensions()
                                params.x = (params.x + dx.roundToInt()).coerceIn(0, (sw - balloonSizePx).toInt().coerceAtLeast(0))
                                params.y = (params.y + dy.roundToInt()).coerceIn(0, (sh - balloonSizePx).toInt().coerceAtLeast(0))
                                try {
                                    windowManager?.updateViewLayout(toolbarView, params)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error updating floating balloon position", e)
                                }
                            }
                        },
                        onExpandedChanged = { expanded ->
                            toolbarParams?.let { params ->
                                if (expanded) {
                                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                                    toolbarView?.outlineProvider = object : ViewOutlineProvider() {
                                        override fun getOutline(view: View, outline: Outline) {
                                            outline.setRoundRect(0, 0, view.width, view.height, 20f * density)
                                        }
                                    }
                                } else {
                                    params.width = balloonSizePx
                                    params.height = balloonSizePx
                                    toolbarView?.outlineProvider = object : ViewOutlineProvider() {
                                        override fun getOutline(view: View, outline: Outline) {
                                            outline.setOval(0, 0, view.width, view.height)
                                        }
                                    }
                                }
                                try {
                                    windowManager?.updateViewLayout(toolbarView, params)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error resizing floating toolbar params", e)
                                }
                            }
                        }
                    )
                }
            }

            tView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            tView.clipToOutline = true

            tryAddView(tView, tbParams)
            toolbarView = tView
        } else {
            // Balloon already exists; adjust position to stay on screen
            toolbarParams?.let { params ->
                params.x = params.x.coerceIn(0, (screenWidth - balloonSizePx).toInt().coerceAtLeast(0))
                params.y = params.y.coerceIn(0, (screenHeight - balloonSizePx).toInt().coerceAtLeast(0))
                try {
                    windowManager?.updateViewLayout(toolbarView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating existing toolbar view layout", e)
                }
            }
        }
    }

    private fun createLayoutParams(width: Int, height: Int, x: Int, y: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun createComposeView(content: @androidx.compose.runtime.Composable () -> Unit): ComposeView {
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GamepadOverlayService)
            setViewTreeViewModelStoreOwner(this@GamepadOverlayService)
            setViewTreeSavedStateRegistryOwner(this@GamepadOverlayService)
            setBackgroundColor(0) // 100% transparent background
            setContent(content)
        }
    }

    private fun tryAddView(view: ComposeView, params: WindowManager.LayoutParams) {
        try {
            windowManager?.addView(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding modular floating view", e)
        }
    }

    private fun removeAllViewsSafely(keepToolbar: Boolean = false) {
        val wm = windowManager ?: return
        leftJoyView?.let {
            try {
                if (it.isAttachedToWindow) wm.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "leftJoyView remove failed", e)
            }
        }
        leftJoyView = null

        rightJoyView?.let {
            try {
                if (it.isAttachedToWindow) wm.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "rightJoyView remove failed", e)
            }
        }
        rightJoyView = null

        buttonViews.forEach { v ->
            try {
                if (v.isAttachedToWindow) wm.removeView(v)
            } catch (e: Exception) {
                Log.w(TAG, "buttonView remove failed", e)
            }
        }
        buttonViews.clear()

        if (!keepToolbar) {
            toolbarView?.let {
                try {
                    if (it.isAttachedToWindow) wm.removeView(it)
                } catch (e: Exception) {
                    Log.w(TAG, "toolbarView remove failed", e)
                }
            }
            toolbarView = null
            toolbarParams = null
        }
    }
}
