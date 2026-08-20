package com.example.workouttracker.data.repository

import com.example.workouttracker.data.local.dao.CategoryDao
import com.example.workouttracker.data.local.dao.ExerciseDao
import com.example.workouttracker.data.local.dao.ProgressConfigDao
import com.example.workouttracker.data.local.entity.ExerciseEntity
import com.example.workouttracker.data.local.entity.ProgressConfigEntity
import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val categoryDao: CategoryDao,
    private val progressConfigDao: ProgressConfigDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { list -> list.map { it.toDomain() } }
    }

    override fun getExerciseById(exerciseId: Long): Flow<Exercise?> {
        return exerciseDao.getExerciseById(exerciseId).map { it?.toDomain() }
    }

    override fun getExercisesByCategory(categoryId: Long): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByCategory(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertExercise(exercise: Exercise): Long {
        val exerciseId = exerciseDao.insertExercise(ExerciseEntity.fromDomain(exercise))
        progressConfigDao.insertProgressConfig(
            ProgressConfigEntity(
                exerciseId = exerciseId,
                minStepKg = if (exercise.isBodyweight) 1.25 else 2.5,
                progressionPercentHeavy = 0.05,
                progressionPercentModerate = 0.02,
                targetReps = 8,
                targetSets = 3,
                deloadPercent = 0.10
            )
        )
        return exerciseId
    }

    override suspend fun getProgressConfig(exerciseId: Long): ProgressConfig {
        val configEntity = progressConfigDao.getProgressConfigSync(exerciseId)
        return configEntity?.toDomain() ?: ProgressConfig(exerciseId = exerciseId)
    }

    override suspend fun updateProgressConfig(config: ProgressConfig) {
        progressConfigDao.updateProgressConfig(ProgressConfigEntity.fromDomain(config))
    }
}
