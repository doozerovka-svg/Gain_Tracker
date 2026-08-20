package com.example.workouttracker.presentation

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.presentation.components.KeypadSanitizer
import com.example.workouttracker.presentation.components.getRirDescription
import com.example.workouttracker.presentation.screens.active_workout.ActiveWorkoutViewModel
import com.example.workouttracker.timer.RestTimerManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Adversarial Challenger Test Suite for Milestone 2:
 * Stress-tests touch targets, boundary inputs, click budget violations, keypad edge cases,
 * and reactive timer concurrency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutAdversarialTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
    private val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)

    private val activeSessionFlow = MutableStateFlow<WorkoutSessionWithSets?>(null)
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { workoutRepository.getActiveSession() } returns activeSessionFlow
        coEvery { exerciseRepository.getAllExercises() } returns exercisesFlow
        coEvery { exerciseRepository.getAllCategories() } returns MutableStateFlow(emptyList())
        coEvery { exerciseRepository.getProgressConfig(any()) } returns ProgressConfig(exerciseId = 1, minStepKg = 2.5)

        exercisesFlow.value = listOf(
            Exercise(id = 1, categoryId = 1, name = "Жим штанги лежа"),
            Exercise(id = 2, categoryId = 1, name = "Приседания со штангой")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ActiveWorkoutViewModel {
        val timerManager = RestTimerManager(scope = testScope)
        return ActiveWorkoutViewModel(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            restTimerManager = timerManager
        )
    }

    @Test
    fun `adversarial stress - rapid weight increment spam does not overflow 999_9kg`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setWeight(980.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(980.0)

        // Spam +20 kg 5 times
        repeat(5) {
            viewModel.incrementWeight(20.0)
        }

        // Must be capped at 999.9 kg without crash or floating point inaccuracies
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(999.9)
    }

    @Test
    fun `adversarial stress - repeated decimal dot inputs do not create malformed float`() {
        var input = "0"
        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.")

        // Repeated dot presses
        input = KeypadSanitizer.appendDot(input)
        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.")

        input = KeypadSanitizer.appendDigit(input, '5')
        assertThat(input).isEqualTo("0.5")

        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.5")

        val parsed = KeypadSanitizer.parseWeight(input)
        assertThat(parsed).isEqualTo(0.5)
    }

    @Test
    fun `adversarial stress - extreme backspace spam recovers gracefully to 0`() {
        var input = "125.5"
        repeat(10) {
            input = KeypadSanitizer.backspace(input)
        }
        assertThat(input).isEqualTo("0")
        assertThat(KeypadSanitizer.parseWeight(input)).isEqualTo(0.0)
    }

    @Test
    fun `adversarial stress - reps stepper clamped strictly between 1 and 999`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // Attempt to set negative reps or 0
        viewModel.setReps(-5)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(1)

        viewModel.setReps(0)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(1)

        // Attempt to set 100,000 reps
        viewModel.setReps(100_000)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(999)
    }

    @Test
    fun `adversarial stress - RIR clamped strictly between 0 and 5`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setRir(-10)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(0)

        viewModel.setRir(100)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(5)

        // Verify semantic Russian description for edge values
        assertThat(getRirDescription(-1)).isEqualTo("0 — До отказа (0 в запасе)")
        assertThat(getRirDescription(6)).isEqualTo("5 — Разминка / Запас ≥ 5")
    }

    @Test
    fun `adversarial stress - rapid timer pause resume and subtract below zero`() = runTest(testDispatcher) {
        var finishedCount = 0
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishedCount++ })

        timerManager.startSetRest(customSeconds = 15)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(15)

        // Rapid pause/resume cycles
        timerManager.pauseTimer()
        timerManager.resumeTimer()
        timerManager.pauseTimer()
        timerManager.resumeTimer()

        testScope.advanceTimeBy(5001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(10)

        // Subtract 60 seconds when 10 seconds remaining
        timerManager.subtractSeconds(60)

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isFalse()
        assertThat(state.isFinished).isTrue()
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(finishedCount).isEqualTo(1)
    }

    @Test
    fun `click budget invariant - set logging from cold state with modifications in exactly 4 clicks`() = runTest(testDispatcher) {
        val session = WorkoutSession(id = 50, status = WorkoutStatus.DRAFT)
        activeSessionFlow.value = WorkoutSessionWithSets(session, emptyList())
        coEvery { workoutRepository.insertSet(any()) } returns 1L

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // Base auto-populated / initial values
        viewModel.setWeight(70.0)
        viewModel.setReps(10)
        viewModel.setRir(3)

        var userClicks = 0

        // User Click 1: Quick weight bump +5 kg (70 -> 75 kg)
        viewModel.incrementWeight(5.0)
        userClicks++
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(75.0)

        // User Click 2: Reps increment (10 -> 11)
        viewModel.setReps(11)
        userClicks++
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(11)

        // User Click 3: RIR 1-tap change (3 -> 1)
        viewModel.setRir(1)
        userClicks++
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(1)

        // User Click 4: Save Set
        viewModel.saveSet()
        userClicks++
        testScheduler.runCurrent()

        assertThat(userClicks).isAtMost(4)
        coVerify(exactly = 1) {
            workoutRepository.insertSet(
                match { set ->
                    set.weightKg == 75.0 &&
                    set.reps == 11 &&
                    set.rir == 1 &&
                    set.isCompleted
                }
            )
        }
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isTrue()
    }
}
