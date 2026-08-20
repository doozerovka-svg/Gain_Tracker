package com.example.workouttracker.domain.repository

import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getActiveSession(): Flow<WorkoutSessionWithSets?>
    fun getSessionById(sessionId: Long): Flow<WorkoutSessionWithSets?>
    fun getAllSessions(): Flow<List<WorkoutSessionWithSets>>
    fun getSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSessionWithSets>>
    suspend fun startNewSession(date: Long = System.currentTimeMillis(), notes: String = ""): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun completeSession(sessionId: Long)
    suspend fun deleteSession(sessionId: Long)
    suspend fun getLastCompletedSetForExercise(exerciseId: Long, beforeDate: Long): SetEntry?
    suspend fun getCompletedSetsForExercise(exerciseId: Long): List<SetEntry>
    suspend fun insertSet(set: SetEntry): Long
    suspend fun updateSet(set: SetEntry)
    suspend fun deleteSet(setId: Long)
    suspend fun cloneSession(sourceSessionId: Long, targetDate: Long): Long
}
