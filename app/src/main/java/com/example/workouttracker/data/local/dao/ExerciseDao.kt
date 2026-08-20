package com.example.workouttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workouttracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercisesList(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getExerciseById(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseByIdSync(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getExercisesByCategory(categoryId: Long): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int
}
