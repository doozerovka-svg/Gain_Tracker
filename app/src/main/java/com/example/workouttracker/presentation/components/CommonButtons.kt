package com.example.workouttracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Quick increment buttons (+1, +2.5, +5, +10, +20 kg) with interactive touch targets strictly >= 48x48 dp.
 */
@Composable
fun QuickWeightIncrementButtons(
    onIncrement: (Double) -> Unit,
    modifier: Modifier = Modifier,
    steps: List<Double> = listOf(1.0, 2.5, 5.0, 10.0, 20.0),
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { step ->
            val label = if (step % 1.0 == 0.0) {
                String.format(Locale.US, "+%d", step.toInt())
            } else {
                String.format(Locale.US, "+%.1f", step)
            }

            FilledTonalButton(
                onClick = { onIncrement(step) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Stepper for Reps with interactive touch targets strictly >= 48x48 dp.
 */
@Composable
fun RepsStepper(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minReps: Int = 1,
    maxReps: Int = 999,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { if (reps > minReps) onRepsChange(reps - 1) },
            enabled = enabled && reps > minReps,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Уменьшить повторения"
            )
        }

        Surface(
            modifier = Modifier
                .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = reps.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        IconButton(
            onClick = { if (reps < maxReps) onRepsChange(reps + 1) },
            enabled = enabled && reps < maxReps,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Увеличить повторения"
            )
        }
    }
}

/**
 * Primary full-width action button with guaranteed >= 48dp touch target height.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
