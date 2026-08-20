package com.example.workouttracker.presentation.screens.active_workout

import com.example.workouttracker.domain.model.Category
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.CalculateProgressionUseCase
import com.example.workouttracker.domain.usecase.GetAutoPopulatedValuesUseCase
import com.example.workouttracker.timer.RestTimerManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
    private val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)

    private val activeSessionFlow = MutableStateFlow<WorkoutSessionWithSets?>(null)
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    private val sampleCategories = listOf(
        Category(id = 1, name = "Грудные"),
        Category(id = 2, name = "Спина")
    )

    private val sampleExercises = listOf(
        Exercise(id = 1, categoryId = 1, name = "Жим штанги лежа"),
        Exercise(id = 2, categoryId = 2, name = "Тяга в наклоне")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { workoutRepository.getActiveSession() } returns activeSessionFlow
        coEvery { exerciseRepository.getAllExercises() } returns exercisesFlow
        coEvery { exerciseRepository.getAllCategories() } returns categoriesFlow
        coEvery { exerciseRepository.getProgressConfig(any()) } returns ProgressConfig(exerciseId = 1, minStepKg = 2.5)

        exercisesFlow.value = sampleExercises
        categoriesFlow.value = sampleCategories
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ActiveWorkoutViewModel {
        val timerManager = RestTimerManager(scope = testScope)
        val calculateProgressionUseCase = CalculateProgressionUseCase()
        val getAutoPopulatedValuesUseCase = GetAutoPopulatedValuesUseCase(workoutRepository)
        return ActiveWorkoutViewModel(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            calculateProgressionUseCase = calculateProgressionUseCase,
            getAutoPopulatedValuesUseCase = getAutoPopulatedValuesUseCase,
            restTimerManager = timerManager
        )
    }

    @Test
    fun `initial state collects exercises and handles empty active session`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.sessionWithSets).isNull()
        assertThat(state.exercises).hasSize(2)
        assertThat(state.categories).hasSize(2)
        assertThat(state.selectedExerciseId).isEqualTo(1L)
    }

    @Test
    fun `startNewWorkout calls workoutRepository startNewSession`() = runTest(testDispatcher) {
        coEvery { workoutRepository.startNewSession(any(), any()) } returns 101L

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.startNewWorkout()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workoutRepository.startNewSession(any(), any()) }
    }

    @Test
    fun `selectExercise with history auto-populates weight, reps, and RIR`() = runTest(testDispatcher) {
        val lastSet = SetEntry(
            id = 55,
            workoutSessionId = 99,
            exerciseId = 1,
            setNumber = 3,
            weightKg = 100.0,
            reps = 8,
            rir = 1,
            isCompleted = true
        )
        coEvery { workoutRepository.getLastCompletedSetForExercise(1L, any()) } returns lastSet

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.selectExercise(1L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedExerciseId).isEqualTo(1L)
        assertThat(state.inputWeightKg).isEqualTo(100.0)
        assertThat(state.inputReps).isEqualTo(8)
        assertThat(state.inputRir).isEqualTo(1)
        assertThat(state.autoPopulatedValues).isNotNull()
        assertThat(state.progressionResult).isNotNull()
        // Progression result for exact 8 reps at RIR 1 recommends +1 plate step (102.5 kg)
        assertThat(state.progressionResult?.recommendedWeightKg).isEqualTo(102.5)
    }

    @Test
    fun `selectExercise with no history defaults gracefully to 0kg and 10 reps`() = runTest(testDispatcher) {
        coEvery { workoutRepository.getLastCompletedSetForExercise(2L, any()) } returns null

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.selectExercise(2L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedExerciseId).isEqualTo(2L)
        assertThat(state.inputWeightKg).isEqualTo(0.0)
        assertThat(state.inputReps).isEqualTo(10)
        assertThat(state.inputRir).isEqualTo(2)
        assertThat(state.autoPopulatedValues).isNull()
        assertThat(state.progressionResult).isNull()
    }

    @Test
    fun `quick increment buttons adjust weight correctly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setWeight(50.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(50.0)

        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(52.5)

        viewModel.incrementWeight(10.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(62.5)

        viewModel.incrementWeight(20.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(82.5)
    }

    @Test
    fun `numeric keypad updates weight and parses decimal input`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.updateRawWeightString("87.5")
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(87.5)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("87.5")
    }

    @Test
    fun `reps and discrete rir adjustments update state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setReps(12)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(12)

        viewModel.setRir(0)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(0)

        viewModel.setRir(5)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(5)
    }

    @Test
    fun `saveSet inserts SetEntry into repository and auto-starts rest timer`() = runTest(testDispatcher) {
        val session = WorkoutSession(id = 10, status = WorkoutStatus.DRAFT)
        val existingSet = SetEntry(
            id = 1,
            workoutSessionId = 10,
            exerciseId = 1,
            setNumber = 1,
            weightKg = 80.0,
            reps = 10,
            rir = 2
        )
        activeSessionFlow.value = WorkoutSessionWithSets(session, listOf(existingSet))

        coEvery { workoutRepository.insertSet(any()) } returns 2L

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.selectExercise(1L)
        testScheduler.advanceUntilIdle()
        viewModel.setWeight(85.0)
        viewModel.setReps(8)
        viewModel.setRir(1)

        viewModel.saveSet()
        testScheduler.runCurrent()

        coVerify {
            workoutRepository.insertSet(
                match { set ->
                    set.workoutSessionId == 10L &&
                    set.exerciseId == 1L &&
                    set.setNumber == 2 && // Auto-increments to set 2
                    set.weightKg == 85.0 &&
                    set.reps == 8 &&
                    set.rir == 1 &&
                    set.isCompleted
                }
            )
        }

        // Rest timer must be active (90s default)
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isTrue()
        assertThat(viewModel.restTimerManager.timerState.value.remainingSeconds).isEqualTo(90)
    }

    @Test
    fun `completeWorkout completes session and stops rest timer`() = runTest(testDispatcher) {
        val session = WorkoutSession(id = 15, status = WorkoutStatus.DRAFT)
        activeSessionFlow.value = WorkoutSessionWithSets(session, emptyList())

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.restTimerManager.startSetRest()
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isTrue()

        viewModel.completeWorkout()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workoutRepository.completeSession(15L) }
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isFalse()
    }

    @Test
    fun `fast set logging workflow completes in 3 clicks`() = runTest(testDispatcher) {
        val session = WorkoutSession(id = 20, status = WorkoutStatus.DRAFT)
        activeSessionFlow.value = WorkoutSessionWithSets(session, emptyList())
        coEvery { workoutRepository.insertSet(any()) } returns 1L

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // User already has selected exercise and auto-populated 100kg x 8 reps
        viewModel.setWeight(100.0)
        viewModel.setReps(8)

        // Click 1: Fast weight bump (+2.5 kg)
        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(102.5)

        // Click 2: Direct 1-tap RIR adjustment (RIR 1)
        viewModel.setRir(1)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(1)

        // Click 3: Tap "Save Set"
        viewModel.saveSet()
        testScheduler.runCurrent()

        // Verified set saved in 3 clicks (<= 4 click budget requirement)
        coVerify(exactly = 1) {
            workoutRepository.insertSet(
                match { it.weightKg == 102.5 && it.reps == 8 && it.rir == 1 }
            )
        }
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isTrue()
    }
}
