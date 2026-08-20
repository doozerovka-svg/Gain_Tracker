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
import com.example.workouttracker.data.local.entity.CategoryEntity
import com.example.workouttracker.data.local.entity.ExerciseEntity
import com.example.workouttracker.data.local.entity.ProgressConfigEntity
import com.example.workouttracker.data.local.entity.SetEntryEntity
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import com.example.workouttracker.data.repository.ExerciseRepositoryImpl
import com.example.workouttracker.data.repository.WorkoutRepositoryImpl
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomDatabaseDAOTest {

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

        // Insert pre-populated data for tests
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
    fun `prepopulated library contains Russian categories and exercises`() = runTest {
        val categories = categoryDao.getAllCategoriesList()
        assertThat(categories).isNotEmpty()
        val categoryNames = categories.map { it.name }
        assertThat(categoryNames).containsAtLeast("Грудные", "Спина", "Ноги", "Плечи", "Руки", "Пресс и кор")

        val exercises = exerciseDao.getAllExercisesList()
        assertThat(exercises).isNotEmpty()
        val exerciseNames = exercises.map { it.name }
        assertThat(exerciseNames).containsAtLeast(
            "Жим штанги лежа",
            "Подтягивания",
            "Приседания со штангой",
            "Армейский жим",
            "Сгибания на бицепс",
            "Брусья"
        )
    }

    @Test
    fun `query exercises by category returns only matching category exercises`() = runTest {
        // Category 1: Грудные
        val chestExercises = exerciseDao.getExercisesByCategory(1).first()
        assertThat(chestExercises).isNotEmpty()
        for (exercise in chestExercises) {
            assertThat(exercise.categoryId).isEqualTo(1)
        }
    }

    @Test
    fun `session lifecycle draft to completed and query by date range`() = runTest {
        val date1 = 1755000000000L
        val sessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                date = date1,
                status = WorkoutStatus.DRAFT.name,
                notes = "Тренировка груди"
            )
        )

        var activeSession = sessionDao.getActiveSessionSync()
        assertThat(activeSession).isNotNull()
        assertThat(activeSession?.id).isEqualTo(sessionId)
        assertThat(activeSession?.status).isEqualTo(WorkoutStatus.DRAFT.name)

        // Complete session
        sessionDao.completeSession(sessionId)

        activeSession = sessionDao.getActiveSessionSync()
        assertThat(activeSession).isNull()

        val completedSession = sessionDao.getSessionByIdSync(sessionId)
        assertThat(completedSession?.status).isEqualTo(WorkoutStatus.COMPLETED.name)

        // Query by date range
        val sessions = sessionDao.getSessionsByDateRangeList(date1 - 1000, date1 + 1000)
        assertThat(sessions).hasSize(1)
        assertThat(sessions.first().id).isEqualTo(sessionId)
    }

    @Test
    fun `set entries CRUD and query last completed set for exercise`() = runTest {
        // Create session 1 (Completed, Date T1)
        val date1 = 10000L
        val sessionId1 = sessionDao.insertSession(
            WorkoutSessionEntity(date = date1, status = WorkoutStatus.COMPLETED.name)
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sessionId1,
                exerciseId = 1L, // Жим лежа
                setNumber = 1,
                weightKg = 90.0,
                reps = 10,
                rir = 2,
                isCompleted = true
            )
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sessionId1,
                exerciseId = 1L,
                setNumber = 2,
                weightKg = 95.0,
                reps = 8,
                rir = 1,
                isCompleted = true
            )
        )

        // Create session 2 (Completed, Date T2 > T1)
        val date2 = 20000L
        val sessionId2 = sessionDao.insertSession(
            WorkoutSessionEntity(date = date2, status = WorkoutStatus.COMPLETED.name)
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sessionId2,
                exerciseId = 1L,
                setNumber = 1,
                weightKg = 100.0,
                reps = 8,
                rir = 1,
                isCompleted = true
            )
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sessionId2,
                exerciseId = 1L,
                setNumber = 2,
                weightKg = 105.0,
                reps = 6,
                rir = 0,
                isCompleted = true
            )
        )

        // Query last completed set before date2 + 100
        val lastSet = setDao.getLastCompletedSetForExercise(1L, date2 + 100)
        assertThat(lastSet).isNotNull()
        assertThat(lastSet?.weightKg).isEqualTo(105.0)
        assertThat(lastSet?.reps).isEqualTo(6)
        assertThat(lastSet?.setNumber).isEqualTo(2)

        // Query last completed set before date2
        val lastSetBeforeT2 = setDao.getLastCompletedSetForExercise(1L, date1 + 100)
        assertThat(lastSetBeforeT2).isNotNull()
        assertThat(lastSetBeforeT2?.weightKg).isEqualTo(95.0)
        assertThat(lastSetBeforeT2?.reps).isEqualTo(8)
    }

    @Test
    fun `last completed set returns null when exercise has no completed history`() = runTest {
        val lastSet = setDao.getLastCompletedSetForExercise(999L, System.currentTimeMillis())
        assertThat(lastSet).isNull()
    }

    @Test
    fun `workout repository cloneSession duplicates exercises, order and sets as draft`() = runTest {
        val originalDate = 10000L
        val targetDate = 20000L

        val sourceSessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                date = originalDate,
                status = WorkoutStatus.COMPLETED.name,
                notes = "День ног"
            )
        )

        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sourceSessionId,
                exerciseId = 8L, // Приседания
                setNumber = 1,
                weightKg = 120.0,
                reps = 8,
                rir = 2,
                isCompleted = true
            )
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sourceSessionId,
                exerciseId = 8L,
                setNumber = 2,
                weightKg = 125.0,
                reps = 6,
                rir = 1,
                isCompleted = true
            )
        )
        setDao.insertSet(
            SetEntryEntity(
                workoutSessionId = sourceSessionId,
                exerciseId = 9L, // Румынская тяга
                setNumber = 1,
                weightKg = 100.0,
                reps = 10,
                rir = 3,
                isCompleted = true
            )
        )

        // Execute clone
        val clonedSessionId = workoutRepository.cloneSession(sourceSessionId, targetDate)

        assertThat(clonedSessionId).isNotEqualTo(sourceSessionId)

        val clonedSession = sessionDao.getSessionByIdSync(clonedSessionId)
        assertThat(clonedSession).isNotNull()
        assertThat(clonedSession?.date).isEqualTo(targetDate)
        assertThat(clonedSession?.status).isEqualTo(WorkoutStatus.DRAFT.name)
        assertThat(clonedSession?.notes).isEqualTo("День ног")

        val clonedSets = setDao.getSetsForSessionSync(clonedSessionId)
        assertThat(clonedSets).hasSize(3)

        // Verify set 1
        assertThat(clonedSets[0].exerciseId).isEqualTo(8L)
        assertThat(clonedSets[0].setNumber).isEqualTo(1)
        assertThat(clonedSets[0].weightKg).isEqualTo(120.0)
        assertThat(clonedSets[0].reps).isEqualTo(8)
        assertThat(clonedSets[0].isCompleted).isFalse()

        // Verify set 2
        assertThat(clonedSets[1].exerciseId).isEqualTo(8L)
        assertThat(clonedSets[1].setNumber).isEqualTo(2)
        assertThat(clonedSets[1].weightKg).isEqualTo(125.0)
        assertThat(clonedSets[1].reps).isEqualTo(6)
        assertThat(clonedSets[1].isCompleted).isFalse()

        // Verify set 3
        assertThat(clonedSets[2].exerciseId).isEqualTo(9L)
        assertThat(clonedSets[2].setNumber).isEqualTo(1)
        assertThat(clonedSets[2].weightKg).isEqualTo(100.0)
        assertThat(clonedSets[2].reps).isEqualTo(10)
        assertThat(clonedSets[2].isCompleted).isFalse()
    }

    @Test
    fun `progress config CRUD and update`() = runTest {
        val config = exerciseRepository.getProgressConfig(1L)
        assertThat(config.exerciseId).isEqualTo(1L)
        assertThat(config.minStepKg).isEqualTo(2.5)

        // Update config
        val updatedConfig = config.copy(minStepKg = 1.25, targetReps = 10)
        exerciseRepository.updateProgressConfig(updatedConfig)

        val retrieved = exerciseRepository.getProgressConfig(1L)
        assertThat(retrieved.minStepKg).isEqualTo(1.25)
        assertThat(retrieved.targetReps).isEqualTo(10)
    }
}
