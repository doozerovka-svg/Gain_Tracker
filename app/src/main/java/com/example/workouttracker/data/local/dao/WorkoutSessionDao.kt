package com.example.workouttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE status = 'DRAFT' ORDER BY date DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'DRAFT' ORDER BY date DESC LIMIT 1")
    suspend fun getActiveSessionSync(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionByIdSync(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    suspend fun getAllSessionsList(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getSessionsByDateRangeList(startDate: Long, endDate: Long): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity): Int

    @Query("UPDATE workout_sessions SET status = 'COMPLETED' WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long): Int

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long): Int
}
