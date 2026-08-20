package com.example.workouttracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workouttracker.data.local.dao.CategoryDao
import com.example.workouttracker.data.local.dao.ExerciseDao
import com.example.workouttracker.data.local.dao.ProgressConfigDao
import com.example.workouttracker.data.local.dao.SetEntryDao
import com.example.workouttracker.data.local.dao.WorkoutSessionDao
import com.example.workouttracker.data.local.entity.SetEntryEntity
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import com.example.workouttracker.data.repository.ExerciseRepositoryImpl
import com.example.workouttracker.data.repository.WorkoutRepositoryImpl
import com.example.workouttracker.domain.model.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomDatabaseStressTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var sessionDao: WorkoutSessionDao
    private lateinit var setDao: SetEntryDao
    private lateinit var configDao: ProgressConfigDao
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        categoryDao = database.categoryDao()
        exerciseDao = database.exerciseDao()
        sessionDao = database.workoutSessionDao()
        setDao = database.setEntryDao()
        configDao = database.progressConfigDao()

        categoryDao.insertCategories(PrepopulateData.categories)
        exerciseDao.insertExercises(PrepopulateData.exercises)
        configDao.insertProgressConfigs(PrepopulateData.defaultConfigs())

        workoutRepository = WorkoutRepositoryImpl(sessionDao, setDao)
        exerciseRepository = ExerciseRepositoryImpl(exerciseDao, categoryDao, configDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `stress test - inserting 100 sessions and 1000 sets maintains data integrity and ordering`() = runTest {
        val baseDate = 1700000000000L
        val sessionCount = 100
        val setsPerSession = 10

        val sessionIds = mutableListOf<Long>()
        for (i in 0 until sessionCount) {
            val sessionDate = baseDate + (i * 86400000L) // Each day
            val sId = sessionDao.insertSession(
                WorkoutSessionEntity(
                    date = sessionDate,
                    status = if (i % 2 == 0) WorkoutStatus.COMPLETED.name else WorkoutStatus.DRAFT.name,
                    notes = "Session #$i: Жим, Присед, Тяга (Тяжелая тренировка №$i)"
                )
            )
            sessionIds.add(sId)

            val sets = (1..setsPerSession).map { setNum ->
                SetEntryEntity(
                    workoutSessionId = sId,
                    exerciseId = ((setNum % 5) + 1).toLong(),
                    setNumber = setNum,
                    weightKg = 50.0 + (i * 0.5) + setNum,
                    reps = 8 + (setNum % 4),
                    rir = setNum % 3,
                    timestamp = sessionDate,
                    isCompleted = i % 2 == 0
                )
            }
            val insertedSetIds = setDao.insertSets(sets)
            assertThat(insertedSetIds).hasSize(setsPerSession)
        }

        // Verify total sessions
        val allSessions = sessionDao.getAllSessionsList()
        assertThat(allSessions).hasSize(sessionCount)

        // Verify ordering (DESC by date)
        for (i in 0 until allSessions.size - 1) {
            assertThat(allSessions[i].date).isAtLeast(allSessions[i + 1].date)
        }

        // Verify Date Range query
        val midStart = baseDate + (25 * 86400000L)
        val midEnd = baseDate + (74 * 86400000L)
        val rangeSessions = sessionDao.getSessionsByDateRangeList(midStart, midEnd)
        assertThat(rangeSessions).hasSize(50) // days 25 to 74 inclusive

        for (s in rangeSessions) {
            assertThat(s.date).isAtLeast(midStart)
            assertThat(s.date).isAtMost(midEnd)
            val sessionSets = setDao.getSetsForSessionSync(s.id)
            assertThat(sessionSets).hasSize(setsPerSession)
        }

        // Batch update sets
        val firstSessionSets = setDao.getSetsForSessionSync(sessionIds[0])
        val updatedSet = firstSessionSets[0].copy(weightKg = 999.0, reps = 99)
        setDao.updateSet(updatedSet)

        val retrievedUpdated = setDao.getSetsForSessionSync(sessionIds[0])[0]
        assertThat(retrievedUpdated.weightKg).isEqualTo(999.0)
        assertThat(retrievedUpdated.reps).isEqualTo(99)
    }

    @Test
    fun `stress test - cloning session across 20 distinct target dates preserves exercises, order, and draft purity`() = runTest {
        val originalDate = 1600000000000L
        val sourceSessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                date = originalDate,
                status = WorkoutStatus.COMPLETED.name,
                notes = "Heavy Chest & Triceps Template (Русские заметки)"
            )
        )

        // 5 exercises with multiple sets each (total 15 sets)
        val sourceTemplateSets = listOf(
            // Exercise 1: Жим лежа (3 sets)
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 1L, setNumber = 1, weightKg = 100.0, reps = 5, rir = 2, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 1L, setNumber = 2, weightKg = 105.0, reps = 5, rir = 1, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 1L, setNumber = 3, weightKg = 110.0, reps = 3, rir = 0, isCompleted = true),
            // Exercise 2: Жим гантелей (3 sets)
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 2L, setNumber = 1, weightKg = 32.0, reps = 8, rir = 2, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 2L, setNumber = 2, weightKg = 32.0, reps = 8, rir = 1, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 2L, setNumber = 3, weightKg = 34.0, reps = 6, rir = 0, isCompleted = true),
            // Exercise 3: Брусья (3 sets)
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 3L, setNumber = 1, weightKg = 0.0, reps = 12, rir = 3, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 3L, setNumber = 2, weightKg = 10.0, reps = 8, rir = 2, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 3L, setNumber = 3, weightKg = 15.0, reps = 6, rir = 1, isCompleted = true),
            // Exercise 14: Сгибания на бицепс (3 sets)
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 14L, setNumber = 1, weightKg = 35.0, reps = 10, rir = 2, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 14L, setNumber = 2, weightKg = 35.0, reps = 10, rir = 1, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 14L, setNumber = 3, weightKg = 40.0, reps = 8, rir = 0, isCompleted = true),
            // Exercise 15: Французский жим (3 sets)
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 15L, setNumber = 1, weightKg = 40.0, reps = 10, rir = 2, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 15L, setNumber = 2, weightKg = 42.5, reps = 8, rir = 1, isCompleted = true),
            SetEntryEntity(workoutSessionId = sourceSessionId, exerciseId = 15L, setNumber = 3, weightKg = 45.0, reps = 6, rir = 0, isCompleted = true)
        )
        setDao.insertSets(sourceTemplateSets)

        // Clone to 20 different dates
        val cloneDates = (1..20).map { originalDate + (it * 7 * 86400000L) }
        val clonedSessionIds = mutableListOf<Long>()

        for (targetDate in cloneDates) {
            val clonedId = workoutRepository.cloneSession(sourceSessionId, targetDate)
            clonedSessionIds.add(clonedId)
        }

        // Verify uniqueness of all cloned session IDs
        assertThat(clonedSessionIds.toSet()).hasSize(20)
        assertThat(clonedSessionIds).doesNotContain(sourceSessionId)

        // Verify each cloned session
        for ((index, clonedId) in clonedSessionIds.withIndex()) {
            val targetDate = cloneDates[index]
            val clonedSession = sessionDao.getSessionByIdSync(clonedId)
            assertThat(clonedSession).isNotNull()
            assertThat(clonedSession?.date).isEqualTo(targetDate)
            assertThat(clonedSession?.status).isEqualTo(WorkoutStatus.DRAFT.name)
            assertThat(clonedSession?.notes).isEqualTo("Heavy Chest & Triceps Template (Русские заметки)")

            val clonedSets = setDao.getSetsForSessionSync(clonedId)
            assertThat(clonedSets).hasSize(15)

            for (i in sourceTemplateSets.indices) {
                val original = sourceTemplateSets[i]
                val cloned = clonedSets[i]

                assertThat(cloned.workoutSessionId).isEqualTo(clonedId)
                assertThat(cloned.exerciseId).isEqualTo(original.exerciseId)
                assertThat(cloned.setNumber).isEqualTo(original.setNumber)
                assertThat(cloned.weightKg).isEqualTo(original.weightKg)
                assertThat(cloned.reps).isEqualTo(original.reps)
                assertThat(cloned.rir).isEqualTo(original.rir)
                assertThat(cloned.timestamp).isEqualTo(targetDate)
                assertThat(cloned.isCompleted).isFalse() // Crucial: draft sets must not be marked completed
            }
        }

        // Verify source session is completely untouched
        val originalSessionAfter = sessionDao.getSessionByIdSync(sourceSessionId)
        assertThat(originalSessionAfter?.status).isEqualTo(WorkoutStatus.COMPLETED.name)
        val originalSetsAfter = setDao.getSetsForSessionSync(sourceSessionId)
        assertThat(originalSetsAfter).hasSize(15)
        assertThat(originalSetsAfter.all { it.isCompleted }).isTrue()
    }

    @Test
    fun `cloning empty session creates draft session with zero sets without error`() = runTest {
        val emptySessionId = sessionDao.insertSession(
            WorkoutSessionEntity(date = 10000L, status = WorkoutStatus.COMPLETED.name, notes = "Empty session")
        )
        val clonedId = workoutRepository.cloneSession(emptySessionId, 20000L)
        val clonedSession = sessionDao.getSessionByIdSync(clonedId)
        assertThat(clonedSession).isNotNull()
        assertThat(clonedSession?.status).isEqualTo(WorkoutStatus.DRAFT.name)
        assertThat(setDao.getSetsForSessionSync(clonedId)).isEmpty()
    }

    @Test
    fun `cloning non-existent session throws IllegalArgumentException`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                workoutRepository.cloneSession(99999L, 20000L)
            }
        }
    }

    @Test
    fun `getLastCompletedSetForExercise query behavior - complex multi-date and draft matrix`() = runTest {
        val exerciseId = 1L // Жим штанги лежа

        // 1. When DB is empty for this exercise -> returns null
        val nullResult = setDao.getLastCompletedSetForExercise(exerciseId, 9999999999999L)
        assertThat(nullResult).isNull()

        // 2. When sets exist ONLY in DRAFT sessions -> returns null
        val draftDate1 = 100000L
        val draftSession1 = sessionDao.insertSession(
            WorkoutSessionEntity(date = draftDate1, status = WorkoutStatus.DRAFT.name)
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = draftSession1,
                exerciseId = exerciseId,
                setNumber = 1,
                weightKg = 80.0,
                reps = 10,
                rir = 2,
                isCompleted = true
            )
        )
        assertThat(setDao.getLastCompletedSetForExercise(exerciseId, 9999999999999L)).isNull()

        // 3. When COMPLETED session exists, but its sets have isCompleted = false -> returns null
        val compDate1 = 200000L
        val compSession1 = sessionDao.insertSession(
            WorkoutSessionEntity(date = compDate1, status = WorkoutStatus.COMPLETED.name)
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = compSession1,
                exerciseId = exerciseId,
                setNumber = 1,
                weightKg = 85.0,
                reps = 8,
                rir = 2,
                isCompleted = false // NOT completed
            )
        )
        assertThat(setDao.getLastCompletedSetForExercise(exerciseId, 9999999999999L)).isNull()

        // 4. Now mark that set as completed -> now returns this set
        setDao.updateSet(setDao.getSetsForSessionSync(compSession1)[0].copy(isCompleted = true))
        val firstCompleted = setDao.getLastCompletedSetForExercise(exerciseId, 9999999999999L)
        assertThat(firstCompleted).isNotNull()
        assertThat(firstCompleted?.weightKg).isEqualTo(85.0)

        // 5. Add second completed set to the same session with setNumber = 2
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = compSession1,
                exerciseId = exerciseId,
                setNumber = 2,
                weightKg = 90.0,
                reps = 6,
                rir = 1,
                isCompleted = true
            )
        )
        val highestSetInSession = setDao.getLastCompletedSetForExercise(exerciseId, 9999999999999L)
        assertThat(highestSetInSession?.setNumber).isEqualTo(2)
        assertThat(highestSetInSession?.weightKg).isEqualTo(90.0)

        // 6. Create newer COMPLETED session at compDate2 = 300000L with 3 sets
        val compDate2 = 300000L
        val compSession2 = sessionDao.insertSession(
            WorkoutSessionEntity(date = compDate2, status = WorkoutStatus.COMPLETED.name)
        )
        setDao.insertSet(SetEntryEntity(workoutSessionId = compSession2, exerciseId = exerciseId, setNumber = 1, weightKg = 95.0, reps = 5, rir = 2, isCompleted = true))
        setDao.insertSet(SetEntryEntity(workoutSessionId = compSession2, exerciseId = exerciseId, setNumber = 2, weightKg = 100.0, reps = 5, rir = 1, isCompleted = true))
        setDao.insertSet(SetEntryEntity(workoutSessionId = compSession2, exerciseId = exerciseId, setNumber = 3, weightKg = 105.0, reps = 3, rir = 0, isCompleted = true))

        // 7. Create even newer DRAFT session at draftDate2 = 400000L with huge weight
        val draftDate2 = 400000L
        val draftSession2 = sessionDao.insertSession(
            WorkoutSessionEntity(date = draftDate2, status = WorkoutStatus.DRAFT.name)
        )
        setDao.insertSet(SetEntryEntity(workoutSessionId = draftSession2, exerciseId = exerciseId, setNumber = 1, weightKg = 150.0, reps = 10, rir = 0, isCompleted = true))

        // Query before 500000L -> Must return set 3 from compDate2 (105.0 kg), ignoring draftDate2 (150.0 kg)
        val resultAfterDraft = setDao.getLastCompletedSetForExercise(exerciseId, 500000L)
        assertThat(resultAfterDraft).isNotNull()
        assertThat(resultAfterDraft?.workoutSessionId).isEqualTo(compSession2)
        assertThat(resultAfterDraft?.setNumber).isEqualTo(3)
        assertThat(resultAfterDraft?.weightKg).isEqualTo(105.0)

        // Query with exact beforeDate = compDate2 -> Returns set 3 from compDate2
        val exactMatch = setDao.getLastCompletedSetForExercise(exerciseId, compDate2)
        assertThat(exactMatch?.workoutSessionId).isEqualTo(compSession2)
        assertThat(exactMatch?.weightKg).isEqualTo(105.0)

        // Query with beforeDate = compDate2 - 1 -> Returns set 2 from compDate1 (90.0 kg)
        val beforeComp2 = setDao.getLastCompletedSetForExercise(exerciseId, compDate2 - 1)
        assertThat(beforeComp2?.workoutSessionId).isEqualTo(compSession1)
        assertThat(beforeComp2?.setNumber).isEqualTo(2)
        assertThat(beforeComp2?.weightKg).isEqualTo(90.0)

        // Query with beforeDate = compDate1 - 1 -> Returns null
        val beforeComp1 = setDao.getLastCompletedSetForExercise(exerciseId, compDate1 - 1)
        assertThat(beforeComp1).isNull()
    }

    @Test
    fun `getCompletedSetsForExercise returns all completed sets chronologically`() = runTest {
        val exerciseId = 1L // Жим штанги лежа

        val s1 = sessionDao.insertSession(WorkoutSessionEntity(date = 1000L, status = WorkoutStatus.COMPLETED.name))
        val s2 = sessionDao.insertSession(WorkoutSessionEntity(date = 2000L, status = WorkoutStatus.COMPLETED.name))
        val sDraft = sessionDao.insertSession(WorkoutSessionEntity(date = 3000L, status = WorkoutStatus.DRAFT.name))

        setDao.insertSet(SetEntryEntity(workoutSessionId = s1, exerciseId = exerciseId, setNumber = 1, weightKg = 90.0, reps = 10, rir = 2, isCompleted = true))
        setDao.insertSet(SetEntryEntity(workoutSessionId = s1, exerciseId = exerciseId, setNumber = 2, weightKg = 95.0, reps = 8, rir = 1, isCompleted = true))
        setDao.insertSet(SetEntryEntity(workoutSessionId = s2, exerciseId = exerciseId, setNumber = 1, weightKg = 100.0, reps = 5, rir = 1, isCompleted = true))
        // Draft set should be ignored
        setDao.insertSet(SetEntryEntity(workoutSessionId = sDraft, exerciseId = exerciseId, setNumber = 1, weightKg = 110.0, reps = 5, rir = 0, isCompleted = true))

        val completedSets = workoutRepository.getCompletedSetsForExercise(exerciseId)
        assertThat(completedSets).hasSize(3)
        assertThat(completedSets[0].weightKg).isEqualTo(90.0)
        assertThat(completedSets[1].weightKg).isEqualTo(95.0)
        assertThat(completedSets[2].weightKg).isEqualTo(100.0)
    }

    @Test
    fun `foreign key cascading delete - deleting session removes all associated set entries`() = runTest {
        val sId = sessionDao.insertSession(
            WorkoutSessionEntity(date = 100000L, status = WorkoutStatus.COMPLETED.name)
        )
        setDao.insertSet(SetEntryEntity(workoutSessionId = sId, exerciseId = 1L, setNumber = 1, weightKg = 100.0, reps = 5, rir = 2))
        setDao.insertSet(SetEntryEntity(workoutSessionId = sId, exerciseId = 1L, setNumber = 2, weightKg = 100.0, reps = 5, rir = 2))
        setDao.insertSet(SetEntryEntity(workoutSessionId = sId, exerciseId = 2L, setNumber = 1, weightKg = 30.0, reps = 10, rir = 1))

        assertThat(setDao.getSetsForSessionSync(sId)).hasSize(3)

        // Delete session
        sessionDao.deleteSession(sId)

        // Verify session is deleted
        assertThat(sessionDao.getSessionByIdSync(sId)).isNull()

        // Verify sets are cascaded and deleted
        assertThat(setDao.getSetsForSessionSync(sId)).isEmpty()
    }

    @Test
    fun `concurrent session and set operations execute reliably without deadlock`() = runTest {
        val deferreds = (1..10).map { index ->
            async {
                val sId = sessionDao.insertSession(
                    WorkoutSessionEntity(
                        date = 1700000000000L + (index * 1000L),
                        status = WorkoutStatus.DRAFT.name,
                        notes = "Concurrent worker #$index"
                    )
                )
                val sets = (1..5).map { setNum ->
                    SetEntryEntity(
                        workoutSessionId = sId,
                        exerciseId = 1L,
                        setNumber = setNum,
                        weightKg = 50.0 + index,
                        reps = 10,
                        rir = 2
                    )
                }
                setDao.insertSets(sets)
                sId
            }
        }

        val createdIds = deferreds.awaitAll()
        assertThat(createdIds).hasSize(10)

        for (sId in createdIds) {
            val session = sessionDao.getSessionByIdSync(sId)
            assertThat(session).isNotNull()
            val sets = setDao.getSetsForSessionSync(sId)
            assertThat(sets).hasSize(5)
        }
    }

    @Test
    fun `getActiveSession Flow behaves reactively when session status changes`() = runTest {
        val date1 = 10000L
        val date2 = 20000L

        // Initially no active session
        assertThat(workoutRepository.getActiveSession().first()).isNull()

        // Start new session (date1)
        val session1Id = workoutRepository.startNewSession(date1, "Session 1")
        val active1 = workoutRepository.getActiveSession().first()
        assertThat(active1).isNotNull()
        assertThat(active1?.session?.id).isEqualTo(session1Id)

        // Start newer session (date2)
        val session2Id = workoutRepository.startNewSession(date2, "Session 2")
        val active2 = workoutRepository.getActiveSession().first()
        assertThat(active2).isNotNull()
        assertThat(active2?.session?.id).isEqualTo(session2Id) // Returns latest by date

        // Complete newer session
        workoutRepository.completeSession(session2Id)

        // Now active session should fall back to session1 (the remaining DRAFT)
        val activeFallback = workoutRepository.getActiveSession().first()
        assertThat(activeFallback).isNotNull()
        assertThat(activeFallback?.session?.id).isEqualTo(session1Id)

        // Complete session1
        workoutRepository.completeSession(session1Id)

        // Now no active session
        assertThat(workoutRepository.getActiveSession().first()).isNull()
    }
}
