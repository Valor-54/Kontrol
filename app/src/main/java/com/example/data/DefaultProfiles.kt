package com.example.data

import android.view.KeyEvent
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.GamepadProfile
import com.example.model.JoystickConfig
import com.example.model.JoystickMode

object DefaultProfiles {

    fun getProfile1(): GamepadProfile = createDualGamepadProfile()
    fun getProfile2(): GamepadProfile = createFpsShooterProfile()
    fun getProfile3(): GamepadProfile = createFpsShooterProfile().copy(
        id = "profile_3_retro_arcade",
        name = "Profil 3: Retro Klavye & Arcade",
        description = "4 Yön Ok tuşları ve Arcade aksiyon tuşları"
    )

    fun getAllDefaultProfiles(): List<GamepadProfile> {
        return listOf(
            getProfile1(),
            getProfile2(),
            getProfile3()
        )
    }

    private fun createDualGamepadProfile(): GamepadProfile {
        return GamepadProfile(
            id = "profile_1_dual_gamepad",
            name = "Profil 1: Standart Kumanda & Gamepad",
            description = "Klasik çift analog kumanda düzeni ve aksiyon tuşları",
            leftJoystick = JoystickConfig(
                id = "joy_left",
                label = "L-STICK",
                centerXRatio = 0.22f,
                centerYRatio = 0.65f,
                sizeDp = 145f,
                deadzone = 0.12f,
                sensitivity = 1.0f,
                colorHex = 0xFF00E676,
                alpha = 0.85f,
                enabled = true,
                mode = JoystickMode.ANALOG
            ),
            rightJoystick = JoystickConfig(
                id = "joy_right",
                label = "R-STICK",
                centerXRatio = 0.78f,
                centerYRatio = 0.65f,
                sizeDp = 145f,
                deadzone = 0.12f,
                sensitivity = 1.0f,
                colorHex = 0xFF00E676,
                alpha = 0.85f,
                enabled = true,
                mode = JoystickMode.ANALOG
            ),
            buttons = listOf(
                GamepadButton(
                    id = "btn_a",
                    label = "A",
                    xRatio = 0.88f,
                    yRatio = 0.48f,
                    sizeDp = 58f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_A
                ),
                GamepadButton(
                    id = "btn_b",
                    label = "B",
                    xRatio = 0.94f,
                    yRatio = 0.38f,
                    sizeDp = 58f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_B
                ),
                GamepadButton(
                    id = "btn_x",
                    label = "X",
                    xRatio = 0.82f,
                    yRatio = 0.38f,
                    sizeDp = 58f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_X
                ),
                GamepadButton(
                    id = "btn_y",
                    label = "Y",
                    xRatio = 0.88f,
                    yRatio = 0.28f,
                    sizeDp = 58f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_Y
                ),
                GamepadButton(
                    id = "btn_lb",
                    label = "LB",
                    xRatio = 0.22f,
                    yRatio = 0.22f,
                    sizeDp = 62f,
                    shape = ButtonShape.ROUNDED_RECT,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_L1
                ),
                GamepadButton(
                    id = "btn_rb",
                    label = "RB",
                    xRatio = 0.78f,
                    yRatio = 0.22f,
                    sizeDp = 62f,
                    shape = ButtonShape.ROUNDED_RECT,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_R1
                )
            ),
            globalAlpha = 0.85f,
            showLabels = true,
            vibrationFeedback = true
        )
    }

    private fun createFpsShooterProfile(): GamepadProfile {
        return GamepadProfile(
            id = "profile_2_fps_shooter",
            name = "Profil 2: Klavye & FPS WASD",
            description = "Klavye WASD hareket modu ve tam klavye tuş atamaları",
            leftJoystick = JoystickConfig(
                id = "joy_left",
                label = "WASD",
                centerXRatio = 0.22f,
                centerYRatio = 0.65f,
                sizeDp = 150f,
                deadzone = 0.12f,
                sensitivity = 1.0f,
                colorHex = 0xFF00E676,
                alpha = 0.90f,
                enabled = true,
                mode = JoystickMode.KEYBOARD,
                upKeyCode = KeyEvent.KEYCODE_W,
                upKeyLabel = "W",
                downKeyCode = KeyEvent.KEYCODE_S,
                downKeyLabel = "S",
                leftKeyCode = KeyEvent.KEYCODE_A,
                leftKeyLabel = "A",
                rightKeyCode = KeyEvent.KEYCODE_D,
                rightKeyLabel = "D"
            ),
            rightJoystick = JoystickConfig(
                id = "joy_right",
                label = "BAKIŞ",
                centerXRatio = 0.78f,
                centerYRatio = 0.65f,
                sizeDp = 145f,
                deadzone = 0.12f,
                sensitivity = 1.0f,
                colorHex = 0xFF00E676,
                alpha = 0.85f,
                enabled = true,
                mode = JoystickMode.ANALOG
            ),
            buttons = listOf(
                GamepadButton(
                    id = "btn_space",
                    label = "SPACE",
                    xRatio = 0.88f,
                    yRatio = 0.50f,
                    sizeDp = 62f,
                    shape = ButtonShape.ROUNDED_RECT,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_SPACE
                ),
                GamepadButton(
                    id = "btn_shift",
                    label = "SHIFT",
                    xRatio = 0.10f,
                    yRatio = 0.65f,
                    sizeDp = 58f,
                    shape = ButtonShape.ROUNDED_RECT,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_SHIFT_LEFT
                ),
                GamepadButton(
                    id = "btn_c",
                    label = "C",
                    xRatio = 0.82f,
                    yRatio = 0.38f,
                    sizeDp = 54f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_C
                ),
                GamepadButton(
                    id = "btn_r",
                    label = "R",
                    xRatio = 0.88f,
                    yRatio = 0.32f,
                    sizeDp = 54f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_R
                ),
                GamepadButton(
                    id = "btn_e",
                    label = "E",
                    xRatio = 0.94f,
                    yRatio = 0.38f,
                    sizeDp = 54f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_E
                ),
                GamepadButton(
                    id = "btn_f",
                    label = "F",
                    xRatio = 0.74f,
                    yRatio = 0.46f,
                    sizeDp = 54f,
                    shape = ButtonShape.CIRCLE,
                    colorHex = 0xFF2D2E36,
                    actionType = ActionType.KEY_CODE,
                    keyCode = KeyEvent.KEYCODE_F
                )
            ),
            globalAlpha = 0.85f,
            showLabels = true,
            vibrationFeedback = true
        )
    }
}
