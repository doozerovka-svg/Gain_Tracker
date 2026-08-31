package com.example.workouttracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class PlateSpec(
    val weightKg: Double,
    val color: Color,
    val textColor: Color,
    val heightFraction: Float
)

val STANDARD_PLATES = listOf(
    PlateSpec(25.0, Color(0xFFD32F2F), Color.White, 1.0f),
    PlateSpec(20.0, Color(0xFF1976D2), Color.White, 0.95f),
    PlateSpec(15.0, Color(0xFFFBC02D), Color.Black, 0.85f),
    PlateSpec(10.0, Color(0xFF388E3C), Color.White, 0.75f),
    PlateSpec(5.0, Color(0xFFEEEEEE), Color.Black, 0.65f),
    PlateSpec(2.5, Color(0xFF212121), Color.White, 0.55f),
    PlateSpec(1.25, Color(0xFF90A4AE), Color.Black, 0.45f),
    PlateSpec(0.5, Color(0xFF78909C), Color.White, 0.35f)
)

data class CalculatedPlate(
    val spec: PlateSpec,
    val countPerSide: Int
)

fun calculatePlates(targetWeight: Double, barWeight: Double): Pair<List<CalculatedPlate>, Double> {
    if (targetWeight <= barWeight) {
        return Pair(emptyList(), 0.0)
    }

    var remainingPerSide = (targetWeight - barWeight) / 2.0
    val result = mutableListOf<CalculatedPlate>()

    for (spec in STANDARD_PLATES) {
        if (remainingPerSide >= spec.weightKg) {
            val count = (remainingPerSide / spec.weightKg).toInt()
            if (count > 0) {
                result.add(CalculatedPlate(spec, count))
                remainingPerSide -= count * spec.weightKg
                remainingPerSide = Math.round(remainingPerSide * 1000.0) / 1000.0
            }
        }
    }

    return Pair(result, remainingPerSide * 2.0)
}

@Composable
fun PlateCalculatorDialog(
    initialWeight: Double,
    onDismiss: () -> Unit,
    onApplyWeight: ((Double) -> Unit)? = null
) {
    var targetWeight by remember { mutableDoubleStateOf(if (initialWeight > 0) initialWeight else 60.0) }
    var barWeight by remember { mutableDoubleStateOf(20.0) }

    val (plates, remainder) = remember(targetWeight, barWeight) {
        calculatePlates(targetWeight, barWeight)
    }

    val totalPlatesWeightPerSide = plates.sumOf { it.spec.weightKg * it.countPerSide }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (onApplyWeight != null) {
                Button(
                    onClick = {
                        onApplyWeight(targetWeight)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Применить ${String.format(Locale.US, "%.1f", targetWeight)} кг", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        },
        dismissButton = {
            if (onApplyWeight != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Отмена")
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Калькулятор блинов",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Weight selector / display
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Целевой вес штанги",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", targetWeight),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "кг",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick delta buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(-10.0, -2.5, -1.25, +1.25, +2.5, +10.0).forEach { delta ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (delta > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            targetWeight = Math.max(0.0, targetWeight + delta)
                                        }
                                ) {
                                    Text(
                                        text = if (delta > 0) "+$delta" else "$delta",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        color = if (delta > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Barbell weight picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Гриф / Бар",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            20.0 to "20 кг",
                            15.0 to "15 кг",
                            10.0 to "10 кг",
                            0.0 to "0 кг"
                        ).forEach { (weight, label) ->
                            FilterChip(
                                selected = barWeight == weight,
                                onClick = { barWeight = weight },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Visual Barbell Sleeve representation
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Надеть на каждую сторону: ${String.format(Locale.US, "%.2f", totalPlatesWeightPerSide)} кг",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        // Sleeve graphic
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Collar / Stopper
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(64.dp)
                                        .background(Color(0xFF64748B), RoundedCornerShape(2.dp))
                                )

                                // Plates
                                if (plates.isEmpty()) {
                                    Text(
                                        text = "Только пустой гриф",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                } else {
                                    plates.forEach { item ->
                                        repeat(item.countPerSide) {
                                            Box(
                                                modifier = Modifier
                                                    .width(14.dp)
                                                    .height((64 * item.spec.heightFraction).dp)
                                                    .background(item.spec.color, RoundedCornerShape(2.dp))
                                                    .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (item.spec.weightKg % 1.0 == 0.0)
                                                        item.spec.weightKg.toInt().toString()
                                                    else
                                                        String.format(Locale.US, "%.1f", item.spec.weightKg),
                                                    color = item.spec.textColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Bar sleeve continuation
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .background(Color(0xFF475569), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }

                        // Breakdown list
                        if (plates.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                plates.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(item.spec.color, CircleShape)
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", item.spec.weightKg)} кг",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                            )
                                        }
                                        Text(
                                            text = "× ${item.countPerSide} шт. на сторону (${item.countPerSide * 2} всего)",
                                            color = Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        if (remainder > 0.0) {
                            Text(
                                text = "Не хватает ${String.format(Locale.US, "%.2f", remainder)} кг до точного веса",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    )
}
