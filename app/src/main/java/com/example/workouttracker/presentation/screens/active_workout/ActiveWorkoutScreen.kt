package com.example.workouttracker.presentation.screens.active_workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import com.example.workouttracker.domain.model.SetType
import com.example.workouttracker.presentation.components.PlateCalculatorDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.presentation.components.NumericWeightKeypad
import com.example.workouttracker.presentation.components.PrimaryActionButton
import java.util.Locale

/**
 * Ultra-Compact Zero-Scroll Active Workout Screen with Custom Exercise Creation
 * and Muscle Group Filtering/Sorting.
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
        snackbarHost = {}
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
                        onOpenAddExerciseDialog = { viewModel.openAddExerciseDialog(true) },
                        onOpenQuickSwap = { viewModel.openQuickSwapDialog(true) },
                        onOpenPlateCalculator = { viewModel.openPlateCalculator(true) },
                        onSelectExercise = { viewModel.selectExercise(it) },
                        onIncrementWeight = { viewModel.incrementWeight(it) },
                        onSetWeight = { viewModel.setWeight(it) },
                        onSetReps = { viewModel.setReps(it) },
                        onSetRir = { viewModel.setRir(it) },
                        onSetSetType = { viewModel.setSetType(it) },
                        onSaveSet = { viewModel.saveSet() },
                        onDeleteSet = { viewModel.deleteSet(it) },
                        onToggleKeypad = { viewModel.toggleNumericKeypad(!uiState.isNumericKeypadOpen) },
                        onUpdateRawWeight = { viewModel.updateRawWeightString(it) },
                        onPauseResumeTimer = { viewModel.pauseResumeTimer() },
                        onSkipTimer = { viewModel.skipTimer() }
                    )
                }
            }

            // Plate Calculator Dialog
            if (uiState.isPlateCalculatorOpen) {
                PlateCalculatorDialog(
                    initialWeight = uiState.inputWeightKg,
                    onDismiss = { viewModel.openPlateCalculator(false) },
                    onApplyWeight = { weight ->
                        viewModel.setWeight(weight)
                    }
                )
            }

            // Quick Swap Exercise Dialog
            if (uiState.isQuickSwapDialogOpen) {
                QuickSwapDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.openQuickSwapDialog(false) },
                    onSwap = { newExId ->
                        viewModel.quickSwapExercise(newExId)
                    }
                )
            }

            // Exercise Selection Dialog
            if (uiState.isAddExerciseDialogOpen) {
                ExerciseSelectionDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.openAddExerciseDialog(false) },
                    onExerciseSelected = { exerciseId ->
                        viewModel.selectExercise(exerciseId)
                        viewModel.openAddExerciseDialog(false)
                    },
                    onSearchQueryChange = { viewModel.setExerciseSearchQuery(it) },
                    onSelectMuscleCategory = { viewModel.setMuscleCategoryFilter(it) },
                    onSelectSortOrder = { viewModel.setExerciseSortOrder(it) },
                    onOpenCreateDialog = { viewModel.openCreateExerciseDialog(true) }
                )
            }

            // Create Custom Exercise Dialog
            if (uiState.isCreateExerciseDialogOpen) {
                CreateExerciseDialog(
                    categories = uiState.categories,
                    onDismiss = { viewModel.openCreateExerciseDialog(false) },
                    onCreateExercise = { name, catId, isBw, restSec, minStep, targetReps ->
                        viewModel.createCustomExercise(name, catId, isBw, restSec, minStep, targetReps)
                    }
                )
            }

            // Non-blocking Top Toast / Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
    onOpenAddExerciseDialog: () -> Unit,
    onOpenQuickSwap: () -> Unit,
    onOpenPlateCalculator: () -> Unit,
    onSelectExercise: (Long) -> Unit,
    onIncrementWeight: (Double) -> Unit,
    onSetWeight: (Double) -> Unit,
    onSetReps: (Int) -> Unit,
    onSetRir: (Int) -> Unit,
    onSetSetType: (SetType) -> Unit,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Deload Periodization Banner (if fatigue detected)
        uiState.deloadAdvice?.let { deload ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Разгрузочная неделя (Deload)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = deload.reasonRu,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

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

        // 2. Exercise Selection Row with Quick Swap
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenPlateCalculator,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Калькулятор блинов",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onOpenQuickSwap,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Быстрая замена",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenAddExerciseDialog,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 32.dp)
                    ) {
                        Text("Выбрать / +", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Adaptive Progression Recommendation Card (No Truncation / No Ellipsis)
        uiState.progressionResult?.let { prog ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Рекомендация нагрузки (No-AI)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "Цель: ${String.format(Locale.US, "%.1f", prog.recommendedWeightKg)} кг × ${prog.recommendedReps}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = prog.explanationRu,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        softWrap = true,
                        maxLines = Int.MAX_VALUE,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Inline input moved to the table below

        // 6. Completed Sets Grouped by Exercise (Gym Luxury Table)
        if (uiState.exerciseSetsMap.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выполненные упражнения (${uiState.exerciseSetsMap.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Всего сетов: ${uiState.totalSetsCount}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.exerciseSetsMap.forEach { (exerciseId, exerciseSets) ->
                    val exercise = uiState.exercises.find { it.id == exerciseId }
                    val isCurrentActive = activeExercise?.id == exerciseId
                    val exerciseVolume = exerciseSets.filter { it.setType != SetType.WARMUP }.sumOf { it.weightKg * it.reps }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentActive)
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            else
                                MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = if (isCurrentActive)
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Exercise Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectExercise(exerciseId) }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Text(
                                        text = exercise?.name ?: "Упражнение #$exerciseId",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCurrentActive) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Активно",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${exerciseSets.size} подх. • ${String.format(Locale.US, "%.0f", exerciseVolume)} кг",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!isCurrentActive) {
                                        FilledTonalButton(
                                            onClick = { onSelectExercise(exerciseId) },
                                            modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Выбрать", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }

                            // Gym Table: Structured List of Sets
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "СЕТ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = "ТИП",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Text(
                                        text = "ВЕС × ПОВТОРЕНИЯ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "RIR / 1RM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.width(76.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Spacer(modifier = Modifier.width(32.dp))
                                }

                                exerciseSets.forEachIndexed { _, set ->
                                    val set1RM = if (set.reps > 0) set.weightKg * (1.0 + set.reps / 30.0) else 0.0
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Set Number Badge
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${set.setNumber}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Set Type Chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (set.setType) {
                                                SetType.WARMUP -> Color(0xFF22C55E).copy(alpha = 0.18f)
                                                SetType.DROP_SET -> Color(0xFFF59E0B).copy(alpha = 0.18f)
                                                SetType.FAILURE -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                                SetType.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            },
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 3.dp)) {
                                                Text(
                                                    text = when (set.setType) {
                                                        SetType.WARMUP -> "Разминка"
                                                        SetType.DROP_SET -> "Дропсет"
                                                        SetType.FAILURE -> "Отказ"
                                                        SetType.NORMAL -> "Обычный"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = when (set.setType) {
                                                            SetType.WARMUP -> Color(0xFF22C55E)
                                                            SetType.DROP_SET -> Color(0xFFF59E0B)
                                                            SetType.FAILURE -> Color(0xFFEF4444)
                                                            SetType.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Large Bold Weight × Reps
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", set.weightKg)} кг × ${set.reps}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // RIR and 1RM
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.width(76.dp)
                                        ) {
                                            Text(
                                                text = "RIR ${set.rir}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            if (set.setType != SetType.WARMUP && set1RM > 0) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.0f", set1RM)} кг 1RM",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                )
                                            }
                                        }

                                        // Delete action
                                        IconButton(
                                            onClick = { onDeleteSet(set.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Удалить подход",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                
                                if (isCurrentActive) {
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Set Number / Type Toggler
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = when (uiState.selectedSetType) {
                                                SetType.WARMUP -> Color(0xFF22C55E).copy(alpha = 0.8f)
                                                SetType.DROP_SET -> Color(0xFFF59E0B).copy(alpha = 0.8f)
                                                SetType.FAILURE -> Color(0xFFEF4444).copy(alpha = 0.8f)
                                                SetType.NORMAL -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { 
                                                    val nextType = when (uiState.selectedSetType) {
                                                        SetType.NORMAL -> SetType.WARMUP
                                                        SetType.WARMUP -> SetType.DROP_SET
                                                        SetType.DROP_SET -> SetType.FAILURE
                                                        SetType.FAILURE -> SetType.NORMAL
                                                    }
                                                    onSetSetType(nextType)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val prefix = when(uiState.selectedSetType) {
                                                    SetType.WARMUP -> "W"
                                                    SetType.DROP_SET -> "D"
                                                    SetType.FAILURE -> "F"
                                                    SetType.NORMAL -> "$nextSetNumber"
                                                }
                                                Text(
                                                    text = prefix,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Weight Input
                                        OutlinedTextField(
                                            value = uiState.rawWeightString,
                                            onValueChange = onUpdateRawWeight,
                                            modifier = Modifier.width(76.dp).height(52.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            singleLine = true,
                                            placeholder = { Text("КГ", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("×", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Reps Input
                                        var repsText by remember(uiState.inputReps) { mutableStateOf(if(uiState.inputReps > 0) uiState.inputReps.toString() else "") }
                                        OutlinedTextField(
                                            value = repsText,
                                            onValueChange = { 
                                                repsText = it
                                                onSetReps(it.toIntOrNull() ?: 0) 
                                            },
                                            modifier = Modifier.width(64.dp).height(52.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            singleLine = true,
                                            placeholder = { Text("ПОВТ", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // RIR Input
                                        var rirText by remember(uiState.inputRir) { mutableStateOf(uiState.inputRir.toString()) }
                                        OutlinedTextField(
                                            value = rirText,
                                            onValueChange = { 
                                                rirText = it
                                                onSetRir(it.toIntOrNull() ?: 0) 
                                            },
                                            modifier = Modifier.width(60.dp).height(52.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            singleLine = true,
                                            label = { Text("RIR", fontSize = 9.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Save Button
                                        IconButton(
                                            onClick = onSaveSet,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Сохранить",
                                                tint = MaterialTheme.colorScheme.onPrimary
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
    }
}

/**
 * Exercise Selection Dialog with Search, Muscle Category Filter, Sorting, and "+ Создать упражнение" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelectionDialog(
    uiState: ActiveWorkoutUiState,
    onDismiss: () -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectMuscleCategory: (Long?) -> Unit,
    onSelectSortOrder: (ExerciseSortOrder) -> Unit,
    onOpenCreateDialog: () -> Unit
) {
    val exercises = uiState.filteredAndSortedExercises
    val categories = uiState.categories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбор упражнения",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                FilledTonalButton(
                    onClick = onOpenCreateDialog,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = uiState.exerciseSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск упражнения...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Muscle Group Categories Filter Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedMuscleCategoryId == null,
                            onClick = { onSelectMuscleCategory(null) },
                            label = { Text("Все группы") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = uiState.selectedMuscleCategoryId == category.id,
                            onClick = { onSelectMuscleCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }

                // Sorting Order Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExerciseSortOrder.values().forEach { order ->
                        val isSelected = uiState.exerciseSortOrder == order
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectSortOrder(order) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = order.titleRu,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (exercises.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(exercises) { exercise ->
                        val categoryName = categories.firstOrNull { it.id == exercise.categoryId }?.name ?: "Другое"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .clickable { onExerciseSelected(exercise.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (exercise.isBodyweight) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Свой вес",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
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
                Text("Закрыть")
            }
        }
    )
}

/**
 * Modal dialog for creating a brand new custom exercise with muscle group assignment.
 */
@Composable
private fun CreateExerciseDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCreateExercise: (name: String, categoryId: Long, isBodyweight: Boolean, restSec: Int, minStepKg: Double, targetReps: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(categories.firstOrNull()?.id ?: 1L) }
    var isBodyweight by remember { mutableStateOf(false) }
    var restSeconds by remember { mutableIntStateOf(90) }
    var minStepKg by remember { mutableDoubleStateOf(2.5) }
    var targetReps by remember { mutableIntStateOf(8) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Новое упражнение",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название упражнения") },
                    placeholder = { Text("Например: Жим гантелей под углом") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Muscle Group Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Группа мышц",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                // Bodyweight Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isBodyweight = !isBodyweight },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isBodyweight,
                        onCheckedChange = { isBodyweight = it }
                    )
                    Text("Упражнение с собственным весом (подтягивания, брусья и т.д.)", style = MaterialTheme.typography.bodySmall)
                }

                // Rest Time Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Отдых: $restSeconds сек", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(60, 90, 120, 180).forEach { s ->
                            FilledTonalButton(
                                onClick = { restSeconds = s },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("$s с", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Min Step Kg
                if (!isBodyweight) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Шаг веса: $minStepKg кг", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1.25, 2.5, 5.0).forEach { step ->
                                FilledTonalButton(
                                    onClick = { minStepKg = step },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("$step", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Target Reps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Целевые повторы: $targetReps", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(6, 8, 10, 12, 15).forEach { r ->
                            FilledTonalButton(
                                onClick = { targetReps = r },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("$r", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateExercise(name, selectedCategoryId, isBodyweight, restSeconds, minStepKg, targetReps)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Quick Swap Dialog — allows swapping the active exercise "on the fly" without losing workout progress.
 */
@Composable
private fun QuickSwapDialog(
    uiState: ActiveWorkoutUiState,
    onDismiss: () -> Unit,
    onSwap: (Long) -> Unit
) {
    val currentEx = uiState.activeExercise
    val sameCategoryExercises = uiState.exercises.filter {
        it.id != currentEx?.id && (currentEx == null || it.categoryId == currentEx.categoryId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Замена упражнения",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Тренажер занят? Замените упражнение на альтернативу для той же группы мышц:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sameCategoryExercises.isEmpty()) {
                    Text(
                        text = "В этой категории нет других упражнений. Создайте новое через меню выбора.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    sameCategoryExercises.forEach { ex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwap(ex.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (ex.isBodyweight) "Свой вес" else "Со снарядом",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Заменить",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

