package com.example.workouttracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Discrete 0 to 5 slider with step 1 and Russian semantic labels.
 * Interactive touch targets strictly >= 48x48 dp.
 */
@Composable
fun DiscreteRirSlider(
    rirValue: Int,
    onRirChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val clampedRir = rirValue.coerceIn(0, 5)
    val labelText = getRirDescription(clampedRir)
    val accentColor = getRirColor(clampedRir)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with current RIR badge and Russian explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RIR (в запасе)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                ) {
                    Text(
                        text = "RIR $clampedRir",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                }
            }

            // Semantic description banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = labelText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Discrete Slider with step 1 (steps = 4 gives discrete 0, 1, 2, 3, 4, 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = clampedRir.toFloat(),
                    onValueChange = { floatVal ->
                        onRirChange(floatVal.roundToInt().coerceIn(0, 5))
                    },
                    valueRange = 0f..5f,
                    steps = 4,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            // Direct 1-tap quick buttons (0..5) with touch targets >= 48x48 dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..5).forEach { rir ->
                    val isSelected = rir == clampedRir
                    val btnBgColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.surface,
                        label = "rir_btn_bg"
                    )
                    val btnTextColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        label = "rir_btn_text"
                    )

                    OutlinedButton(
                        onClick = { onRirChange(rir) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = btnBgColor,
                            contentColor = btnTextColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = isSelected)
                    ) {
                        Text(
                            text = rir.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = btnTextColor
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns exact Russian semantic description for RIR values 0..5.
 */
fun getRirDescription(rir: Int): String {
    return when (rir.coerceIn(0, 5)) {
        0 -> "0 — До отказа (0 в запасе)"
        1 -> "1 — Предельно тяжело (1 в запасе)"
        2 -> "2 — Тяжело (2 в запасе)"
        3 -> "3 — Умеренно (3 в запасе)"
        4 -> "4 — Легко (4 в запасе)"
        5 -> "5 — Разминка / Запас ≥ 5"
        else -> "RIR $rir"
    }
}

/**
 * Visual semantic color for RIR intensity.
 */
@Composable
fun getRirColor(rir: Int): Color {
    return when (rir.coerceIn(0, 5)) {
        0 -> Color(0xFFD32F2F) // Deep Red - Failure
        1 -> Color(0xFFE64A19) // Deep Orange - Very Heavy
        2 -> Color(0xFFF57C00) // Orange - Heavy
        3 -> Color(0xFFFBC02D) // Yellow - Moderate
        4 -> Color(0xFF689F38) // Light Green - Easy
        5 -> Color(0xFF388E3C) // Green - Warmup
        else -> MaterialTheme.colorScheme.primary
    }
}
