package com.example.workouttracker.presentation.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.CalculateOneRepMaxUseCase
import com.example.workouttracker.presentation.components.ChartDataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseId: Long? = null,
    val chartDataPoints: List<ChartDataPoint> = emptyList(),
    val maxOneRM: Double = 0.0,
    val totalVolume: Double = 0.0,
    val totalSessions: Int = 0,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculateOneRepMaxUseCase: CalculateOneRepMaxUseCase = CalculateOneRepMaxUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var allSessions: List<WorkoutSessionWithSets> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllSessions(),
                exerciseRepository.getAllExercises()
            ) { sessions, exercises ->
                Pair(sessions.filter { it.session.status == WorkoutStatus.COMPLETED }, exercises)
            }.collect { (sessions, exercises) ->
                allSessions = sessions
                val selectedId = _uiState.value.selectedExerciseId ?: exercises.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        exercises = exercises,
                        selectedExerciseId = selectedId,
                        isLoading = false
                    )
                }
                selectedId?.let { buildChartData(it) }
            }
        }
    }

    fun selectExercise(exerciseId: Long) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
        buildChartData(exerciseId)
    }

    private fun buildChartData(exerciseId: Long) {
        val points = mutableListOf<ChartDataPoint>()
        var maxOneRM = 0.0
        var totalVolume = 0.0
        var sessionCount = 0

        for (sessionWithSets in allSessions.sortedBy { it.session.date }) {
            val exerciseSets = sessionWithSets.sets.filter { it.exerciseId == exerciseId }
            if (exerciseSets.isEmpty()) continue

            sessionCount++
            val bestSet = exerciseSets.maxByOrNull { it.weightKg * it.reps } ?: continue
            val oneRM = calculateOneRepMaxUseCase.calculateEpley(bestSet.weightKg, bestSet.reps)
            val maxWeight = exerciseSets.maxOf { it.weightKg }
            val sessionVolume = exerciseSets.sumOf { it.weightKg * it.reps }
            totalVolume += sessionVolume

            if (oneRM > maxOneRM) maxOneRM = oneRM

            points.add(
                ChartDataPoint(
                    date = sessionWithSets.session.date,
                    estimatedOneRepMax = oneRM,
                    workingWeight = maxWeight
                )
            )
        }

        _uiState.update {
            it.copy(
                chartDataPoints = points,
                maxOneRM = maxOneRM,
                totalVolume = totalVolume,
                totalSessions = sessionCount
            )
        }
    }
}

class AnalyticsViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(workoutRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
