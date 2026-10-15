package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class GamepadAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isServiceRunning.value = true
        Log.i(TAG, "GamepadAccessibilityService connected and ready for gesture dispatching")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Exclusively used for gesture injection into active games
    }

    override fun onInterrupt() {
        Log.w(TAG, "GamepadAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef = null
        _isServiceRunning.value = false
        Log.i(TAG, "GamepadAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "GamepadA11yService"
        private var instanceRef: WeakReference<GamepadAccessibilityService>? = null

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun isConnected(): Boolean {
            return instanceRef?.get() != null
        }

        /**
         * Checks if the Accessibility Service is enabled in system settings,
         * ensuring the UI doesn't erroneously show "İzin Ver" when the user already enabled it.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isConnected()) return true
            return try {
                val expectedComponentName = ComponentName(context, GamepadAccessibilityService::class.java).flattenToString()
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                Log.w(TAG, "Could not check accessibility setting", e)
                false
            }
        }

        fun dispatchGesture(
            gesture: GestureDescription,
            callback: GestureResultCallback? = null
        ): Boolean {
            val service = instanceRef?.get() ?: return false
            return try {
                service.dispatchGesture(gesture, callback, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch gesture", e)
                false
            }
        }
    }
}
