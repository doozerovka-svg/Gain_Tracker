package com.example.workouttracker.presentation.screens.active_workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.ProgressionResult
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.AutoPopulatedValues
import com.example.workouttracker.domain.usecase.CalculateProgressionUseCase
import com.example.workouttracker.domain.usecase.GetAutoPopulatedValuesUseCase
import com.example.workouttracker.presentation.components.KeypadSanitizer
import com.example.workouttracker.timer.RestTimerManager
import com.example.workouttracker.timer.RestTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * UI State for the Active Workout Screen.
 */
data class ActiveWorkoutUiState(
    val sessionWithSets: WorkoutSessionWithSets? = null,
    val exercises: List<Exercise> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedExerciseId: Long? = null,
    val inputWeightKg: Double = 0.0,
    val inputReps: Int = 10,
    val inputRir: Int = 2,
    val rawWeightString: String = "0",
    val autoPopulatedValues: AutoPopulatedValues? = null,
    val progressionResult: ProgressionResult? = null,
    val isAddExerciseDialogOpen: Boolean = false,
    val isNumericKeypadOpen: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val timerState: RestTimerState = RestTimerState()
) {
    val activeExercise: Exercise?
        get() = exercises.firstOrNull { it.id == selectedExerciseId }

    val exerciseSetsMap: Map<Long, List<SetEntry>>
        get() = sessionWithSets?.sets?.groupBy { it.exerciseId } ?: emptyMap()

    val totalSetsCount: Int
        get() = sessionWithSets?.sets?.size ?: 0

    val totalVolumeKg: Double
        get() = sessionWithSets?.sets?.sumOf { it.weightKg * it.reps } ?: 0.0
}

/**
 * ViewModel managing the active workout session, set entry state, fast logging click budget (<= 4 clicks),
 * auto-population from historical sets, progression recommendations, and reactive rest timer.
 */
class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculateProgressionUseCase: CalculateProgressionUseCase = CalculateProgressionUseCase(),
    private val getAutoPopulatedValuesUseCase: GetAutoPopulatedValuesUseCase = GetAutoPopulatedValuesUseCase(workoutRepository),
    val restTimerManager: RestTimerManager = RestTimerManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(isLoading = true))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    init {
        observeActiveSession()
        observeExercisesAndCategories()
        observeRestTimer()
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            workoutRepository.getActiveSession().collect { activeSession ->
                _uiState.update { current ->
                    val selectedId = current.selectedExerciseId
                        ?: activeSession?.sets?.lastOrNull()?.exerciseId
                        ?: current.exercises.firstOrNull()?.id

                    current.copy(
                        sessionWithSets = activeSession,
                        selectedExerciseId = selectedId,
                        isLoading = false
                    )
                }

                // If selected exercise changed or not initialized, trigger auto-population
                _uiState.value.selectedExerciseId?.let { exerciseId ->
                    loadAutoPopulatedAndProgression(exerciseId)
                }
            }
        }
    }

    private fun observeExercisesAndCategories() {
        viewModelScope.launch {
            combine(
                exerciseRepository.getAllExercises(),
                exerciseRepository.getAllCategories()
            ) { exercises, categories ->
                Pair(exercises, categories)
            }.collect { (exercises, categories) ->
                _uiState.update { current ->
                    val selectedId = current.selectedExerciseId ?: exercises.firstOrNull()?.id
                    current.copy(
                        exercises = exercises,
                        categories = categories,
                        selectedExerciseId = selectedId
                    )
                }
                _uiState.value.selectedExerciseId?.let { exerciseId ->
                    loadAutoPopulatedAndProgression(exerciseId)
                }
            }
        }
    }

    private fun observeRestTimer() {
        viewModelScope.launch {
            restTimerManager.timerState.collect { timerState ->
                _uiState.update { it.copy(timerState = timerState) }
            }
        }
    }

    /**
     * Start a new active workout session if none exists.
     */
    fun startNewWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newSessionId = workoutRepository.startNewSession()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Select active exercise to log sets for. Pre-fills weight, reps, RIR from history.
     */
    fun selectExercise(exerciseId: Long) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
        loadAutoPopulatedAndProgression(exerciseId)
    }

    private fun loadAutoPopulatedAndProgression(exerciseId: Long) {
        viewModelScope.launch {
            val autoPopulated = getAutoPopulatedValuesUseCase.execute(exerciseId)
            val progressConfig = try {
                exerciseRepository.getProgressConfig(exerciseId)
            } catch (_: Exception) {
                ProgressConfig(exerciseId = exerciseId)
            }

            val progressionResult = if (autoPopulated != null) {
                calculateProgressionUseCase.execute(
                    previousWeightKg = autoPopulated.weightKg,
                    actualReps = autoPopulated.reps,
                    actualRir = autoPopulated.rir,
                    config = progressConfig
                )
            } else null

            _uiState.update { current ->
                if (autoPopulated != null) {
                    val weight = autoPopulated.weightKg
                    val weightStr = if (weight % 1.0 == 0.0) weight.toInt().toString() else String.format(Locale.US, "%.1f", weight)
                    current.copy(
                        autoPopulatedValues = autoPopulated,
                        progressionResult = progressionResult,
                        inputWeightKg = weight,
                        inputReps = autoPopulated.reps,
                        inputRir = autoPopulated.rir,
                        rawWeightString = weightStr
                    )
                } else {
                    current.copy(
                        autoPopulatedValues = null,
                        progressionResult = null,
                        inputWeightKg = 0.0,
                        inputReps = 10,
                        inputRir = 2,
                        rawWeightString = "0"
                    )
                }
            }
        }
    }

    /**
     * Quick increment weight using +X buttons (+1, +2.5, +5, +10, +20 kg).
     */
    fun incrementWeight(delta: Double) {
        _uiState.update { current ->
            val newWeight = BigDecimal.valueOf((current.inputWeightKg + delta).coerceIn(0.0, 999.9))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            val weightStr = if (newWeight % 1.0 == 0.0) newWeight.toInt().toString() else String.format(Locale.US, "%.1f", newWeight)
            current.copy(
                inputWeightKg = newWeight,
                rawWeightString = weightStr
            )
        }
    }

    /**
     * Set weight directly from numeric keypad or picker.
     */
    fun setWeight(weight: Double) {
        val clamped = BigDecimal.valueOf(weight.coerceIn(0.0, 999.9))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
        val weightStr = if (clamped % 1.0 == 0.0) clamped.toInt().toString() else String.format(Locale.US, "%.1f", clamped)
        _uiState.update {
            it.copy(
                inputWeightKg = clamped,
                rawWeightString = weightStr
            )
        }
    }

    /**
     * Update raw weight input from numeric keypad.
     */
    fun updateRawWeightString(rawInput: String) {
        val parsed = KeypadSanitizer.parseWeight(rawInput)
        _uiState.update {
            it.copy(
                rawWeightString = rawInput,
                inputWeightKg = parsed
            )
        }
    }

    /**
     * Set repetition count.
     */
    fun setReps(reps: Int) {
        _uiState.update { it.copy(inputReps = reps.coerceIn(1, 999)) }
    }

    /**
     * Set discrete RIR (0 to 5).
     */
    fun setRir(rir: Int) {
        _uiState.update { it.copy(inputRir = rir.coerceIn(0, 5)) }
    }

    /**
     * Save active set entry.
     * Completes set logging in <= 4 clicks, persists to Room DB, and auto-starts Rest Timer.
     */
    fun saveSet(customRestSeconds: Int? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val activeSession = currentState.sessionWithSets?.session
            val sessionId = activeSession?.id ?: workoutRepository.startNewSession()

            val exerciseId = currentState.selectedExerciseId
                ?: currentState.exercises.firstOrNull()?.id
                ?: 1L

            val existingSetsForExercise = currentState.sessionWithSets?.sets?.filter { it.exerciseId == exerciseId } ?: emptyList()
            val nextSetNumber = (existingSetsForExercise.maxOfOrNull { it.setNumber } ?: 0) + 1

            val newSet = SetEntry(
                workoutSessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = nextSetNumber,
                weightKg = currentState.inputWeightKg,
                reps = currentState.inputReps,
                rir = currentState.inputRir,
                timestamp = System.currentTimeMillis(),
                isCompleted = true
            )

            workoutRepository.insertSet(newSet)

            // Trigger Rest Timer
            restTimerManager.startSetRest(customRestSeconds)

            _uiState.update {
                it.copy(
                    isNumericKeypadOpen = false,
                    timerState = restTimerManager.timerState.value,
                    userMessage = "Подход №$nextSetNumber сохранён"
                )
            }
        }
    }

    /**
     * Delete a set entry by ID.
     */
    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSet(setId)
        }
    }

    /**
     * Complete active workout session.
     */
    fun completeWorkout() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionWithSets?.session?.id ?: return@launch
            workoutRepository.completeSession(sessionId)
            restTimerManager.skipTimer()
            _uiState.update {
                it.copy(
                    timerState = restTimerManager.timerState.value,
                    userMessage = "Тренировка успешно завершена!"
                )
            }
        }
    }

    /**
     * Cancel/Discard active workout session.
     */
    fun cancelWorkout() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionWithSets?.session?.id ?: return@launch
            workoutRepository.deleteSession(sessionId)
            restTimerManager.skipTimer()
            _uiState.update {
                it.copy(
                    timerState = restTimerManager.timerState.value
                )
            }
        }
    }

    fun openAddExerciseDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isAddExerciseDialogOpen = isOpen) }
    }

    fun toggleNumericKeypad(isOpen: Boolean) {
        _uiState.update { it.copy(isNumericKeypadOpen = isOpen) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // Rest Timer Passthroughs
    fun addTimerSeconds(seconds: Int = 30) = restTimerManager.addSeconds(seconds)
    fun subTimerSeconds(seconds: Int = 30) = restTimerManager.subtractSeconds(seconds)
    fun pauseResumeTimer() {
        if (restTimerManager.timerState.value.isPaused) {
            restTimerManager.resumeTimer()
        } else {
            restTimerManager.pauseTimer()
        }
    }
    fun skipTimer() = restTimerManager.skipTimer()
}

/**
 * Factory for creating ActiveWorkoutViewModel with application repository dependencies.
 */
class ActiveWorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
            return ActiveWorkoutViewModel(workoutRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
