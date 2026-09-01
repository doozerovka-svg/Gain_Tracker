package com.example.workouttracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D Rotary Side-Wheel (Jog Dial / Volume Thumbwheel)
 * Simulates a knurled cylindrical drum viewed from the side/edge with smooth horizontal rotation,
 * graduation tick marks, haptic feedback on each tick, and quick step adjustments.
 */
@Composable
fun RotarySideWheelPicker(
    value: Double,
    onValueChange: (Double) -> Unit,
    min: Double = 0.0,
    max: Double = 500.0,
    step: Double = 0.5,
    label: String,
    unit: String,
    quickSteps: List<Double> = listOf(-5.0, -2.5, 2.5, 5.0),
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var lastHapticValue by remember(value) { mutableStateOf(value) }

    fun clampAndRound(raw: Double): Double {
        val factor = 1.0 / step
        val rounded = (raw * factor).roundToInt() / factor
        val clamped = rounded.coerceIn(min, max)
        return if (step < 1.0) {
            String.format(Locale.US, "%.1f", clamped).toDouble()
        } else {
            clamped.roundToInt().toDouble()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Label, Digital Readout, Quick Buttons, and Done Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (step < 1.0) String.format(Locale.US, "%.1f", value) else value.toInt().toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            ),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                // Quick nudge buttons + Done Check button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickSteps.forEach { qs ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.height(32.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val next = clampAndRound(value + qs)
                                    onValueChange(next)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    contentColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (qs > 0) "+${if (qs == qs.toInt().toDouble()) qs.toInt() else qs}" else "${if (qs == qs.toInt().toDouble()) qs.toInt() else qs}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Done / Close button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Готово",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3D Knurled Cylinder Wheel (Canvas with drag gestures)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF171717),
                                Color(0xFF080808),
                                Color(0xFF171717)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragAccumulator = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                                val pixelsPerStep = 24f // ~24px drag per step increment
                                val stepsDelta = (dragAccumulator / pixelsPerStep).toInt()
                                if (stepsDelta != 0) {
                                    val next = clampAndRound(value + stepsDelta * step)
                                    dragAccumulator -= stepsDelta * pixelsPerStep
                                    if (next != value) {
                                        onValueChange(next)
                                        if (next != lastHapticValue) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastHapticValue = next
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = Color(0xFFA3A3A3)

                Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val visibleTicks = 16

                    // Draw cylindrical tick marks
                    for (i in -visibleTicks..visibleTicks) {
                        val tickVal = value + i * step
                        val isMajor = (tickVal / (step * 5)).roundToInt().toDouble() == tickVal / (step * 5)
                        val isMid = (tickVal / (step * 2)).roundToInt().toDouble() == tickVal / (step * 2)

                        // 3D Cylindrical curve projection
                        val norm = i.toFloat() / visibleTicks
                        val angle = norm * (PI.toFloat() / 2.2f)
                        val cosVal = cos(angle)
                        val sinVal = sin(angle)
                        val posX = width / 2f + sinVal * (width * 0.46f)
                        val alpha = (cosVal.pow(2f)).coerceIn(0.08f, 1f)

                        val tickHeight = when {
                            isMajor -> 36.dp.toPx()
                            isMid -> 24.dp.toPx()
                            else -> 14.dp.toPx()
                        } * cosVal

                        val tickWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                        val tickColor = if (isMajor) primaryColor.copy(alpha = alpha) else onSurfaceColor.copy(alpha = alpha * 0.6f)

                        drawLine(
                            color = tickColor,
                            start = Offset(posX, centerY - tickHeight / 2f),
                            end = Offset(posX, centerY + tickHeight / 2f),
                            strokeWidth = tickWidth
                        )
                    }

                    // Left & Right cylinder edge shadow vignettes
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent),
                            startX = 0f,
                            endX = width * 0.22f
                        ),
                        size = Size(width * 0.22f, height)
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                            startX = width * 0.78f,
                            endX = width
                        ),
                        topLeft = Offset(width * 0.78f, 0f),
                        size = Size(width * 0.22f, height)
                    )

                    // Center Illuminated Aim / Marker Needle
                    val markerX = width / 2f
                    drawLine(
                        color = primaryColor,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, height),
                        strokeWidth = 2.5.dp.toPx()
                    )

                    // Top and Bottom pointer triangles
                    val pointerPathTop = Path().apply {
                        moveTo(markerX - 6.dp.toPx(), 0f)
                        lineTo(markerX + 6.dp.toPx(), 0f)
                        lineTo(markerX, 8.dp.toPx())
                        close()
                    }
                    drawPath(pointerPathTop, color = primaryColor)

                    val pointerPathBottom = Path().apply {
                        moveTo(markerX - 6.dp.toPx(), height)
                        lineTo(markerX + 6.dp.toPx(), height)
                        lineTo(markerX, height - 8.dp.toPx())
                        close()
                    }
                    drawPath(pointerPathBottom, color = primaryColor)
                }

                // Subtitle helper hint
                Text(
                    text = "⇄ ВЛЕВО-ВПРАВО ДЛЯ ВРАЩЕНИЯ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = Color.DarkGray
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}
