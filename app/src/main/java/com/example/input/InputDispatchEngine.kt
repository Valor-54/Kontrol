package com.example.input

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.SystemClock
import android.util.Log
import com.example.model.ActionType
import com.example.model.GamepadButton
import com.example.model.InputDriverType
import com.example.model.InputTelemetryEvent
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import com.example.model.KeyboardKeys
import com.example.model.LiveJoystickState
import com.example.service.GamepadAccessibilityService
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class PointerState(
    val pointerId: Int,
    val controlId: String,
    val startX: Float,
    val startY: Float,
    var currentX: Float,
    var currentY: Float,
    val startTime: Long = SystemClock.uptimeMillis()
)

class InputDispatchEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val activePointers = ConcurrentHashMap<Int, PointerState>()
    private val activeJoystickKeys = ConcurrentHashMap<Boolean, MutableSet<Int>>()

    private val _telemetryLogs = MutableStateFlow<List<InputTelemetryEvent>>(emptyList())
    val telemetryLogs: StateFlow<List<InputTelemetryEvent>> = _telemetryLogs.asStateFlow()

    private val _liveLeftJoystick = MutableStateFlow(LiveJoystickState())
    val liveLeftJoystick: StateFlow<LiveJoystickState> = _liveLeftJoystick.asStateFlow()

    private val _liveRightJoystick = MutableStateFlow(LiveJoystickState())
    val liveRightJoystick: StateFlow<LiveJoystickState> = _liveRightJoystick.asStateFlow()

    private val _pressedButtons = MutableStateFlow<Set<String>>(emptySet())
    val pressedButtons: StateFlow<Set<String>> = _pressedButtons.asStateFlow()

    private val _activePointerIds = MutableStateFlow<List<Int>>(emptyList())
    val activePointerIds: StateFlow<List<Int>> = _activePointerIds.asStateFlow()

    private var activeDriver = InputDriverType.ACCESSIBILITY

    fun setDriver(driver: InputDriverType) {
        if (activeDriver != driver) {
            releaseAll("Driver changed to ${driver.name}")
            activeDriver = driver
        }
    }

    fun getDriver(): InputDriverType = activeDriver

    // -------------------------------------------------------------
    // Joystick Processing
    // -------------------------------------------------------------

    fun onJoystickDown(
        isLeft: Boolean,
        pointerId: Int,
        centerX: Float,
        centerY: Float
    ) {
        val controlId = if (isLeft) "joy_left" else "joy_right"
        activePointers[pointerId] = PointerState(
            pointerId = pointerId,
            controlId = controlId,
            startX = centerX,
            startY = centerY,
            currentX = centerX,
            currentY = centerY
        )
        updateActivePointerList()

        if (isLeft) {
            _liveLeftJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, true)
        } else {
            _liveRightJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, true)
        }

        logEvent(
            driver = activeDriver,
            eventType = "DOWN",
            pointerId = pointerId,
            details = "$controlId pressed at (${centerX.toInt()}, ${centerY.toInt()})",
            isSuccess = true
        )
    }

    fun onJoystickMove(
        isLeft: Boolean,
        pointerId: Int,
        deltaX: Float,
        deltaY: Float,
        maxRadius: Float,
        deadzone: Float = 0.12f,
        sensitivity: Float = 1.0f,
        screenCenterX: Float = 0f,
        screenCenterY: Float = 0f
    ) {
        val defaultConfig = JoystickConfig(
            id = if (isLeft) "joy_left" else "joy_right",
            label = if (isLeft) "L" else "R",
            centerXRatio = 0.5f,
            centerYRatio = 0.5f,
            deadzone = deadzone,
            sensitivity = sensitivity
        )
        onJoystickMove(defaultConfig, isLeft, pointerId, deltaX, deltaY, maxRadius, screenCenterX, screenCenterY)
    }

    fun onJoystickMove(
        config: JoystickConfig,
        isLeft: Boolean,
        pointerId: Int,
        deltaX: Float,
        deltaY: Float,
        maxRadius: Float,
        screenCenterX: Float,
        screenCenterY: Float
    ) {
        val dist = hypot(deltaX, deltaY)
        val normalizedDist = (dist / maxRadius).coerceIn(0f, 1f)
        val angleRad = atan2(deltaY, deltaX)
        val angleDeg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 360f) % 360f

        val effectiveDist: Float
        val normX: Float
        val normY: Float

        if (normalizedDist < config.deadzone) {
            effectiveDist = 0f
            normX = 0f
            normY = 0f
        } else {
            val scaledDist = ((normalizedDist - config.deadzone) / (1f - config.deadzone)) * config.sensitivity
            effectiveDist = scaledDist.coerceIn(0f, 1f)
            normX = (cos(angleRad) * effectiveDist).coerceIn(-1f, 1f)
            normY = (sin(angleRad) * effectiveDist).coerceIn(-1f, 1f)
        }

        val joyState = LiveJoystickState(
            normalizedX = normX,
            normalizedY = normY,
            rawDistance = effectiveDist,
            angleDegrees = angleDeg,
            isActive = true
        )

        if (isLeft) {
            _liveLeftJoystick.value = joyState
        } else {
            _liveRightJoystick.value = joyState
        }

        val state = activePointers[pointerId]
        if (state != null) {
            state.currentX = screenCenterX + (deltaX.coerceIn(-maxRadius, maxRadius))
            state.currentY = screenCenterY + (deltaY.coerceIn(-maxRadius, maxRadius))
        }

        // Branch 1: Keyboard Mode (WASD / 4-directional key signals)
        if (config.mode == JoystickMode.KEYBOARD) {
            handleKeyboardJoystickMotion(config, isLeft, normX, normY, effectiveDist)
            return
        }

        // Branch 2: Analog Mode (Touch Gesture Swipe / Analog Motion)
        if (effectiveDist > 0.05f) {
            dispatchJoystickMotion(
                isLeft = isLeft,
                pointerId = pointerId,
                startX = screenCenterX,
                startY = screenCenterY,
                targetX = screenCenterX + normX * maxRadius,
                targetY = screenCenterY + normY * maxRadius
            )
        }
    }

    private fun handleKeyboardJoystickMotion(
        config: JoystickConfig,
        isLeft: Boolean,
        normX: Float,
        normY: Float,
        effectiveDist: Float
    ) {
        val newKeys = mutableSetOf<Int>()

        // Check 4 directional thresholds (supports diagonals like W+D, W+A, S+D, S+A)
        if (effectiveDist > config.deadzone) {
            val threshold = 0.32f
            if (normY < -threshold) newKeys.add(config.upKeyCode)      // Up (e.g. W)
            if (normY > threshold) newKeys.add(config.downKeyCode)     // Down (e.g. S)
            if (normX < -threshold) newKeys.add(config.leftKeyCode)    // Left (e.g. A)
            if (normX > threshold) newKeys.add(config.rightKeyCode)   // Right (e.g. D)
        }

        val stickKeys = activeJoystickKeys.getOrPut(isLeft) { mutableSetOf() }
        val toRelease = stickKeys - newKeys
        val toPress = newKeys - stickKeys

        for (code in toRelease) {
            dispatchKeyEvent(code, isDown = false)
        }
        for (code in toPress) {
            dispatchKeyEvent(code, isDown = true)
        }

        stickKeys.clear()
        stickKeys.addAll(newKeys)
    }

    fun onJoystickUp(isLeft: Boolean, pointerId: Int) {
        val controlId = if (isLeft) "joy_left" else "joy_right"
        activePointers.remove(pointerId)
        updateActivePointerList()

        // Release any held keyboard keys for this stick
        val stickKeys = activeJoystickKeys[isLeft]
        if (stickKeys != null && stickKeys.isNotEmpty()) {
            for (code in stickKeys) {
                dispatchKeyEvent(code, isDown = false)
            }
            stickKeys.clear()
        }

        if (isLeft) {
            _liveLeftJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, false)
        } else {
            _liveRightJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, false)
        }

        logEvent(
            driver = activeDriver,
            eventType = "UP",
            pointerId = pointerId,
            details = "$controlId released and reset to center",
            isSuccess = true
        )
    }

    // -------------------------------------------------------------
    // Button Processing
    // -------------------------------------------------------------

    fun onButtonDown(
        button: GamepadButton,
        pointerId: Int,
        screenX: Float,
        screenY: Float
    ) {
        activePointers[pointerId] = PointerState(
            pointerId = pointerId,
            controlId = button.id,
            startX = screenX,
            startY = screenY,
            currentX = screenX,
            currentY = screenY
        )
        updateActivePointerList()

        val set = _pressedButtons.value.toMutableSet()
        set.add(button.id)
        _pressedButtons.value = set

        if (button.actionType == ActionType.KEY_CODE) {
            dispatchKeyEvent(button.keyCode, isDown = true)
        } else {
            dispatchButtonDown(button, pointerId, screenX, screenY)
        }
    }

    fun onButtonUp(button: GamepadButton, pointerId: Int) {
        activePointers.remove(pointerId)
        updateActivePointerList()

        val set = _pressedButtons.value.toMutableSet()
        set.remove(button.id)
        _pressedButtons.value = set

        if (button.actionType == ActionType.KEY_CODE) {
            dispatchKeyEvent(button.keyCode, isDown = false)
        } else {
            logEvent(
                driver = activeDriver,
                eventType = "UP",
                pointerId = pointerId,
                details = "Button ${button.label} released",
                isSuccess = true
            )
        }
    }

    // -------------------------------------------------------------
    // Keyboard Key Event Dispatch
    // -------------------------------------------------------------

    fun dispatchKeyEvent(keyCode: Int, isDown: Boolean) {
        val label = KeyboardKeys.findLabel(keyCode)
        val startTime = SystemClock.uptimeMillis()

        when (activeDriver) {
            InputDriverType.SHIZUKU -> {
                if (isDown) {
                    val cmd = "input keyevent $keyCode"
                    val success = ShizukuManager.sendFastShellCommand(cmd)
                    val latency = SystemClock.uptimeMillis() - startTime
                    logEvent(
                        driver = activeDriver,
                        eventType = "KEY_DOWN",
                        pointerId = 0,
                        details = "Shizuku [$cmd] ($label)",
                        isSuccess = success,
                        latencyMs = latency
                    )
                } else {
                    val latency = SystemClock.uptimeMillis() - startTime
                    logEvent(
                        driver = activeDriver,
                        eventType = "KEY_UP",
                        pointerId = 0,
                        details = "Shizuku Key $label bırakıldı",
                        isSuccess = true,
                        latencyMs = latency
                    )
                }
            }
            InputDriverType.ACCESSIBILITY, InputDriverType.SIMULATED -> {
                if (isDown) {
                    try {
                        Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent $keyCode"))
                    } catch (_: Exception) {
                        // In-app fallback
                    }
                }
                val latency = SystemClock.uptimeMillis() - startTime
                logEvent(
                    driver = activeDriver,
                    eventType = if (isDown) "KEY_DOWN" else "KEY_UP",
                    pointerId = 0,
                    details = "Key $label (kod $keyCode) [${if (isDown) "BASILDI" else "BIRAKILDI"}]",
                    isSuccess = true,
                    latencyMs = latency
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Touch Tap Dispatch Logic
    // -------------------------------------------------------------

    private fun dispatchButtonDown(
        button: GamepadButton,
        pointerId: Int,
        screenX: Float,
        screenY: Float
    ) {
        val startTime = SystemClock.uptimeMillis()

        when (activeDriver) {
            InputDriverType.ACCESSIBILITY -> {
                if (!GamepadAccessibilityService.isConnected()) {
                    logEvent(
                        driver = activeDriver,
                        eventType = "TAP",
                        pointerId = pointerId,
                        details = "Erişilebilirlik Servisi açık değil. Dokunma gönderilemedi.",
                        isSuccess = false
                    )
                    return
                }

                val path = Path()
                path.moveTo(screenX, screenY)
                path.lineTo(screenX + 1f, screenY + 1f)

                val stroke = GestureDescription.StrokeDescription(path, 0, 40)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()

                GamepadAccessibilityService.dispatchGesture(
                    gesture,
                    object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            val latency = SystemClock.uptimeMillis() - startTime
                            logEvent(
                                driver = activeDriver,
                                eventType = "TAP",
                                pointerId = pointerId,
                                details = "Dokunma '${button.label}' (${screenX.toInt()}, ${screenY.toInt()})",
                                isSuccess = true,
                                latencyMs = latency
                            )
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            logEvent(
                                driver = activeDriver,
                                eventType = "CANCEL",
                                pointerId = pointerId,
                                details = "Dokunma '${button.label}' sistemce iptal edildi",
                                isSuccess = false
                            )
                        }
                    }
                )
            }

            InputDriverType.SHIZUKU -> {
                val cmd = when (button.actionType) {
                    ActionType.TOUCH_TAP -> "input tap ${screenX.toInt()} ${screenY.toInt()}"
                    ActionType.KEY_CODE -> "input keyevent ${button.keyCode}"
                    ActionType.TURBO_TAP -> "input tap ${screenX.toInt()} ${screenY.toInt()}; input tap ${screenX.toInt()} ${screenY.toInt()}"
                }
                val success = ShizukuManager.sendFastShellCommand(cmd)
                val latency = SystemClock.uptimeMillis() - startTime
                logEvent(
                    driver = activeDriver,
                    eventType = if (button.actionType == ActionType.KEY_CODE) "KEY" else "TAP",
                    pointerId = pointerId,
                    details = "Shizuku [$cmd] (${button.label})",
                    isSuccess = success,
                    latencyMs = latency
                )
            }

            InputDriverType.SIMULATED -> {
                val latency = SystemClock.uptimeMillis() - startTime
                logEvent(
                    driver = activeDriver,
                    eventType = "SIMULATED",
                    pointerId = pointerId,
                    details = "Simüle '${button.label}' (${button.actionType}) (${screenX.toInt()}, ${screenY.toInt()})",
                    isSuccess = true,
                    latencyMs = latency
                )
            }
        }
    }

    private var lastJoystickDispatchTime = 0L

    private fun dispatchJoystickMotion(
        isLeft: Boolean,
        pointerId: Int,
        startX: Float,
        startY: Float,
        targetX: Float,
        targetY: Float
    ) {
        val now = SystemClock.uptimeMillis()
        if (now - lastJoystickDispatchTime < 28L) return
        lastJoystickDispatchTime = now

        when (activeDriver) {
            InputDriverType.ACCESSIBILITY -> {
                if (!GamepadAccessibilityService.isConnected()) return

                val path = Path()
                path.moveTo(startX, startY)
                path.lineTo(targetX, targetY)

                val stroke = GestureDescription.StrokeDescription(path, 0, 30, false)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()

                GamepadAccessibilityService.dispatchGesture(gesture)
            }

            InputDriverType.SHIZUKU -> {
                val cmd = "input swipe ${startX.toInt()} ${startY.toInt()} ${targetX.toInt()} ${targetY.toInt()} 30"
                ShizukuManager.sendFastShellCommand(cmd)
            }

            InputDriverType.SIMULATED -> {}
        }
    }

    fun releaseAll(reason: String = "Tüm girişler sıfırlandı") {
        activePointers.clear()
        updateActivePointerList()

        for ((_, keys) in activeJoystickKeys) {
            for (code in keys) {
                dispatchKeyEvent(code, isDown = false)
            }
            keys.clear()
        }

        _liveLeftJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, false)
        _liveRightJoystick.value = LiveJoystickState(0f, 0f, 0f, 0f, false)
        _pressedButtons.value = emptySet()

        logEvent(
            driver = activeDriver,
            eventType = "RELEASE_ALL",
            pointerId = 0,
            details = reason,
            isSuccess = true
        )
    }

    private fun updateActivePointerList() {
        _activePointerIds.value = activePointers.keys().toList()
    }

    private fun logEvent(
        driver: InputDriverType,
        eventType: String,
        pointerId: Int,
        details: String,
        isSuccess: Boolean,
        latencyMs: Long = 0
    ) {
        val event = InputTelemetryEvent(
            driver = driver,
            eventType = eventType,
            pointerId = pointerId,
            details = details,
            isSuccess = isSuccess,
            latencyMs = latencyMs
        )
        val current = _telemetryLogs.value.toMutableList()
        current.add(0, event)
        if (current.size > 80) current.removeAt(current.size - 1)
        _telemetryLogs.value = current
    }

    companion object {
        @Volatile
        private var INSTANCE: InputDispatchEngine? = null

        fun getInstance(context: Context): InputDispatchEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InputDispatchEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
