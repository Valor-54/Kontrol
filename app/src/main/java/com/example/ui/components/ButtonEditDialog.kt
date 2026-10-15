package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.KeyboardKeys
import com.example.ui.theme.TacticalGreen

@Composable
fun ButtonEditDialog(
    button: GamepadButton,
    onDismiss: () -> Unit,
    onSave: (GamepadButton) -> Unit,
    onDelete: (String) -> Unit
) {
    var label by remember { mutableStateOf(button.label) }
    var sizeDp by remember { mutableFloatStateOf(button.sizeDp) }
    var alpha by remember { mutableFloatStateOf(button.alpha) }
    var selectedShape by remember { mutableStateOf(button.shape) }
    var selectedAction by remember { mutableStateOf(button.actionType) }
    var selectedKeyCode by remember { mutableIntStateOf(button.keyCode) }
    var selectedColorHex by remember { mutableStateOf(button.colorHex) }

    var showKeyPicker by remember { mutableStateOf(false) }

    // Color presets: Neutral matte grays and tactical green accents prioritized
    val colorPresets = listOf(
        0xFF2A2D35, // Matte Dark Grey (Standard)
        0xFF3B3E4A, // Slate Grey
        0xFF1A1C22, // Carbon Black
        0xFF4B4F5E, // Light Charcoal
        0xFF00E676, // Tactical Green (Accent)
        0xFF10B981, // Emerald Green
        0xFFFF3D00, // Tactical Amber/Red
        0xFFFFD600  // Tactical Yellow
    )

    val currentKeyLabel = remember(selectedKeyCode) {
        KeyboardKeys.findLabel(selectedKeyCode)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Tuş Özelleştir: ${button.label}",
                style = MaterialTheme.typography.titleLarge,
                color = TacticalGreen,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Etiket
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(8) },
                    label = { Text("Ekranda Görünen Ad (Örn: W, SPACE, FIRE)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Giriş Türü (Aksiyon)
                Text("Giriş Türü (Sinyal)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionType.values().forEach { act ->
                        FilterChip(
                            selected = selectedAction == act,
                            onClick = { selectedAction = act },
                            label = {
                                Text(
                                    when (act) {
                                        ActionType.KEY_CODE -> "⌨️ Klavye / Kumanda Tuşu"
                                        ActionType.TOUCH_TAP -> "👆 Ekrana Dokunma"
                                        ActionType.TURBO_TAP -> "⚡ Turbo Seri Dokunma"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // KeyCode Seçimi (Tüm bilgisayar klavyesi tuşları)
                if (selectedAction == ActionType.KEY_CODE) {
                    Text("Bilgisayar Klavye Tuş Ataması", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF14171E))
                            .border(1.5.dp, TacticalGreen, RoundedCornerShape(10.dp))
                            .clickable { showKeyPicker = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Atanan Tuş:", color = Color(0xFF8892A8), fontSize = 11.sp)
                                Text(
                                    text = currentKeyLabel,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Button(
                                onClick = { showKeyPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tuş Değiştir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Şekil
                Text("Şekil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonShape.values().forEach { shape ->
                        FilterChip(
                            selected = selectedShape == shape,
                            onClick = { selectedShape = shape },
                            label = {
                                Text(
                                    when (shape) {
                                        ButtonShape.CIRCLE -> "Daire"
                                        ButtonShape.ROUNDED_RECT -> "Oval / Dikdörtgen"
                                        ButtonShape.SQUARE -> "Kare"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // Boyut Slider (35dp - 130dp)
                Text("Boyut: ${sizeDp.toInt()} dp", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = sizeDp,
                    onValueChange = { sizeDp = it },
                    valueRange = 35f..130f
                )

                // Şeffaflık Slider (10% - 100%)
                Text("Şeffaflık: ${(alpha * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    valueRange = 0.10f..1.0f
                )

                // Renk Seçimi
                Text("Tuş Rengi (Gri ve Renkler)", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorPresets.forEach { hex ->
                        val color = Color(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorHex == hex) 2.5.dp else 1.dp,
                                    color = if (selectedColorHex == hex) TacticalGreen else Color(0xFF4A4B58),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        button.copy(
                            label = label.ifBlank { currentKeyLabel },
                            shape = selectedShape,
                            actionType = selectedAction,
                            keyCode = selectedKeyCode,
                            colorHex = selectedColorHex,
                            sizeDp = sizeDp,
                            alpha = alpha
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onDelete(button.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF334B))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sil")
                }
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        }
    )

    if (showKeyPicker) {
        KeyPickerDialog(
            title = "Klavye / Kumanda Tuşu Seç",
            onDismiss = { showKeyPicker = false },
            onSelectKey = { keyDef ->
                selectedKeyCode = keyDef.code
                if (label.isBlank() || label == "KEY" || label == button.label) {
                    label = keyDef.label
                }
                showKeyPicker = false
            }
        )
    }
}
