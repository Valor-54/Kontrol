package com.example.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.InputDispatchEngine
import com.example.model.JoystickConfig
import com.example.model.JoystickMode
import com.example.ui.theme.TacticalGreen
import com.example.util.HapticUtil
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun VirtualJoystick(
    config: JoystickConfig,
    isLeft: Boolean,
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
    if (!config.enabled) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val sizePx = with(density) { config.sizeDp.dp.toPx() }
    val maxRadiusPx = sizePx / 2f
    val knobRadiusPx = maxRadiusPx * 0.40f
    val maxTravelPx = maxRadiusPx - (knobRadiusPx * 0.5f)

    // Smooth jitter-free position dragging in Edit Mode
    var isDraggingPosition by remember { mutableStateOf(false) }
    var localCenterX by remember(config.id) { mutableFloatStateOf(config.centerXRatio * screenWidthPx) }
    var localCenterY by remember(config.id) { mutableFloatStateOf(config.centerYRatio * screenHeightPx) }

    LaunchedEffect(config.centerXRatio, screenWidthPx) {
        if (!isDraggingPosition && screenWidthPx > 0) {
            localCenterX = config.centerXRatio * screenWidthPx
        }
    }
    LaunchedEffect(config.centerYRatio, screenHeightPx) {
        if (!isDraggingPosition && screenHeightPx > 0) {
            localCenterY = config.centerYRatio * screenHeightPx
        }
    }

    var rawKnobOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointerId by remember { mutableStateOf<PointerId?>(null) }
    var isDraggingKnob by remember { mutableStateOf(false) }

    val springBackOffset by animateOffsetAsState(
        targetValue = Offset.Zero,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1600f),
        label = "joystick_snap_back"
    )

    val currentKnobOffset = if (isDraggingKnob) rawKnobOffset else springBackOffset
    val baseColor = TacticalGreen
    val effectiveAlpha = (config.alpha * globalAlpha).coerceIn(0.15f, 1f)

    // Window positioning vs standalone floating window
    val topLeftX = if (applyScreenOffset) (localCenterX - maxRadiusPx) else 0f
    val topLeftY = if (applyScreenOffset) (localCenterY - maxRadiusPx) else 0f

    // The physical screen coordinates sent to the real touch input engine
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

    // Directional active indicators in Keyboard/WASD mode
    val isUpActive = isDraggingKnob && currentKnobOffset.y < -maxTravelPx * 0.32f
    val isDownActive = isDraggingKnob && currentKnobOffset.y > maxTravelPx * 0.32f
    val isLeftActive = isDraggingKnob && currentKnobOffset.x < -maxTravelPx * 0.32f
    val isRightActive = isDraggingKnob && currentKnobOffset.x > maxTravelPx * 0.32f
    androidx.compose.runtime.DisposableEffect(isLeft, config.id) {
        onDispose {
            if (isDraggingKnob) {
                val pId = ((activePointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                engine.onJoystickUp(isLeft, pId)
                isDraggingKnob = false
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
            .size(config.sizeDp.dp)
            .clip(CircleShape) // Zero square layer! Strictly clipped to circle
            .then(
                if (isEditMode) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = Color(0xFFFFD600),
                            shape = CircleShape
                        )
                        .background(Color(0x22FFD600), shape = CircleShape)
                        .pointerInput(isLeft, config.id) {
                            detectTapGestures(
                                onTap = { onEditClicked() }
                            )
                        }
                        .pointerInput(isLeft, config.id, screenWidthPx, screenHeightPx) {
                            detectDragGestures(
                                onDragStart = { isDraggingPosition = true },
                                onDragEnd = {
                                    isDraggingPosition = false
                                    if (screenWidthPx > 0 && screenHeightPx > 0) {
                                        onPositionChanged(
                                            (localCenterX / screenWidthPx).coerceIn(0.05f, 0.95f),
                                            (localCenterY / screenHeightPx).coerceIn(0.05f, 0.95f)
                                        )
                                    }
                                },
                                onDragCancel = { isDraggingPosition = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    localCenterX = (localCenterX + dragAmount.x).coerceIn(maxRadiusPx, screenWidthPx - maxRadiusPx)
                                    localCenterY = (localCenterY + dragAmount.y).coerceIn(maxRadiusPx, screenHeightPx - maxRadiusPx)
                                }
                            )
                        }
                } else {
                    Modifier.pointerInput(isLeft, config.id, config.deadzone, config.sensitivity, config.mode) {
                        while (true) {
                            try {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()

                                        if (activePointerId == null) {
                                            val downPointer = event.changes.firstOrNull { it.pressed && !it.isConsumed }
                                            if (downPointer != null) {
                                                val dx = downPointer.position.x - maxRadiusPx
                                                val dy = downPointer.position.y - maxRadiusPx
                                                val dist = hypot(dx, dy)

                                                // Crucial: Ignore touches outside circular track so corners don't block screen!
                                                if (dist <= maxRadiusPx) {
                                                    activePointerId = downPointer.id
                                                    isDraggingKnob = true
                                                    val pId = (downPointer.id.value and 0x7FFFFFFF).toInt()
                                                    HapticUtil.performClickFeedback(context)
                                                    engine.onJoystickDown(isLeft, pId, engineCenterX, engineCenterY)

                                                    val clamped = dist.coerceAtMost(maxTravelPx)
                                                    val factor = if (dist > 0f) clamped / dist else 0f
                                                    rawKnobOffset = Offset(dx * factor, dy * factor)

                                                    engine.onJoystickMove(
                                                        config = config,
                                                        isLeft = isLeft,
                                                        pointerId = pId,
                                                        deltaX = rawKnobOffset.x,
                                                        deltaY = rawKnobOffset.y,
                                                        maxRadius = maxTravelPx,
                                                        screenCenterX = engineCenterX,
                                                        screenCenterY = engineCenterY
                                                    )
                                                    downPointer.consume()
                                                }
                                            }
                                        } else {
                                            val myPointer = event.changes.firstOrNull { it.id == activePointerId }

                                            if (myPointer == null || !myPointer.pressed) {
                                                val pId = ((activePointerId?.value ?: 0L) and 0x7FFFFFFF).toInt()
                                                engine.onJoystickUp(isLeft, pId)
                                                activePointerId = null
                                                isDraggingKnob = false
                                                rawKnobOffset = Offset.Zero
                                                myPointer?.consume()
                                            } else {
                                                val dx = myPointer.position.x - maxRadiusPx
                                                val dy = myPointer.position.y - maxRadiusPx
                                                val dist = hypot(dx, dy)
                                                val clamped = dist.coerceAtMost(maxTravelPx)
                                                val factor = if (dist > 0f) clamped / dist else 0f
                                                rawKnobOffset = Offset(dx * factor, dy * factor)

                                                val pId = (myPointer.id.value and 0x7FFFFFFF).toInt()
                                                engine.onJoystickMove(
                                                    config = config,
                                                    isLeft = isLeft,
                                                    pointerId = pId,
                                                    deltaX = rawKnobOffset.x,
                                                    deltaY = rawKnobOffset.y,
                                                    maxRadius = maxTravelPx,
                                                    screenCenterX = engineCenterX,
                                                    screenCenterY = engineCenterY
                                                )
                                                myPointer.consume()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.w("VirtualJoystick", "Restarting joystick pointer input loop for ${config.id}", e)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Base Circular Track
        Canvas(modifier = Modifier.size(config.sizeDp.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 3f

            drawCircle(
                color = Color(0xFF0B0D10).copy(alpha = effectiveAlpha * 0.82f),
                radius = radius,
                center = center
            )

            drawCircle(
                color = baseColor.copy(alpha = effectiveAlpha * 0.85f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Deadzone ring
            drawCircle(
                color = baseColor.copy(alpha = effectiveAlpha * 0.30f),
                radius = radius * config.deadzone,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Crosshair Guides
            val crossColor = Color(0xFF353940).copy(alpha = effectiveAlpha * 0.5f)
            val lineStroke = 1.dp.toPx()
            drawLine(
                color = crossColor,
                start = Offset(center.x - radius * 0.65f, center.y),
                end = Offset(center.x + radius * 0.65f, center.y),
                strokeWidth = lineStroke
            )
            drawLine(
                color = crossColor,
                start = Offset(center.x, center.y - radius * 0.65f),
                end = Offset(center.x, center.y + radius * 0.65f),
                strokeWidth = lineStroke
            )
        }

        // Directional Labels in Keyboard/WASD mode
        if (config.mode == JoystickMode.KEYBOARD) {
            // UP Key
            Text(
                text = config.upKeyLabel,
                color = if (isUpActive) TacticalGreen else Color(0xFF8E94A0).copy(alpha = effectiveAlpha),
                fontSize = 11.sp,
                fontWeight = if (isUpActive) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )
            // DOWN Key
            Text(
                text = config.downKeyLabel,
                color = if (isDownActive) TacticalGreen else Color(0xFF8E94A0).copy(alpha = effectiveAlpha),
                fontSize = 11.sp,
                fontWeight = if (isDownActive) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
            // LEFT Key
            Text(
                text = config.leftKeyLabel,
                color = if (isLeftActive) TacticalGreen else Color(0xFF8E94A0).copy(alpha = effectiveAlpha),
                fontSize = 11.sp,
                fontWeight = if (isLeftActive) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )
            // RIGHT Key
            Text(
                text = config.rightKeyLabel,
                color = if (isRightActive) TacticalGreen else Color(0xFF8E94A0).copy(alpha = effectiveAlpha),
                fontSize = 11.sp,
                fontWeight = if (isRightActive) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

        // Draggable Knob (Neutral Grey Gradient with Neon Accent when active)
        val knobDp = with(density) { (knobRadiusPx * 2).toDp() }
        val knobOffsetInt = IntOffset(currentKnobOffset.x.roundToInt(), currentKnobOffset.y.roundToInt())

        Box(
            modifier = Modifier
                .offset { knobOffsetInt }
                .size(knobDp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF383A40).copy(alpha = effectiveAlpha),
                            Color(0xFF222428).copy(alpha = effectiveAlpha),
                            Color(0xFF121316).copy(alpha = effectiveAlpha)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isDraggingKnob) baseColor.copy(alpha = effectiveAlpha) else Color(0xFF484C55).copy(alpha = effectiveAlpha * 0.85f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (config.mode == JoystickMode.KEYBOARD) "WASD" else config.label,
                color = if (isDraggingKnob) baseColor else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Edit Mode Quick Action Badge
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFD600))
                    .clickable { onEditClicked() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
