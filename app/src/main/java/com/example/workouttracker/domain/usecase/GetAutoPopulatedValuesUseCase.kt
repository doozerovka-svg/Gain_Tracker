package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.repository.WorkoutRepository

data class AutoPopulatedValues(
    val weightKg: Double,
    val reps: Int,
    val rir: Int
)

class GetAutoPopulatedValuesUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend fun execute(exerciseId: Long, beforeDate: Long = System.currentTimeMillis()): AutoPopulatedValues? {
        val lastSet = workoutRepository.getLastCompletedSetForExercise(exerciseId, beforeDate) ?: return null
        return AutoPopulatedValues(
            weightKg = lastSet.weightKg,
            reps = lastSet.reps,
            rir = lastSet.rir
        )
    }
}
