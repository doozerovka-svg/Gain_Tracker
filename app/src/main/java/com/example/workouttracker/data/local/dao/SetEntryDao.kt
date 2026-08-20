package com.example.workouttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workouttracker.data.local.entity.SetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {
    @Query("SELECT * FROM set_entries WHERE workoutSessionId = :sessionId ORDER BY id ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE workoutSessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetsForSessionSync(sessionId: Long): List<SetEntryEntity>

    @Query("""
        SELECT s.* FROM set_entries s
        INNER JOIN workout_sessions w ON s.workoutSessionId = w.id
        WHERE s.exerciseId = :exerciseId
          AND w.status = 'COMPLETED'
          AND w.date <= :beforeDate
          AND s.isCompleted = 1
        ORDER BY w.date DESC, s.setNumber DESC
        LIMIT 1
    """)
    suspend fun getLastCompletedSetForExercise(exerciseId: Long, beforeDate: Long): SetEntryEntity?

    @Query("""
        SELECT s.* FROM set_entries s
        INNER JOIN workout_sessions w ON s.workoutSessionId = w.id
        WHERE s.exerciseId = :exerciseId
          AND w.status = 'COMPLETED'
          AND s.isCompleted = 1
        ORDER BY w.date ASC, s.setNumber ASC
    """)
    suspend fun getCompletedSetsForExercise(exerciseId: Long): List<SetEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntryEntity>): List<Long>

    @Update
    suspend fun updateSet(set: SetEntryEntity): Int

    @Query("DELETE FROM set_entries WHERE id = :setId")
    suspend fun deleteSet(setId: Long): Int

    @Query("DELETE FROM set_entries WHERE workoutSessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long): Int
}
