package com.example.workouttracker.data.repository

import com.example.workouttracker.data.local.dao.SetEntryDao
import com.example.workouttracker.data.local.dao.WorkoutSessionDao
import com.example.workouttracker.data.local.entity.SetEntryEntity
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: SetEntryDao
) : WorkoutRepository {

    override fun getActiveSession(): Flow<WorkoutSessionWithSets?> {
        return sessionDao.getActiveSession().flatMapLatest { sessionEntity ->
            if (sessionEntity == null) {
                flowOf(null)
            } else {
                setDao.getSetsForSession(sessionEntity.id).map { setEntities ->
                    WorkoutSessionWithSets(
                        session = sessionEntity.toDomain(),
                        sets = setEntities.map { it.toDomain() }
                    )
                }
            }
        }
    }

    override fun getSessionById(sessionId: Long): Flow<WorkoutSessionWithSets?> {
        return sessionDao.getSessionById(sessionId).flatMapLatest { sessionEntity ->
            if (sessionEntity == null) {
                flowOf(null)
            } else {
                setDao.getSetsForSession(sessionEntity.id).map { setEntities ->
                    WorkoutSessionWithSets(
                        session = sessionEntity.toDomain(),
                        sets = setEntities.map { it.toDomain() }
                    )
                }
            }
        }
    }

    override fun getAllSessions(): Flow<List<WorkoutSessionWithSets>> {
        return sessionDao.getAllSessions().map { sessionEntities ->
            sessionEntities.map { sessionEntity ->
                val sets = setDao.getSetsForSessionSync(sessionEntity.id)
                WorkoutSessionWithSets(
                    session = sessionEntity.toDomain(),
                    sets = sets.map { it.toDomain() }
                )
            }
        }
    }

    override fun getSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSessionWithSets>> {
        return sessionDao.getSessionsByDateRange(startDate, endDate).map { sessionEntities ->
            sessionEntities.map { sessionEntity ->
                val sets = setDao.getSetsForSessionSync(sessionEntity.id)
                WorkoutSessionWithSets(
                    session = sessionEntity.toDomain(),
                    sets = sets.map { it.toDomain() }
                )
            }
        }
    }

    override suspend fun startNewSession(date: Long, notes: String): Long {
        val entity = WorkoutSessionEntity(
            date = date,
            status = WorkoutStatus.DRAFT.name,
            notes = notes
        )
        return sessionDao.insertSession(entity)
    }

    override suspend fun updateSession(session: WorkoutSession) {
        sessionDao.updateSession(WorkoutSessionEntity.fromDomain(session))
    }

    override suspend fun completeSession(sessionId: Long) {
        sessionDao.completeSession(sessionId)
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSession(sessionId)
    }

    override suspend fun getLastCompletedSetForExercise(exerciseId: Long, beforeDate: Long): SetEntry? {
        return setDao.getLastCompletedSetForExercise(exerciseId, beforeDate)?.toDomain()
    }

    override suspend fun getCompletedSetsForExercise(exerciseId: Long): List<SetEntry> {
        return setDao.getCompletedSetsForExercise(exerciseId).map { it.toDomain() }
    }

    override suspend fun getLastUsedExerciseId(): Long? {
        return setDao.getLastUsedExerciseId()
    }

    override suspend fun insertSet(set: SetEntry): Long {
        return setDao.insertSet(SetEntryEntity.fromDomain(set))
    }

    override suspend fun updateSet(set: SetEntry) {
        setDao.updateSet(SetEntryEntity.fromDomain(set))
    }

    override suspend fun deleteSet(setId: Long) {
        setDao.deleteSet(setId)
    }

    override suspend fun cloneSession(sourceSessionId: Long, targetDate: Long): Long {
        val sourceSession = sessionDao.getSessionByIdSync(sourceSessionId)
            ?: throw IllegalArgumentException("Session not found: $sourceSessionId")
        val sourceSets = setDao.getSetsForSessionSync(sourceSessionId)

        val newSessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                date = targetDate,
                status = WorkoutStatus.DRAFT.name,
                notes = sourceSession.notes
            )
        )

        val clonedSets = sourceSets.map { set ->
            SetEntryEntity(
                workoutSessionId = newSessionId,
                exerciseId = set.exerciseId,
                setNumber = set.setNumber,
                weightKg = set.weightKg,
                reps = set.reps,
                rir = set.rir,
                timestamp = targetDate,
                isCompleted = false
            )
        }

        if (clonedSets.isNotEmpty()) {
            setDao.insertSets(clonedSets)
        }

        return newSessionId
    }
}
