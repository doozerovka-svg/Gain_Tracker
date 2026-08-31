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
    val strengthProfile: com.example.workouttracker.domain.usecase.StrengthProfile? = null,
    val deloadAdvice: com.example.workouttracker.domain.usecase.DeloadAdvice? = null,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculateOneRepMaxUseCase: CalculateOneRepMaxUseCase = CalculateOneRepMaxUseCase(),
    private val calculateStrengthRankUseCase: com.example.workouttracker.domain.usecase.CalculateStrengthRankUseCase = com.example.workouttracker.domain.usecase.CalculateStrengthRankUseCase(),
    private val checkDeloadRecommendationUseCase: com.example.workouttracker.domain.usecase.CheckDeloadRecommendationUseCase = com.example.workouttracker.domain.usecase.CheckDeloadRecommendationUseCase()
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
                
                val deload = checkDeloadRecommendationUseCase.evaluate(sessions.sortedByDescending { it.session.date })
                val profile = buildStrengthProfile(sessions, exercises)

                _uiState.update {
                    it.copy(
                        exercises = exercises,
                        selectedExerciseId = selectedId,
                        deloadAdvice = deload,
                        strengthProfile = profile,
                        isLoading = false
                    )
                }
                selectedId?.let { buildChartData(it) }
            }
        }
    }

    private fun buildStrengthProfile(
        sessions: List<WorkoutSessionWithSets>,
        exercises: List<Exercise>
    ): com.example.workouttracker.domain.usecase.StrengthProfile {
        fun getMax1RMForExercise(nameMatches: List<String>): Double {
            val matchingIds = exercises.filter { ex -> nameMatches.any { ex.name.contains(it, ignoreCase = true) } }.map { it.id }
            var max1RM = 0.0
            for (s in sessions) {
                val sets = s.sets.filter { matchingIds.contains(it.exerciseId) && it.setType != com.example.workouttracker.domain.model.SetType.WARMUP }
                for (st in sets) {
                    val oneRM = calculateOneRepMaxUseCase.calculateEpley(st.weightKg, st.reps)
                    if (oneRM > max1RM) max1RM = oneRM
                }
            }
            return max1RM
        }

        val bench = getMax1RMForExercise(listOf("Жим штанги лежа", "Жим лежа", "Bench"))
        val squat = getMax1RMForExercise(listOf("Приседания", "Squat"))
        val deadlift = getMax1RMForExercise(listOf("Становая тяга", "Deadlift"))
        val ohp = getMax1RMForExercise(listOf("Армейский жим", "Жим стоя", "OHP"))

        return calculateStrengthRankUseCase.buildProfile(
            bodyweightKg = 75.0,
            benchPress1RM = bench,
            squat1RM = squat,
            deadlift1RM = deadlift,
            ohp1RM = ohp
        )
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
            val workingSets = exerciseSets.filter { it.setType != com.example.workouttracker.domain.model.SetType.WARMUP }
            val setsForStats = if (workingSets.isNotEmpty()) workingSets else exerciseSets

            val bestSet = setsForStats.maxByOrNull { it.weightKg * it.reps } ?: continue
            val oneRM = calculateOneRepMaxUseCase.calculateEpley(bestSet.weightKg, bestSet.reps)
            val maxWeight = setsForStats.maxOf { it.weightKg }
            val sessionVolume = setsForStats.sumOf { it.weightKg * it.reps }
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
