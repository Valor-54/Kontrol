package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.InputDispatchEngine
import com.example.model.ButtonShape
import com.example.model.GamepadButton
import com.example.ui.theme.TacticalGreen
import com.example.util.HapticUtil
import kotlin.math.roundToInt

@Composable
fun VirtualButton(
    button: GamepadButton,
    isEditMode: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float,
    engine: InputDispatchEngine,
    globalAlpha: Float = 0.85f,
    applyScreenOffset: Boolean = true,
    actualScreenCenterX: Float = 0f,
    actualScreenCenterY: Float = 0f,
    onPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    onEditClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val sizePx = with(density) { button.sizeDp.dp.toPx() }
    val halfSizePx = sizePx / 2f

    // Smooth jitter-free local drag state in Edit Mode
    var isDraggingPosition by remember { mutableStateOf(false) }
    var localCenterX by remember(button.id) { mutableFloatStateOf(button.xRatio * screenWidthPx) }
    var localCenterY by remember(button.id) { mutableFloatStateOf(button.yRatio * screenHeightPx) }

    LaunchedEffect(button.xRatio, screenWidthPx) {
        if (!isDraggingPosition && screenWidthPx > 0) {
            localCenterX = button.xRatio * screenWidthPx
        }
    }
    LaunchedEffect(button.yRatio, screenHeightPx) {
        if (!isDraggingPosition && screenHeightPx > 0) {
            localCenterY = button.yRatio * screenHeightPx
        }
    }

    val topLeftX = if (applyScreenOffset) (localCenterX - halfSizePx) else 0f
    val topLeftY = if (applyScreenOffset) (localCenterY - halfSizePx) else 0f

    // Target screen coordinates for input injection
    val engineCenterX = if (actualScreenCenterX > 0f) {
        actualScreenCenterX
    } else {
        localCenterX
    }
    val engineCenterY = if (actualScreenCenterY > 0f) {
        actualScreenCenterY
    } else {
        localCenterY
    }

    var isPressed by remember { mutableStateOf(false) }
    var activePointerId by remember { mutableStateOf<PointerId?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 1600f),
        label = "button_scale"
    )

    val effectiveAlpha = (button.alpha * globalAlpha).coerceIn(0.15f, 1f)

    val shape: Shape = when (button.shape) {
        ButtonShape.CIRCLE -> CircleShape
        ButtonShape.ROUNDED_RECT -> RoundedCornerShape(14.dp)
        ButtonShape.SQUARE -> RoundedCornerShape(6.dp)
    }

    androidx.compose.runtime.DisposableEffect(button.id) {
        onDispose {
            if (isPressed) {
                val pId = ((activePointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                engine.onButtonUp(button, pId)
                isPressed = false
                activePointerId = null
            }
        }
    }

    Box(
        modifier = modifier
            .then(
                if (applyScreenOffset) {
                    Modifier.offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                } else {
                    Modifier
                }
            )
            .size(button.sizeDp.dp)
            .scale(scale)
            .then(
                if (isEditMode) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = Color(0xFFFFD600),
                            shape = shape
                        )
                        .background(Color(0x33FFD600), shape = shape)
                        .pointerInput(button.id) {
                            detectTapGestures(
                                onTap = { onEditClicked() }
                            )
                        }
                        .pointerInput(button.id, screenWidthPx, screenHeightPx) {
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingPosition = true
                                },
                                onDragEnd = {
                                    isDraggingPosition = false
                                    if (screenWidthPx > 0 && screenHeightPx > 0) {
                                        onPositionChanged(
                                            (localCenterX / screenWidthPx).coerceIn(0.04f, 0.96f),
                                            (localCenterY / screenHeightPx).coerceIn(0.04f, 0.96f)
                                        )
                                    }
                                },
                                onDragCancel = {
                                    isDraggingPosition = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    localCenterX = (localCenterX + dragAmount.x).coerceIn(halfSizePx, screenWidthPx - halfSizePx)
                                    localCenterY = (localCenterY + dragAmount.y).coerceIn(halfSizePx, screenHeightPx - halfSizePx)
                                }
                            )
                        }
                } else {
                    Modifier.pointerInput(button.id) {
                        while (true) {
                            try {
                                awaitPointerEventScope {
                                    var trackedPointerId: PointerId? = null
                                    var isDown = false

                                    while (true) {
                                        val event = awaitPointerEvent()

                                        if (trackedPointerId == null) {
                                            val downPointer = event.changes.firstOrNull { it.pressed && !it.isConsumed }
                                            if (downPointer != null) {
                                                trackedPointerId = downPointer.id
                                                activePointerId = downPointer.id
                                                isDown = true
                                                isPressed = true
                                                val pId = (downPointer.id.value and 0x7FFFFFFF).toInt()
                                                HapticUtil.performClickFeedback(context)
                                                engine.onButtonDown(button, pId, engineCenterX, engineCenterY)
                                                downPointer.consume()
                                            }
                                        } else {
                                            val myPointer = event.changes.firstOrNull { it.id == trackedPointerId }
                                            if (myPointer == null || !myPointer.pressed) {
                                                if (isDown) {
                                                    val pId = ((trackedPointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                                                    engine.onButtonUp(button, pId)
                                                }
                                                isPressed = false
                                                isDown = false
                                                trackedPointerId = null
                                                activePointerId = null
                                                myPointer?.consume()
                                            } else {
                                                val px = myPointer.position.x
                                                val py = myPointer.position.y
                                                val slop = sizePx * 0.22f
                                                val isInside = px >= -slop && px <= (sizePx + slop) && py >= -slop && py <= (sizePx + slop)

                                                if (!isInside && isDown) {
                                                    // Finger slid outside: release button but keep tracking until lifted
                                                    val pId = ((trackedPointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                                                    engine.onButtonUp(button, pId)
                                                    isDown = false
                                                    isPressed = false
                                                } else if (isInside && !isDown) {
                                                    // Finger slid back inside: re-engage button
                                                    val pId = ((trackedPointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                                                    engine.onButtonDown(button, pId, engineCenterX, engineCenterY)
                                                    isDown = true
                                                    isPressed = true
                                                }
                                                myPointer.consume()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.w("VirtualButton", "Restarting pointer loop for ${button.id}", e)
                            }
                        }
                    }
                }
            )
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(
                            Color(0xFF4C4F5E).copy(alpha = effectiveAlpha),
                            Color(0xFF353744).copy(alpha = effectiveAlpha),
                            Color(0xFF1E2028).copy(alpha = effectiveAlpha)
                        )
                    } else {
                        listOf(
                            Color(0xFF383B47).copy(alpha = effectiveAlpha),
                            Color(0xFF262832).copy(alpha = effectiveAlpha),
                            Color(0xFF181920).copy(alpha = effectiveAlpha)
                        )
                    }
                ),
                shape = shape
            )
            .border(
                width = if (isPressed) 2.5.dp else 1.5.dp,
                color = if (isPressed) TacticalGreen else Color(0xFF505466).copy(alpha = effectiveAlpha),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.label,
            color = if (isPressed) TacticalGreen else Color(0xFFEDEDF2).copy(alpha = effectiveAlpha),
            fontSize = if (button.label.length > 3) 11.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold
        )

        // Edit Mode Quick Action Badge
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFD600))
                    .clickable { onEditClicked() }
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
