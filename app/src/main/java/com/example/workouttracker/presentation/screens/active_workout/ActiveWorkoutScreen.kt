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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.presentation.components.DiscreteRirSlider
import com.example.workouttracker.presentation.components.NumericWeightKeypad
import com.example.workouttracker.presentation.components.PrimaryActionButton
import com.example.workouttracker.presentation.components.QuickWeightIncrementButtons
import com.example.workouttracker.presentation.components.RepsStepper
import com.example.workouttracker.presentation.components.RestTimerOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Active Workout Screen with Exercise List, Completed Sets Table,
 * Inline Set Entry with +X buttons, RIR Slider, Direct Keypad, and Rest Timer HUD.
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Активная тренировка",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (uiState.sessionWithSets != null) {
                        Button(
                            onClick = { viewModel.completeWorkout() },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Завершить", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Floating Rest Timer Overlay
            RestTimerOverlay(
                timerState = uiState.timerState,
                onAdd30s = { viewModel.addTimerSeconds(30) },
                onSub30s = { viewModel.subTimerSeconds(30) },
                onPauseResume = { viewModel.pauseResumeTimer() },
                onSkip = { viewModel.skipTimer() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.sessionWithSets == null -> {
                    EmptyWorkoutState(
                        onStartWorkout = { viewModel.startNewWorkout() }
                    )
                }
                else -> {
                    ActiveWorkoutContent(
                        uiState = uiState,
                        onSelectExercise = { viewModel.selectExercise(it) },
                        onOpenAddExerciseDialog = { viewModel.openAddExerciseDialog(true) },
                        onIncrementWeight = { viewModel.incrementWeight(it) },
                        onSetReps = { viewModel.setReps(it) },
                        onSetRir = { viewModel.setRir(it) },
                        onSaveSet = { viewModel.saveSet() },
                        onDeleteSet = { viewModel.deleteSet(it) },
                        onToggleKeypad = { viewModel.toggleNumericKeypad(!uiState.isNumericKeypadOpen) },
                        onUpdateRawWeight = { viewModel.updateRawWeightString(it) }
                    )
                }
            }

            // Exercise Selection Dialog
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Нет активной тренировки",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Нажмите «Начать тренировку», чтобы зафиксировать подходы, вес и повторения.",
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
private fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onSelectExercise: (Long) -> Unit,
    onOpenAddExerciseDialog: () -> Unit,
    onIncrementWeight: (Double) -> Unit,
    onSetReps: (Int) -> Unit,
    onSetRir: (Int) -> Unit,
    onSaveSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleKeypad: () -> Unit,
    onUpdateRawWeight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeExercise = uiState.activeExercise
    val setsForActiveExercise = activeExercise?.let { uiState.exerciseSetsMap[it.id] } ?: emptyList()
    val nextSetNumber = (setsForActiveExercise.maxOfOrNull { it.setNumber } ?: 0) + 1

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // Top Workout Info Banner
        item {
            WorkoutSessionSummaryCard(
                date = uiState.sessionWithSets?.session?.date ?: System.currentTimeMillis(),
                totalSets = uiState.totalSetsCount,
                totalVolumeKg = uiState.totalVolumeKg
            )
        }

        // Exercise Selector Strip
        item {
            ExerciseSelectorRow(
                exercises = uiState.exercises,
                selectedExerciseId = uiState.selectedExerciseId,
                onSelectExercise = onSelectExercise,
                onAddExerciseClick = onOpenAddExerciseDialog
            )
        }

        if (activeExercise != null) {
            // Active Exercise Header & Progression Hint
            item {
                ActiveExerciseHeaderCard(
                    exercise = activeExercise,
                    autoPopulated = uiState.autoPopulatedValues,
                    progressionResult = uiState.progressionResult
                )
            }

            // Completed Sets Table for this Exercise
            if (setsForActiveExercise.isNotEmpty()) {
                item {
                    CompletedSetsTable(
                        sets = setsForActiveExercise,
                        onDeleteSet = onDeleteSet
                    )
                }
            }

            // Active Set Entry Card (<= 4 click fast logging)
            item {
                ActiveSetEntryCard(
                    nextSetNumber = nextSetNumber,
                    weightKg = uiState.inputWeightKg,
                    rawWeightInput = uiState.rawWeightString,
                    reps = uiState.inputReps,
                    rir = uiState.inputRir,
                    isKeypadOpen = uiState.isNumericKeypadOpen,
                    onIncrementWeight = onIncrementWeight,
                    onSetReps = onSetReps,
                    onSetRir = onSetRir,
                    onSaveSet = onSaveSet,
                    onToggleKeypad = onToggleKeypad,
                    onUpdateRawWeight = onUpdateRawWeight
                )
            }
        }
    }
}

@Composable
private fun WorkoutSessionSummaryCard(
    date: Long,
    totalSets: Int,
    totalVolumeKg: Double,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru")) }
    val formattedDate = remember(date) { dateFormat.format(Date(date)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Подходов: $totalSets • Тоннаж: ${String.format(Locale.US, "%.1f", totalVolumeKg)} кг",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "В процессе",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ExerciseSelectorRow(
    exercises: List<Exercise>,
    selectedExerciseId: Long?,
    onSelectExercise: (Long) -> Unit,
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Упражнение",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            TextButton(
                onClick = onAddExerciseClick,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Выбрать / Сменить")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(exercises) { exercise ->
                val isSelected = exercise.id == selectedExerciseId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectExercise(exercise.id) },
                    label = { Text(exercise.name) },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveExerciseHeaderCard(
    exercise: Exercise,
    autoPopulated: com.example.workouttracker.domain.usecase.AutoPopulatedValues?,
    progressionResult: com.example.workouttracker.domain.model.ProgressionResult?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (autoPopulated != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Прошлый раз: ${String.format(Locale.US, "%.1f", autoPopulated.weightKg)} кг × ${autoPopulated.reps} (RIR ${autoPopulated.rir})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            if (progressionResult != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = progressionResult.explanationRu,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedSetsTable(
    sets: List<SetEntry>,
    onDeleteSet: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Выполненные подходы (${sets.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("№", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Text("Вес", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Text("Повт.", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Text("RIR", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.weight(1f))
            }

            // Table Rows
            sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${set.setNumber}",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", set.weightKg)} кг",
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${set.reps}",
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${set.rir}",
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(
                        onClick = { onDeleteSet(set.id) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить подход",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ActiveSetEntryCard(
    nextSetNumber: Int,
    weightKg: Double,
    rawWeightInput: String,
    reps: Int,
    rir: Int,
    isKeypadOpen: Boolean,
    onIncrementWeight: (Double) -> Unit,
    onSetReps: (Int) -> Unit,
    onSetRir: (Int) -> Unit,
    onSaveSet: () -> Unit,
    onToggleKeypad: () -> Unit,
    onUpdateRawWeight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Set Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Подход №$nextSetNumber",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = onToggleKeypad,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Прямой цифровой ввод"
                    )
                }
            }

            // Weight Control & Display
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Вес снаряда",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { onToggleKeypad() }
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", weightKg)} кг",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Quick Increment Buttons (+1, +2.5, +5, +10, +20 kg) strictly >=48dp touch targets
                QuickWeightIncrementButtons(
                    onIncrement = onIncrementWeight
                )

                // Numeric Keypad Drawer (Collapsible)
                AnimatedVisibility(
                    visible = isKeypadOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    NumericWeightKeypad(
                        currentInput = rawWeightInput,
                        onInputChange = onUpdateRawWeight,
                        onConfirm = onToggleKeypad
                    )
                }
            }

            HorizontalDivider()

            // Reps Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Повторения",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                RepsStepper(
                    reps = reps,
                    onRepsChange = onSetReps
                )
            }

            HorizontalDivider()

            // Discrete RIR Slider (0 to 5)
            DiscreteRirSlider(
                rirValue = rir,
                onRirChange = onSetRir
            )

            // Save Set Button (<=4 click budget fulfillment)
            PrimaryActionButton(
                text = "Сохранить подход",
                onClick = onSaveSet,
                icon = Icons.Default.Check
            )
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
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск упражнения...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Все") },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                            label = { Text(cat.name) },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                }

                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredExercises) { ex ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onExerciseSelected(ex.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ex.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Закрыть")
            }
        }
    )
}
