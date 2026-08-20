package com.example.workouttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workouttracker.data.local.entity.ProgressConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressConfigDao {
    @Query("SELECT * FROM progress_configs WHERE exerciseId = :exerciseId")
    fun getProgressConfig(exerciseId: Long): Flow<ProgressConfigEntity?>

    @Query("SELECT * FROM progress_configs WHERE exerciseId = :exerciseId")
    suspend fun getProgressConfigSync(exerciseId: Long): ProgressConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressConfig(config: ProgressConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressConfigs(configs: List<ProgressConfigEntity>): List<Long>

    @Update
    suspend fun updateProgressConfig(config: ProgressConfigEntity): Int
}
