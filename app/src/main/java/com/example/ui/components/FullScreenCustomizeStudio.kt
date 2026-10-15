package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GamepadRepository
import com.example.input.InputDispatchEngine
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.JoystickConfig
import com.example.ui.theme.DarkGreyBorder
import com.example.ui.theme.DarkGreyCard
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.StealthBlack
import com.example.ui.theme.TacticalGreen
import java.util.UUID

@Composable
fun FullScreenCustomizeStudio(
    repository: GamepadRepository,
    engine: InputDispatchEngine,
    onDismiss: () -> Unit
) {
    val activeProfile by repository.activeProfile.collectAsState()
    val initialSnapshot = remember { activeProfile }
    var editingButton by remember { mutableStateOf<GamepadButton?>(null) }
    var editingJoystick by remember { mutableStateOf<Pair<JoystickConfig, Boolean>?>(null) } // Config to isLeft
    var isNewButton by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StealthBlack)
        ) {
            // Full Screen Gamepad Canvas in Edit Mode
            GamepadCanvas(
                profile = activeProfile,
                isEditMode = true,
                engine = engine,
                onToggleEditMode = { /* stays in edit mode */ },
                onCloseOverlay = onDismiss,
                onSelectProfile1 = { repository.selectProfile("profile_1_dual_gamepad") },
                onSelectProfile2 = { repository.selectProfile("profile_2_fps_shooter") },
                onUpdateLeftJoyPos = { nx, ny ->
                    repository.updateLeftJoystick { it.copy(centerXRatio = nx.coerceIn(0.08f, 0.92f), centerYRatio = ny.coerceIn(0.08f, 0.92f)) }
                },
                onUpdateRightJoyPos = { nx, ny ->
                    repository.updateRightJoystick { it.copy(centerXRatio = nx.coerceIn(0.08f, 0.92f), centerYRatio = ny.coerceIn(0.08f, 0.92f)) }
                },
                onUpdateButtonPos = { btnId, nx, ny ->
                    repository.updateButton(btnId) { it.copy(xRatio = nx.coerceIn(0.06f, 0.94f), yRatio = ny.coerceIn(0.06f, 0.94f)) }
                },
                onSaveButton = { updatedBtn ->
                    repository.updateButton(updatedBtn.id) { updatedBtn }
                },
                onDeleteButton = { btnId ->
                    repository.deleteButton(btnId)
                },
                onAddButton = { newBtn ->
                    repository.addButton(newBtn)
                },
                onEditLeftJoyClicked = {
                    editingJoystick = Pair(activeProfile.leftJoystick, true)
                },
                onEditRightJoyClicked = {
                    editingJoystick = Pair(activeProfile.rightJoystick, false)
                },
                showToolbar = false,
                backgroundColor = StealthBlack
            )

            // Top Studio Action Bar (Scrollable horizontally on smaller screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ÖZELLEŞTİRME STÜDYOSU",
                        color = TacticalGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = activeProfile.name,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Joystick Actions
                    if (activeProfile.leftJoystick.enabled) {
                        Button(
                            onClick = { editingJoystick = Pair(activeProfile.leftJoystick, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreyCard, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Sol Stick", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                repository.updateLeftJoystick { it.copy(enabled = true, centerXRatio = 0.20f, centerYRatio = 0.70f) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF142B1C), contentColor = TacticalGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+ Sol Stick", fontSize = 11.sp)
                        }
                    }

                    // Right Joystick Actions
                    if (activeProfile.rightJoystick.enabled) {
                        Button(
                            onClick = { editingJoystick = Pair(activeProfile.rightJoystick, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreyCard, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Sağ Stick", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                repository.updateRightJoystick { it.copy(enabled = true, centerXRatio = 0.80f, centerYRatio = 0.70f) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF142B1C), contentColor = TacticalGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+ Sağ Stick", fontSize = 11.sp)
                        }
                    }

                    // Add Button (Can add unlimited buttons as requested)
                    Button(
                        onClick = {
                            val newBtn = GamepadButton(
                                id = "btn_custom_${UUID.randomUUID().toString().take(6)}",
                                label = "YENİ",
                                xRatio = 0.5f,
                                yRatio = 0.45f,
                                sizeDp = 56f,
                                shape = ButtonShape.CIRCLE,
                                colorHex = 0xFF2A2D35,
                                alpha = 0.85f,
                                actionType = ActionType.KEY_CODE
                            )
                            editingButton = newBtn
                            isNewButton = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252833), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Tuş Ekle", fontSize = 11.sp)
                    }

                    // Undo (Geri Al) Button
                    OutlinedButton(
                        onClick = { repository.revertToSnapshot(initialSnapshot) }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Geri Al", fontSize = 11.sp)
                    }

                    // Reset to Defaults
                    OutlinedButton(
                        onClick = { repository.resetActiveProfileToDefault() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Sıfırla", fontSize = 11.sp)
                    }

                    // Save and Exit
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Kaydet", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Bottom Guidance Tip
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkGreySurface.copy(alpha = 0.94f))
                    .border(1.dp, DarkGreyBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "💡 Kontrolleri sürükleyip taşıyın. Silmek, tuş atamak veya boyut/şeffaflık ayarı için joystick veya tuşa dokunun.",
                    color = Color(0xFFD3D8E2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Joystick Edit / Delete Dialog
            editingJoystick?.let { (joyConfig, isLeft) ->
                JoystickEditDialog(
                    config = joyConfig,
                    isLeft = isLeft,
                    onDismiss = { editingJoystick = null },
                    onSave = { updated ->
                        if (isLeft) {
                            repository.updateLeftJoystick { updated }
                        } else {
                            repository.updateRightJoystick { updated }
                        }
                        editingJoystick = null
                    },
                    onDelete = {
                        if (isLeft) {
                            repository.updateLeftJoystick { it.copy(enabled = false) }
                        } else {
                            repository.updateRightJoystick { it.copy(enabled = false) }
                        }
                        editingJoystick = null
                    }
                )
            }

            // Button Edit / Delete Dialog
            editingButton?.let { btn ->
                ButtonEditDialog(
                    button = btn,
                    onDismiss = { editingButton = null; isNewButton = false },
                    onSave = { updated ->
                        if (isNewButton) {
                            repository.addButton(updated)
                        } else {
                            repository.updateButton(updated.id) { updated }
                        }
                        editingButton = null
                        isNewButton = false
                    },
                    onDelete = { id ->
                        repository.deleteButton(id)
                        editingButton = null
                        isNewButton = false
                    }
                )
            }
        }
    }
}
