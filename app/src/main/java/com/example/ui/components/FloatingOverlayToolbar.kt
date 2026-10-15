package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkGreyBorder
import com.example.ui.theme.DarkGreyCard
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.StealthBlack
import com.example.ui.theme.TacticalGreen
import kotlin.math.abs

/**
 * Small, completely round floating balloon.
 * - Draggable across screen.
 * - Stays visible even when controls are hidden.
 * - Single tap when hidden immediately restores/opens controls.
 * - Separate actions for hiding controls vs completely closing the overlay.
 * - Strictly black, dark charcoal grey and tactical green palette (Zero blue).
 */
@Composable
fun FloatingOverlayToolbar(
    activeProfileId: String,
    areControlsVisible: Boolean = true,
    onToggleControlsVisibility: () -> Unit = {},
    onSelectProfile1: () -> Unit = {},
    onSelectProfile2: () -> Unit = {},
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onAddButton: () -> Unit = {},
    onCloseOverlay: () -> Unit = {},
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onExpandedChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        onExpandedChanged(isExpanded)
    }

    // Drag detection with tap distinction
    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += abs(dragAmount.x)
                        dragAccumulatorY += abs(dragAmount.y)
                        onDragDelta(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        // If moved less than 8px total, it's a click!
                        if (dragAccumulatorX < 8f && dragAccumulatorY < 8f) {
                            if (!areControlsVisible) {
                                // Controls are hidden: 1-tap instantly restores controls!
                                onToggleControlsVisibility()
                            } else {
                                // Controls are visible: toggle mini action menu
                                isExpanded = !isExpanded
                            }
                        }
                    }
                )
            }
    ) {
        if (!isExpanded) {
            // ========================================================
            // KÜÇÜK, TAMAMEN YUVARLAK YÜZEN BALON (52.dp daire)
            // ========================================================
            val balloonBorderColor = if (areControlsVisible) TacticalGreen else Color(0xFFFFD600)

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1B1E26),
                                StealthBlack
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, balloonBorderColor, CircleShape)
                    .clickable {
                        if (!areControlsVisible) {
                            onToggleControlsVisibility()
                        } else {
                            isExpanded = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (areControlsVisible) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Menüyü Aç",
                            tint = TacticalGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (activeProfileId.contains("2")) "P2" else "P1",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 10.sp
                        )
                    } else {
                        // KONTROLLER GİZLİYKEN: Balon ekranda kalır, üzerinde yeşil/sarı "AÇ" görünür
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Kontrolleri Göster",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AÇ",
                            color = Color(0xFFFFD600),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 11.sp
                        )
                    }
                }

                // Canlı durum sinyali (yeşil / sarı nokta)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (areControlsVisible) TacticalGreen else Color(0xFFFFD600))
                )
            }
        } else {
            // ========================================================
            // AÇILIR EYLEM MENÜSÜ (Gizleme ve Kapatma Net Olarak Ayrılmış)
            // ========================================================
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StealthBlack.copy(alpha = 0.98f))
                    .border(1.5.dp, DarkGreyBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Balon simgesi (Menüyü küçültmek için)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkGreySurface)
                        .border(1.dp, TacticalGreen, CircleShape)
                        .clickable { isExpanded = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Küçült",
                        tint = TacticalGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. KONTROLLERİ GİZLE (Balon ekranda kalır, sadece kontrol katmanı gizlenir!)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF14241B))
                        .border(1.2.dp, TacticalGreen, RoundedCornerShape(10.dp))
                        .clickable {
                            onToggleControlsVisibility()
                            isExpanded = false
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Kontrolleri Gizle",
                            tint = TacticalGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Gizle",
                            color = TacticalGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Profil 1
                val isP1 = activeProfileId.contains("1")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isP1) TacticalGreen else DarkGreyCard)
                        .clickable { onSelectProfile1() }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P1",
                        color = if (isP1) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 4. Profil 2
                val isP2 = activeProfileId.contains("2")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isP2) TacticalGreen else DarkGreyCard)
                        .clickable { onSelectProfile2() }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P2",
                        color = if (isP2) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 5. Ayarlar / Stüdyo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222630))
                        .border(1.dp, DarkGreyBorder, RoundedCornerShape(10.dp))
                        .clickable { onToggleEditMode() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 6. YÜZEN PENCEREYİ TAMAMEN KAPAT (Kırmızı X butonu - servisi tamamen sonlandırır)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF38151D))
                        .border(1.2.dp, Color(0xFFFF4B60), RoundedCornerShape(10.dp))
                        .clickable {
                            isExpanded = false
                            onCloseOverlay()
                        }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Pencereyi Tamamen Kapat",
                            tint = Color(0xFFFF4B60),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Kapat",
                            color = Color(0xFFFF4B60),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
