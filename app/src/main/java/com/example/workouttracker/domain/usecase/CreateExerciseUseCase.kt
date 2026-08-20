package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.repository.ExerciseRepository

/**
 * UseCase to validate and create a new custom exercise with default progression configuration.
 */
class CreateExerciseUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend fun execute(
        name: String,
        categoryId: Long,
        isBodyweight: Boolean = false,
        defaultRestTimeSeconds: Int = 90,
        minStepKg: Double = 2.5,
        targetReps: Int = 8
    ): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Название упражнения не может быть пустым"))
        }

        val exercise = Exercise(
            id = 0,
            name = trimmed,
            categoryId = categoryId,
            defaultRestTimeSeconds = defaultRestTimeSeconds,
            isBodyweight = isBodyweight
        )

        return try {
            val exerciseId = exerciseRepository.insertExercise(exercise)
            // Update custom config
            val config = ProgressConfig(
                exerciseId = exerciseId,
                minStepKg = if (isBodyweight) 1.25 else minStepKg,
                progressionPercentHeavy = 0.05,
                progressionPercentModerate = 0.02,
                targetReps = targetReps
            )
            exerciseRepository.updateProgressConfig(config)
            Result.success(exerciseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
