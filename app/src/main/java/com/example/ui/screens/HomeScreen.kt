package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.GamepadRepository
import com.example.input.InputDispatchEngine
import com.example.model.ActionType
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.model.InputDriverType
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import com.example.service.GamepadAccessibilityService
import com.example.service.GamepadOverlayService
import com.example.shizuku.ShizukuManager
import com.example.ui.components.ButtonEditDialog
import com.example.ui.components.FullScreenCustomizeStudio
import com.example.ui.components.JoystickEditDialog
import com.example.ui.theme.DarkGreyBorder
import com.example.ui.theme.DarkGreyCard
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.StealthBlack
import com.example.ui.theme.TacticalGreen
import java.util.UUID

@Composable
fun HomeScreen(
    repository: GamepadRepository,
    engine: InputDispatchEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeProfile by repository.activeProfile.collectAsState()
    val isOverlayRunning by repository.isOverlayRunning.collectAsState()
    val activeDriver by repository.inputDriver.collectAsState()

    // Reactive, dynamic permission checks with immediate lifecycle update
    var hasOverlayPerm by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isA11yRunning by remember { mutableStateOf(GamepadAccessibilityService.isAccessibilityServiceEnabled(context)) }
    val shizukuStatus by ShizukuManager.status.collectAsState()

    fun refreshPermissions() {
        hasOverlayPerm = Settings.canDrawOverlays(context)
        isA11yRunning = GamepadAccessibilityService.isAccessibilityServiceEnabled(context)
        ShizukuManager.checkStatus()
        if (isOverlayRunning) {
            GamepadOverlayService.restoreControls(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activePointerIds by engine.activePointerIds.collectAsState()
    val pressedButtons by engine.pressedButtons.collectAsState()

    var showFullScreenStudio by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<GamepadButton?>(null) }
    var editingJoystick by remember { mutableStateOf<Pair<JoystickConfig, Boolean>?>(null) }
    var isNewButtonDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StealthBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header with Refresh & Status
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkGreyCard)
                            .border(1.5.dp, TacticalGreen, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "FlexPad",
                            tint = TacticalGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "FlexPad Gamepad",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Modüler Sanal Gamepad & Gerçek Giriş Motoru",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { refreshPermissions() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = TacticalGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isOverlayRunning) Color(0x3300E676) else DarkGreyCard)
                            .border(1.dp, if (isOverlayRunning) TacticalGreen else DarkGreyBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isOverlayRunning) "● KATMAN AÇIK" else "○ KAPALI",
                            color = if (isOverlayRunning) TacticalGreen else Color(0xFFA0A5B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 1: Floating Overlay Window Master Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreyCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, if (isOverlayRunning) TacticalGreen else DarkGreyBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Yüzen Gamepad Katmanı",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Oyunun üzerine şeffaf kontrolleri yerleştirir",
                                fontSize = 12.sp,
                                color = Color(0xFFA0A5B2)
                            )
                        }

                        if (!hasOverlayPerm) {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
                            ) {
                                Text("İzin Ver", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isOverlayRunning) {
                                            GamepadOverlayService.start(context)
                                        } else {
                                            // Servis zaten çalışıyorsa yeniden başlatmak yerine kaybolan kontrol katmanını geri yükle
                                            repository.setControlsVisibility(true)
                                            GamepadOverlayService.restoreControls(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = if (!isOverlayRunning) "Aç" else "Aç / Geri Yükle",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (isOverlayRunning) {
                                    OutlinedButton(
                                        onClick = { GamepadOverlayService.stop(context) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4B60))
                                    ) {
                                        Text("Kapat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Direct Profile Selector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isP1 = activeProfile.id == "profile_1_dual_gamepad"
                        val isP2 = activeProfile.id == "profile_2_fps_shooter"
                        val isP3 = activeProfile.id == "profile_3_retro_arcade"

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isP1) TacticalGreen else Color(0xFF22252E))
                                .clickable { repository.selectProfile("profile_1_dual_gamepad") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1. Profil (Standart)",
                                color = if (isP1) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isP2) TacticalGreen else Color(0xFF22252E))
                                .clickable { repository.selectProfile("profile_2_fps_shooter") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2. Profil (FPS)",
                                color = if (isP2) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isP3) TacticalGreen else Color(0xFF22252E))
                                .clickable { repository.selectProfile("profile_3_retro_arcade") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "3. Profil",
                                color = if (isP3) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // PROMINENT CUSTOMIZE (ÖZELLEŞTİR) BUTTON!
                    Button(
                        onClick = { showFullScreenStudio = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD600),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "🎮 ÖZELLEŞTİR (DÜZENİ AYARLA)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Section 2: Joysticks & Buttons Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreyCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkGreyBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Kontroller & Tuşlar (${activeProfile.name})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Joystick Quick Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Stick Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkGreyCard)
                                .border(1.dp, if (activeProfile.leftJoystick.enabled) TacticalGreen else DarkGreyBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (activeProfile.leftJoystick.enabled) {
                                        editingJoystick = Pair(activeProfile.leftJoystick, true)
                                    } else {
                                        repository.updateLeftJoystick { it.copy(enabled = true, centerXRatio = 0.20f, centerYRatio = 0.70f) }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🕹️ Sol Stick",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (activeProfile.leftJoystick.enabled) "Aktif" else "Silindi",
                                        color = if (activeProfile.leftJoystick.enabled) TacticalGreen else Color(0xFFFF5252),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (activeProfile.leftJoystick.enabled) {
                                        val modeStr = if (activeProfile.leftJoystick.mode == JoystickMode.KEYBOARD) {
                                            "⌨️ WASD (${activeProfile.leftJoystick.upKeyLabel},${activeProfile.leftJoystick.leftKeyLabel},${activeProfile.leftJoystick.downKeyLabel},${activeProfile.leftJoystick.rightKeyLabel})"
                                        } else {
                                            "🎮 Analog"
                                        }
                                        "$modeStr | ${activeProfile.leftJoystick.sizeDp.toInt()}dp"
                                    } else {
                                        "Geri yüklemek için dokunun"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A5B2)
                                )
                            }
                        }

                        // Right Stick Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkGreyCard)
                                .border(1.dp, if (activeProfile.rightJoystick.enabled) TacticalGreen else DarkGreyBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (activeProfile.rightJoystick.enabled) {
                                        editingJoystick = Pair(activeProfile.rightJoystick, false)
                                    } else {
                                        repository.updateRightJoystick { it.copy(enabled = true, centerXRatio = 0.80f, centerYRatio = 0.70f) }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🕹️ Sağ Stick",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (activeProfile.rightJoystick.enabled) "Aktif" else "Silindi",
                                        color = if (activeProfile.rightJoystick.enabled) TacticalGreen else Color(0xFFFF5252),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (activeProfile.rightJoystick.enabled) {
                                        val modeStr = if (activeProfile.rightJoystick.mode == JoystickMode.KEYBOARD) {
                                            "⌨️ Klavye"
                                        } else {
                                            "🎮 Analog"
                                        }
                                        "$modeStr | ${activeProfile.rightJoystick.sizeDp.toInt()}dp"
                                    } else {
                                        "Geri yüklemek için dokunun"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A5B2)
                                )
                            }
                        }
                    }

                    // Buttons Header with Add Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktif Tuşlar (${activeProfile.buttons.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

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
                                isNewButtonDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tuş Ekle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Neutral Grey Button Tiles Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeProfile.buttons.chunked(4).forEach { rowButtons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowButtons.forEach { btn ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF232630))
                                            .border(1.dp, DarkGreyBorder, RoundedCornerShape(10.dp))
                                            .clickable { editingButton = btn; isNewButtonDialog = false }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = btn.label,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${btn.sizeDp.toInt()}dp",
                                                fontSize = 10.sp,
                                                color = TacticalGreen
                                            )
                                        }
                                    }
                                }
                                val remaining = 4 - rowButtons.size
                                for (i in 0 until remaining) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Input Driver & Permissions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreyCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkGreyBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Giriş Motoru & İzin Durumu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Driver selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputDriverType.values().forEach { driver ->
                            FilterChip(
                                selected = activeDriver == driver,
                                onClick = {
                                    repository.setInputDriver(driver)
                                    engine.setDriver(driver)
                                },
                                label = {
                                    Text(
                                        when (driver) {
                                            InputDriverType.ACCESSIBILITY -> "Accessibility (Dokunma)"
                                            InputDriverType.SHIZUKU -> "Shizuku ADB"
                                            InputDriverType.SIMULATED -> "Simülatör"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TacticalGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Accessibility Service Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isA11yRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isA11yRunning) TacticalGreen else Color(0xFFFFD600),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Erişilebilirlik Hizmeti",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isA11yRunning) "✓ Açık / Gerçek dokunma hazır" else "⚠️ Devre Dışı (Açmak için dokunun)",
                                    fontSize = 11.sp,
                                    color = if (isA11yRunning) TacticalGreen else Color(0xFF9EACB8)
                                )
                            }
                        }

                        if (!isA11yRunning) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Ayarları Aç", fontSize = 11.sp)
                            }
                        }
                    }

                    // Shizuku Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (shizukuStatus.isAvailable && shizukuStatus.isAuthorized) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (shizukuStatus.isAvailable && shizukuStatus.isAuthorized) TacticalGreen else Color(0xFF758099),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Shizuku / ADB Kabuğu",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = shizukuStatus.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9EACB8)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (!shizukuStatus.isAvailable) {
                                    ShizukuManager.checkStatus()
                                } else {
                                    ShizukuManager.requestPermission()
                                }
                            }
                        ) {
                            Text(if (!shizukuStatus.isAuthorized && shizukuStatus.isAvailable) "Yetkilendir" else "Kontrol Et", fontSize = 11.sp)
                        }
                    }

                    // Telemetry Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkGreySurface)
                            .border(1.dp, DarkGreyBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktif Dokunma: ${activePointerIds.size} nokta",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Basılı: ${if (pressedButtons.isEmpty()) "Yok" else pressedButtons.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = TacticalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }

    // Full-Screen Customization Studio
    if (showFullScreenStudio) {
        FullScreenCustomizeStudio(
            repository = repository,
            engine = engine,
            onDismiss = { showFullScreenStudio = false }
        )
    }

    // Joystick Edit Dialog
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

    // Button Edit Dialog
    editingButton?.let { btn ->
        ButtonEditDialog(
            button = btn,
            onDismiss = { editingButton = null; isNewButtonDialog = false },
            onSave = { updated ->
                if (isNewButtonDialog) {
                    repository.addButton(updated)
                } else {
                    repository.updateButton(updated.id) { updated }
                }
                editingButton = null
                isNewButtonDialog = false
            },
            onDelete = { id ->
                repository.deleteButton(id)
                editingButton = null
                isNewButtonDialog = false
            }
        )
    }
}
