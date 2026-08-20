package com.example.workouttracker.presentation.screens.active_workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.presentation.components.NumericWeightKeypad
import com.example.workouttracker.presentation.components.PrimaryActionButton
import java.util.Locale

/**
 * Ultra-Compact Zero-Scroll Active Workout Screen.
 * Places Exercise Picker, Progression Target, Two-Column Weight & Reps inputs,
 * Segmented RIR bar, and Action button entirely in the primary view without vertical scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.sessionWithSets == null -> {
                    EmptyWorkoutState(onStartWorkout = { viewModel.startNewWorkout() })
                }
                else -> {
                    CompactWorkoutContent(
                        uiState = uiState,
                        onCompleteWorkout = { viewModel.completeWorkout() },
                        onSelectExercise = { viewModel.selectExercise(it) },
                        onOpenAddExerciseDialog = { viewModel.openAddExerciseDialog(true) },
                        onIncrementWeight = { viewModel.incrementWeight(it) },
                        onSetReps = { viewModel.setReps(it) },
                        onSetRir = { viewModel.setRir(it) },
                        onSaveSet = { viewModel.saveSet() },
                        onDeleteSet = { viewModel.deleteSet(it) },
                        onToggleKeypad = { viewModel.toggleNumericKeypad(!uiState.isNumericKeypadOpen) },
                        onUpdateRawWeight = { viewModel.updateRawWeightString(it) },
                        onPauseResumeTimer = { viewModel.pauseResumeTimer() },
                        onSkipTimer = { viewModel.skipTimer() }
                    )
                }
            }

            if (uiState.isAddExerciseDialogOpen) {
                ExerciseSelectionDialog(
                    exercises = uiState.exercises,
                    categories = uiState.categories,
                    onDismiss = { viewModel.openAddExerciseDialog(false) },
                    onExerciseSelected = { exerciseId ->
                        viewModel.selectExercise(exerciseId)
                        viewModel.openAddExerciseDialog(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkoutState(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Нет активной тренировки",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Нажмите «Начать тренировку», чтобы приступить к выполнению подходов.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PrimaryActionButton(
                    text = "Начать тренировку",
                    onClick = onStartWorkout,
                    icon = Icons.Default.PlayArrow
                )
            }
        }
    }
}

@Composable
private fun CompactWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onCompleteWorkout: () -> Unit,
    onSelectExercise: (Long) -> Unit,
    onOpenAddExerciseDialog: () -> Unit,
    onIncrementWeight: (Double) -> Unit,
    onSetReps: (Int) -> Unit,
    onSetRir: (Int) -> Unit,
    onSaveSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleKeypad: () -> Unit,
    onUpdateRawWeight: (String) -> Unit,
    onPauseResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeExercise = uiState.activeExercise
    val setsForActiveExercise = activeExercise?.let { uiState.exerciseSetsMap[it.id] } ?: emptyList()
    val nextSetNumber = (setsForActiveExercise.maxOfOrNull { it.setNumber } ?: 0) + 1
    val timerState = uiState.timerState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Compact Header Bar (Stats + Timer + Complete)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Подходов: ${uiState.totalSetsCount} • ${String.format(Locale.US, "%.0f", uiState.totalVolumeKg)} кг",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (timerState.isRunning) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%02d:%02d",
                                        timerState.remainingSeconds / 60,
                                        timerState.remainingSeconds % 60
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                                )
                                Icon(
                                    imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onPauseResumeTimer() }
                                )
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onSkipTimer() }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onCompleteWorkout,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                            .sizeIn(minWidth = 48.dp, minHeight = 36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Завершить", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 2. Exercise Selection Row & One-line Progression Hint
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeExercise?.name ?: "Выберите упражнение",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAddExerciseDialog() }
                    )

                    FilledTonalButton(
                        onClick = onOpenAddExerciseDialog,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 32.dp)
                    ) {
                        Text("Сменить", style = MaterialTheme.typography.labelSmall)
                    }
                }

                uiState.progressionResult?.let { prog ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = prog.explanationRu,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 3. Two-Column Input Grid (Weight on Left + Reps on Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Column: Weight
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Вес (кг)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = onToggleKeypad,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Stepper Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onIncrementWeight(-2.5) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                .size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("-2.5", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f", uiState.inputWeightKg),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )

                        FilledTonalButton(
                            onClick = { onIncrementWeight(2.5) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                .size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+2.5", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Quick Plate Chips (+1, +2.5, +5, +10 kg)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1.0, 2.5, 5.0, 10.0).forEach { inc ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 28.dp)
                                    .clickable { onIncrementWeight(inc) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "+${if (inc == inc.toLong().toDouble()) inc.toLong() else inc}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Reps
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Повторения", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // Reps Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onSetReps(Math.max(1, uiState.inputReps - 1)) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                .size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("-1", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Text(
                            text = "${uiState.inputReps}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )

                        FilledTonalButton(
                            onClick = { onSetReps(uiState.inputReps + 1) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                .size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+1", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Quick Rep Chips (6, 8, 10, 12)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(6, 8, 10, 12).forEach { r ->
                            val isSelected = uiState.inputReps == r
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 28.dp)
                                    .clickable { onSetReps(r) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "$r",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Numeric Keypad Drawer (Collapsible)
        AnimatedVisibility(
            visible = uiState.isNumericKeypadOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            NumericWeightKeypad(
                currentInput = uiState.rawWeightString,
                onInputChange = onUpdateRawWeight,
                onConfirm = onToggleKeypad
            )
        }

        // 4. Horizontal Segmented RIR Selector (0..5+)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RIR (запас сил)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = when (uiState.inputRir) {
                            0 -> "0: Отказ"
                            1 -> "1: Предел"
                            2 -> "2: Рабочий"
                            3 -> "3: Запас"
                            4 -> "4: Легко"
                            else -> "5+: Разминка"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rirLabels = listOf("0", "1", "2", "3", "4", "5+")
                    rirLabels.forEachIndexed { index, label ->
                        val isSelected = uiState.inputRir == index
                        val btnColor = when (index) {
                            0 -> Color(0xFFEF4444)
                            1 -> Color(0xFFF97316)
                            2 -> Color(0xFFEAB308)
                            3 -> Color(0xFF10B981)
                            4 -> Color(0xFF3B82F6)
                            else -> Color(0xFF6366F1)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) btnColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, btnColor) else null,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 36.dp)
                                .clickable { onSetRir(index) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) btnColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Main Action Button (>=48dp touch target)
        Button(
            onClick = onSaveSet,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .sizeIn(minHeight = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Зафиксировать подход #$nextSetNumber",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // 6. Horizontal Chips of Completed Sets for this Exercise
        if (setsForActiveExercise.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Выполненные подходы (${setsForActiveExercise.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(setsForActiveExercise) { set ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "#${set.setNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", set.weightKg)} кг × ${set.reps}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "RIR ${set.rir}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteSet(set.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelectionDialog(
    exercises: List<Exercise>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedCategoryId) {
        exercises.filter { ex ->
            val matchesCategory = selectedCategoryId == null || ex.categoryId == selectedCategoryId
            val matchesSearch = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выберите упражнение",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск упражнения...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Все") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onExerciseSelected(exercise.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                if (exercise.isBodyweight) {
                                    Text(
                                        text = "Свой вес",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
