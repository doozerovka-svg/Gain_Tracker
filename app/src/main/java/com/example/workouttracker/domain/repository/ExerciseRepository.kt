package com.example.workouttracker.domain.repository

import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>
    fun getExercisesByCategory(categoryId: Long): Flow<List<Exercise>>
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun getProgressConfig(exerciseId: Long): ProgressConfig
    suspend fun updateProgressConfig(config: ProgressConfig)
}
