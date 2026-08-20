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
import com.example.workouttracker.domain.usecase.CreateExerciseUseCase
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
 * Exercise Sorting options
 */
enum class ExerciseSortOrder(val titleRu: String) {
    BY_CATEGORY("По группам мышц"),
    ALPHABETICAL("А–Я"),
    RECENT("Недавние")
}

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
    val isCreateExerciseDialogOpen: Boolean = false,
    val isNumericKeypadOpen: Boolean = false,
    val selectedMuscleCategoryId: Long? = null,
    val exerciseSearchQuery: String = "",
    val exerciseSortOrder: ExerciseSortOrder = ExerciseSortOrder.BY_CATEGORY,
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

    val filteredAndSortedExercises: List<Exercise>
        get() {
            var list = exercises
            if (selectedMuscleCategoryId != null) {
                list = list.filter { it.categoryId == selectedMuscleCategoryId }
            }
            if (exerciseSearchQuery.isNotBlank()) {
                list = list.filter { it.name.contains(exerciseSearchQuery, ignoreCase = true) }
            }
            return when (exerciseSortOrder) {
                ExerciseSortOrder.ALPHABETICAL -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
                ExerciseSortOrder.BY_CATEGORY -> list.sortedWith(compareBy({ it.categoryId }, { it.name }))
                ExerciseSortOrder.RECENT -> list
            }
        }
}

/**
 * ViewModel managing the active workout session, exercise creation, set entry state, fast logging click budget,
 * auto-population from historical sets, progression recommendations, and reactive rest timer.
 */
class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculateProgressionUseCase: CalculateProgressionUseCase = CalculateProgressionUseCase(),
    private val getAutoPopulatedValuesUseCase: GetAutoPopulatedValuesUseCase = GetAutoPopulatedValuesUseCase(workoutRepository),
    private val createExerciseUseCase: CreateExerciseUseCase = CreateExerciseUseCase(exerciseRepository),
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
                val prevSelectedId = _uiState.value.selectedExerciseId
                val newSelectedId = prevSelectedId
                    ?: activeSession?.sets?.lastOrNull()?.exerciseId
                    ?: _uiState.value.exercises.firstOrNull()?.id

                _uiState.update { current ->
                    current.copy(
                        sessionWithSets = activeSession,
                        selectedExerciseId = newSelectedId,
                        isLoading = false
                    )
                }

                if (prevSelectedId == null && newSelectedId != null) {
                    loadAutoPopulatedAndProgression(newSelectedId)
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
                val prevSelectedId = _uiState.value.selectedExerciseId
                val newSelectedId = prevSelectedId ?: exercises.firstOrNull()?.id

                _uiState.update { current ->
                    current.copy(
                        exercises = exercises,
                        categories = categories,
                        selectedExerciseId = newSelectedId,
                        isLoading = false
                    )
                }

                if (prevSelectedId == null && newSelectedId != null) {
                    loadAutoPopulatedAndProgression(newSelectedId)
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

    fun startNewWorkout() {
        viewModelScope.launch {
            val sessionId = workoutRepository.startNewSession(
                date = System.currentTimeMillis(),
                notes = "Силовая тренировка"
            )
            _uiState.update { it.copy(userMessage = "Тренировка начата") }
        }
    }

    fun selectExercise(exerciseId: Long) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
        loadAutoPopulatedAndProgression(exerciseId)
    }

    fun setMuscleCategoryFilter(categoryId: Long?) {
        _uiState.update { it.copy(selectedMuscleCategoryId = categoryId) }
    }

    fun setExerciseSearchQuery(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun setExerciseSortOrder(order: ExerciseSortOrder) {
        _uiState.update { it.copy(exerciseSortOrder = order) }
    }

    fun openCreateExerciseDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isCreateExerciseDialogOpen = isOpen) }
    }

    fun createCustomExercise(
        name: String,
        categoryId: Long,
        isBodyweight: Boolean,
        defaultRestTimeSeconds: Int,
        minStepKg: Double,
        targetReps: Int
    ) {
        viewModelScope.launch {
            val result = createExerciseUseCase.execute(
                name = name,
                categoryId = categoryId,
                isBodyweight = isBodyweight,
                defaultRestTimeSeconds = defaultRestTimeSeconds,
                minStepKg = minStepKg,
                targetReps = targetReps
            )
            result.onSuccess { newExerciseId ->
                _uiState.update {
                    it.copy(
                        isCreateExerciseDialogOpen = false,
                        isAddExerciseDialogOpen = false,
                        selectedExerciseId = newExerciseId,
                        userMessage = "Упражнение «$name» создано!"
                    )
                }
                loadAutoPopulatedAndProgression(newExerciseId)
            }.onFailure { error ->
                _uiState.update { it.copy(userMessage = error.message ?: "Ошибка создания упражнения") }
            }
        }
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

    fun updateRawWeightString(rawInput: String) {
        val parsed = KeypadSanitizer.parseWeight(rawInput)
        _uiState.update {
            it.copy(
                rawWeightString = rawInput,
                inputWeightKg = parsed
            )
        }
    }

    fun setReps(reps: Int) {
        _uiState.update { it.copy(inputReps = reps.coerceIn(1, 999)) }
    }

    fun setRir(rir: Int) {
        _uiState.update { it.copy(inputRir = rir.coerceIn(0, 5)) }
    }

    fun saveSet(customRestSeconds: Int? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val activeSession = currentState.sessionWithSets?.session
            val sessionId = activeSession?.id ?: workoutRepository.startNewSession(
                date = System.currentTimeMillis(),
                notes = "Силовая тренировка"
            )

            val exerciseId = currentState.selectedExerciseId
                ?: currentState.exercises.firstOrNull()?.id
                ?: 1L

            val existingSetsForExercise = currentState.sessionWithSets?.sets?.filter { it.exerciseId == exerciseId } ?: emptyList()
            val nextSetNumber = (existingSetsForExercise.maxOfOrNull { it.setNumber } ?: 0) + 1

            val newSet = SetEntry(
                id = 0,
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

            _uiState.update { current ->
                val currentSets = current.sessionWithSets?.sets ?: emptyList()
                val updatedSession = current.sessionWithSets?.copy(sets = currentSets + newSet)
                    ?: WorkoutSessionWithSets(
                        session = com.example.workouttracker.domain.model.WorkoutSession(id = sessionId, date = System.currentTimeMillis()),
                        sets = listOf(newSet)
                    )
                current.copy(
                    sessionWithSets = updatedSession,
                    isNumericKeypadOpen = false,
                    timerState = restTimerManager.timerState.value,
                    userMessage = "Подход №$nextSetNumber сохранён"
                )
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSet(setId)
        }
    }

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
