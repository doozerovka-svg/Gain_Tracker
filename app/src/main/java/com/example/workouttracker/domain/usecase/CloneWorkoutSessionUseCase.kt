package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.repository.WorkoutRepository

class CloneWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend fun execute(sourceSessionId: Long, targetDate: Long): Long {
        return workoutRepository.cloneSession(sourceSessionId, targetDate)
    }
}
