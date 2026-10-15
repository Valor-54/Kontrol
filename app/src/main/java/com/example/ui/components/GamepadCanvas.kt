package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.example.input.InputDispatchEngine
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.GamepadProfile
import java.util.UUID

@Composable
fun GamepadCanvas(
    profile: GamepadProfile,
    isEditMode: Boolean,
    engine: InputDispatchEngine,
    onToggleEditMode: () -> Unit,
    onCloseOverlay: () -> Unit,
    onSelectProfile1: () -> Unit = {},
    onSelectProfile2: () -> Unit = {},
    onUpdateLeftJoyPos: (Float, Float) -> Unit,
    onUpdateRightJoyPos: (Float, Float) -> Unit,
    onUpdateButtonPos: (String, Float, Float) -> Unit,
    onSaveButton: (GamepadButton) -> Unit,
    onDeleteButton: (String) -> Unit,
    onAddButton: (GamepadButton) -> Unit,
    onEditLeftJoyClicked: () -> Unit = {},
    onEditRightJoyClicked: () -> Unit = {},
    onResetProfile: () -> Unit = {},
    showToolbar: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    var editingButton by remember { mutableStateOf<GamepadButton?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Left Joystick (if enabled)
        if (profile.leftJoystick.enabled) {
            VirtualJoystick(
                config = profile.leftJoystick,
                isLeft = true,
                isEditMode = isEditMode,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                engine = engine,
                globalAlpha = profile.globalAlpha,
                applyScreenOffset = true,
                onPositionChanged = { nx, ny -> onUpdateLeftJoyPos(nx, ny) },
                onEditClicked = onEditLeftJoyClicked
            )
        }

        // Right Joystick (if enabled)
        if (profile.rightJoystick.enabled) {
            VirtualJoystick(
                config = profile.rightJoystick,
                isLeft = false,
                isEditMode = isEditMode,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                engine = engine,
                globalAlpha = profile.globalAlpha,
                applyScreenOffset = true,
                onPositionChanged = { nx, ny -> onUpdateRightJoyPos(nx, ny) },
                onEditClicked = onEditRightJoyClicked
            )
        }

        // Buttons
        profile.buttons.forEach { button ->
            VirtualButton(
                button = button,
                isEditMode = isEditMode,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                engine = engine,
                globalAlpha = profile.globalAlpha,
                applyScreenOffset = true,
                onPositionChanged = { nx, ny -> onUpdateButtonPos(button.id, nx, ny) },
                onEditClicked = { editingButton = button }
            )
        }

        // Floating Overlay Toolbar
        if (showToolbar) {
            FloatingOverlayToolbar(
                activeProfileId = profile.id,
                onSelectProfile1 = onSelectProfile1,
                onSelectProfile2 = onSelectProfile2,
                isEditMode = isEditMode,
                onToggleEditMode = onToggleEditMode,
                onAddButton = {
                    val newBtn = GamepadButton(
                        id = "btn_custom_${UUID.randomUUID().toString().take(6)}",
                        label = "KEY",
                        xRatio = 0.5f,
                        yRatio = 0.5f,
                        sizeDp = 58f,
                        shape = ButtonShape.CIRCLE,
                        colorHex = 0xFF2D2E36,
                        actionType = ActionType.TOUCH_TAP
                    )
                    onAddButton(newBtn)
                },
                onCloseOverlay = onCloseOverlay
            )
        }

        // Button Customization Dialog
        editingButton?.let { btn ->
            ButtonEditDialog(
                button = btn,
                onDismiss = { editingButton = null },
                onSave = { updated ->
                    onSaveButton(updated)
                    editingButton = null
                },
                onDelete = { id ->
                    onDeleteButton(id)
                    editingButton = null
                }
            )
        }
    }
}
