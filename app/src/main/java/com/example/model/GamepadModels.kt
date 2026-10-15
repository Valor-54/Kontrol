package com.example.model

import android.view.KeyEvent

enum class ButtonShape {
    CIRCLE,
    ROUNDED_RECT,
    SQUARE
}

enum class ActionType {
    TOUCH_TAP,
    KEY_CODE,
    TURBO_TAP
}

enum class InputDriverType {
    ACCESSIBILITY,
    SHIZUKU,
    SIMULATED
}

enum class JoystickMode {
    ANALOG,      // Analog stick / touch gesture swipe
    KEYBOARD     // 4-directional PC keyboard keys (WASD, Up/Down/Left/Right, etc.)
}

data class JoystickConfig(
    val id: String,
    val label: String,
    val centerXRatio: Float,
    val centerYRatio: Float,
    val sizeDp: Float = 140f,
    val deadzone: Float = 0.12f,
    val sensitivity: Float = 1.0f,
    val colorHex: Long = 0xFF00F5D4,
    val alpha: Float = 0.8f,
    val targetTouchRadiusRatio: Float = 0.12f,
    val enabled: Boolean = true,
    // Keyboard mode & directional key mapping
    val mode: JoystickMode = JoystickMode.ANALOG,
    val upKeyCode: Int = KeyEvent.KEYCODE_W,
    val upKeyLabel: String = "W",
    val downKeyCode: Int = KeyEvent.KEYCODE_S,
    val downKeyLabel: String = "S",
    val leftKeyCode: Int = KeyEvent.KEYCODE_A,
    val leftKeyLabel: String = "A",
    val rightKeyCode: Int = KeyEvent.KEYCODE_D,
    val rightKeyLabel: String = "D"
)

data class GamepadButton(
    val id: String,
    val label: String,
    val xRatio: Float,
    val yRatio: Float,
    val sizeDp: Float = 60f,
    val shape: ButtonShape = ButtonShape.CIRCLE,
    val colorHex: Long = 0xFF2D2E36, // Neutral gray default as requested
    val alpha: Float = 0.85f,
    val actionType: ActionType = ActionType.KEY_CODE, // Default to sending keyboard/gamepad keycodes
    val keyCode: Int = KeyEvent.KEYCODE_BUTTON_A,
    val targetTouchXRatio: Float = xRatio,
    val targetTouchYRatio: Float = yRatio
)

data class GamepadProfile(
    val id: String,
    val name: String,
    val description: String,
    val leftJoystick: JoystickConfig,
    val rightJoystick: JoystickConfig,
    val buttons: List<GamepadButton>,
    val globalAlpha: Float = 0.85f,
    val showLabels: Boolean = true,
    val vibrationFeedback: Boolean = true
)

data class LiveJoystickState(
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
    val rawDistance: Float = 0f,
    val angleDegrees: Float = 0f,
    val isActive: Boolean = false
)

data class InputTelemetryEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val driver: InputDriverType,
    val eventType: String,
    val pointerId: Int,
    val details: String,
    val isSuccess: Boolean,
    val latencyMs: Long = 0
)
