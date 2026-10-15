package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import com.example.model.KeyDefinition
import com.example.model.KeyboardKeys
import com.example.ui.theme.TacticalGreen

@Composable
fun JoystickEditDialog(
    config: JoystickConfig,
    isLeft: Boolean,
    onDismiss: () -> Unit,
    onSave: (JoystickConfig) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(config.label) }
    var sizeDp by remember { mutableFloatStateOf(config.sizeDp) }
    var sensitivity by remember { mutableFloatStateOf(config.sensitivity) }
    var deadzone by remember { mutableFloatStateOf(config.deadzone) }
    var alpha by remember { mutableFloatStateOf(config.alpha) }
    var selectedMode by remember { mutableStateOf(config.mode) }

    var upKeyCode by remember { mutableIntStateOf(config.upKeyCode) }
    var upKeyLabel by remember { mutableStateOf(config.upKeyLabel) }

    var downKeyCode by remember { mutableIntStateOf(config.downKeyCode) }
    var downKeyLabel by remember { mutableStateOf(config.downKeyLabel) }

    var leftKeyCode by remember { mutableIntStateOf(config.leftKeyCode) }
    var leftKeyLabel by remember { mutableStateOf(config.leftKeyLabel) }

    var rightKeyCode by remember { mutableIntStateOf(config.rightKeyCode) }
    var rightKeyLabel by remember { mutableStateOf(config.rightKeyLabel) }

    // State for which direction is currently picking a key (null, "UP", "DOWN", "LEFT", "RIGHT")
    var pickingDirection by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${if (isLeft) "Sol" else "Sağ"} Joystick'i Düzenle",
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
                // Mod Seçimi: Analog Kumanda vs Klavye / WASD
                Text("Çalışma Modu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == JoystickMode.ANALOG,
                        onClick = { selectedMode = JoystickMode.ANALOG },
                        leadingIcon = {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("🎮 Analog / Dokunma", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMode == JoystickMode.KEYBOARD,
                        onClick = { selectedMode = JoystickMode.KEYBOARD },
                        leadingIcon = {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("⌨️ Klavye / WASD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Klavye Yön Atamaları Bölümü
                if (selectedMode == JoystickMode.KEYBOARD) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF14161A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E36)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🕹️ 4 Yön Tuş Atamaları (İterek Tuş Basma)",
                                color = TacticalGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Coistiki yukarı itince W, sağa itince D, sağ-yukarı itince W+D basar. İstediğiniz yöne dokunup herhangi bir bilgisayar tuşunu atayabilirsiniz.",
                                color = Color(0xFFB0B4BC),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            // Hızlı Presetler
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        upKeyCode = KeyEvent.KEYCODE_W; upKeyLabel = "W"
                                        downKeyCode = KeyEvent.KEYCODE_S; downKeyLabel = "S"
                                        leftKeyCode = KeyEvent.KEYCODE_A; leftKeyLabel = "A"
                                        rightKeyCode = KeyEvent.KEYCODE_D; rightKeyLabel = "D"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("WASD Preseti", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        upKeyCode = KeyEvent.KEYCODE_DPAD_UP; upKeyLabel = "YUKARI"
                                        downKeyCode = KeyEvent.KEYCODE_DPAD_DOWN; downKeyLabel = "AŞAĞI"
                                        leftKeyCode = KeyEvent.KEYCODE_DPAD_LEFT; leftKeyLabel = "SOL"
                                        rightKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT; rightKeyLabel = "SAĞ"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ok Tuşları", fontSize = 11.sp)
                                }
                            }

                            // 4 Direction Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DirectionKeyChip(
                                    directionName = "⬆️ Yukarı",
                                    currentLabel = upKeyLabel,
                                    onClick = { pickingDirection = "UP" },
                                    modifier = Modifier.weight(1f)
                                )
                                DirectionKeyChip(
                                    directionName = "⬇️ Aşağı",
                                    currentLabel = downKeyLabel,
                                    onClick = { pickingDirection = "DOWN" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DirectionKeyChip(
                                    directionName = "⬅️ Sol",
                                    currentLabel = leftKeyLabel,
                                    onClick = { pickingDirection = "LEFT" },
                                    modifier = Modifier.weight(1f)
                                )
                                DirectionKeyChip(
                                    directionName = "➡️ Sağ",
                                    currentLabel = rightKeyLabel,
                                    onClick = { pickingDirection = "RIGHT" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(8) },
                    label = { Text("Joystick Başlığı (Örn: WASD, L, MOVE)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Boyut
                Text("Boyut: ${sizeDp.toInt()} dp", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = sizeDp,
                    onValueChange = { sizeDp = it },
                    valueRange = 90f..220f
                )

                // Hassasiyet
                Text("Hassasiyet: ${"%.2f".format(sensitivity)}x", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..2.5f
                )

                // Ölü Bölge
                Text("Ölü Bölge: ${(deadzone * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = deadzone,
                    onValueChange = { deadzone = it },
                    valueRange = 0.04f..0.30f
                )

                // Şeffaflık
                Text("Şeffaflık: ${(alpha * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    valueRange = 0.15f..1.0f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        config.copy(
                            label = label.ifBlank { config.label },
                            sizeDp = sizeDp,
                            sensitivity = sensitivity,
                            deadzone = deadzone,
                            alpha = alpha,
                            mode = selectedMode,
                            upKeyCode = upKeyCode,
                            upKeyLabel = upKeyLabel,
                            downKeyCode = downKeyCode,
                            downKeyLabel = downKeyLabel,
                            leftKeyCode = leftKeyCode,
                            leftKeyLabel = leftKeyLabel,
                            rightKeyCode = rightKeyCode,
                            rightKeyLabel = rightKeyLabel
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
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF334B))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Joystick'i Sil")
                }
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        }
    )

    // Key Picker Dialog for Directional Key
    pickingDirection?.let { dir ->
        KeyPickerDialog(
            title = "Yön Tuşu Seç: $dir",
            onDismiss = { pickingDirection = null },
            onSelectKey = { keyDef ->
                when (dir) {
                    "UP" -> {
                        upKeyCode = keyDef.code
                        upKeyLabel = keyDef.label
                    }
                    "DOWN" -> {
                        downKeyCode = keyDef.code
                        downKeyLabel = keyDef.label
                    }
                    "LEFT" -> {
                        leftKeyCode = keyDef.code
                        leftKeyLabel = keyDef.label
                    }
                    "RIGHT" -> {
                        rightKeyCode = keyDef.code
                        rightKeyLabel = keyDef.label
                    }
                }
                pickingDirection = null
            }
        )
    }
}

@Composable
private fun DirectionKeyChip(
    directionName: String,
    currentLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1F24))
            .border(1.dp, Color(0xFF32363E), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(directionName, color = Color(0xFF9E9E9E), fontSize = 10.sp)
            Text(currentLabel, color = TacticalGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun KeyPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelectKey: (KeyDefinition) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(KeyboardKeys.CATEGORIES.first()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredKeys = remember(selectedCategory, searchQuery) {
        if (searchQuery.isNotBlank()) {
            KeyboardKeys.ALL_KEYS.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        } else {
            KeyboardKeys.ALL_KEYS.filter { it.category == selectedCategory }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF101215),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2A2E36)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = TacticalGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onDismiss) { Text("Kapat") }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tuş ara (Örn: W, Space, F1, Enter)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Tabs (when not searching)
                if (searchQuery.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyboardKeys.CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Key Chips Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 65.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredKeys) { keyDef ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B1E23))
                                .border(1.dp, Color(0xFF2E323A), RoundedCornerShape(8.dp))
                                .clickable { onSelectKey(keyDef) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = keyDef.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (keyDef.description.isNotBlank()) {
                                    Text(
                                        text = keyDef.description,
                                        color = Color(0xFF8E8E8E),
                                        fontSize = 8.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
