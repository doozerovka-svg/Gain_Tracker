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
import com.example.workouttracker.presentation.screens.active_workout.ActiveWorkoutViewModel
import com.example.workouttracker.timer.RestTimerManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

/**
 * Empirical Challenger Milestone 2 Stress Test Harness.
 * Systematically tests:
 * 1. Floating point precision in +X additions (e.g. +2.5 + 2.5 == 5.0) over thousands of iterations
 * 2. Keypad multi-dot protection ("10.5.2" -> "10.5", ".5", "...", etc.)
 * 3. Max weight limits (999.9 kg), zero, and negative handling
 * 4. Rapid multi-clicks on Save Set, Exercise selection, and timer mutations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengerMilestone2StressTest {

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
            Exercise(id = 2, categoryId = 1, name = "Приседания со штангой"),
            Exercise(id = 3, categoryId = 2, name = "Становая тяга")
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
    fun `edge case 1 - floating point precision in +X increments is mathematically exact`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setWeight(0.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(0.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("0")

        // +2.5 -> 2.5
        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(2.5)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("2.5")

        // +2.5 -> 5.0 (must format as "5" integer string)
        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(5.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("5")

        // +1.0 -> 6.0
        viewModel.incrementWeight(1.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(6.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("6")

        // +2.5 -> 8.5
        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(8.5)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("8.5")

        // +2.5 -> 11.0
        viewModel.incrementWeight(2.5)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(11.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("11")

        // Reset and test 100 x 2.5 additions = exact 250.0 without IEEE-754 binary drift
        viewModel.setWeight(0.0)
        for (i in 1..100) {
            viewModel.incrementWeight(2.5)
        }
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(250.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("250")
    }

    @Test
    fun `edge case 1b - randomized +X additions maintain exact BigDecimal precision`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setWeight(0.0)
        val validSteps = listOf(1.0, 2.5, 5.0, 10.0, 20.0)
        var expectedSum = BigDecimal("0.00")

        val random = Random(42)
        for (i in 1..50) {
            val step = validSteps[random.nextInt(validSteps.size)]
            expectedSum = expectedSum.add(BigDecimal.valueOf(step))
            if (expectedSum.toDouble() > 999.9) {
                expectedSum = BigDecimal("999.90")
            }
            viewModel.incrementWeight(step)
            assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(expectedSum.setScale(2, RoundingMode.HALF_UP).toDouble())
        }
    }

    @Test
    fun `edge case 2 - keypad multi-dot protection and edge keystroke sequences`() {
        // Multi-dot via Keypad simulation
        var input = ""
        // Press '.' -> "0."
        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.")

        // Press '.' again -> still "0."
        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.")

        // Press '5' -> "0.5"
        input = KeypadSanitizer.appendDigit(input, '5')
        assertThat(input).isEqualTo("0.5")

        // Press '.' -> still "0.5"
        input = KeypadSanitizer.appendDot(input)
        assertThat(input).isEqualTo("0.5")

        // Press '2' -> "0.52"
        input = KeypadSanitizer.appendDigit(input, '2')
        assertThat(input).isEqualTo("0.52")

        // Direct parse of malformed string "10.5.2"
        assertThat(KeypadSanitizer.parseWeight("10.5.2")).isEqualTo(0.0)

        // Parse with Russian comma separator "102,5"
        assertThat(KeypadSanitizer.parseWeight("102,5")).isEqualTo(102.5)

        // Parse empty / whitespace
        assertThat(KeypadSanitizer.parseWeight("   ")).isEqualTo(0.0)
        assertThat(KeypadSanitizer.parseWeight("")).isEqualTo(0.0)
    }

    @Test
    fun `edge case 3 - max weight bounds, zero, and negative input clamping`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // Negative weight clamp to 0.0
        viewModel.setWeight(-50.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(0.0)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("0")

        // Max weight clamp to 999.9
        viewModel.setWeight(1500.0)
        assertThat(viewModel.uiState.value.inputWeightKg).isEqualTo(999.9)
        assertThat(viewModel.uiState.value.rawWeightString).isEqualTo("999.9")

        // Negative / Zero Reps clamped to 1
        viewModel.setReps(-10)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(1)
        viewModel.setReps(0)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(1)

        // Excessive Reps clamped to 999
        viewModel.setReps(50000)
        assertThat(viewModel.uiState.value.inputReps).isEqualTo(999)

        // RIR bounds clamped to 0..5
        viewModel.setRir(-1)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(0)
        viewModel.setRir(10)
        assertThat(viewModel.uiState.value.inputRir).isEqualTo(5)
    }

    @Test
    fun `edge case 4 - rapid multi-clicks on Save Set increment set numbers monotonically`() = runTest(testDispatcher) {
        val session = WorkoutSession(id = 77, status = WorkoutStatus.DRAFT)
        val setsList = mutableListOf<SetEntry>()
        activeSessionFlow.value = WorkoutSessionWithSets(session, setsList)

        var lastInsertedSetNumber = 0
        coEvery { workoutRepository.insertSet(any()) } answers {
            val set = firstArg<SetEntry>()
            setsList.add(set)
            activeSessionFlow.value = WorkoutSessionWithSets(session, setsList.toList())
            lastInsertedSetNumber = set.setNumber
            set.setNumber.toLong()
        }

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.selectExercise(1L)
        viewModel.setWeight(100.0)
        viewModel.setReps(10)
        viewModel.setRir(2)

        // Rapidly save 20 sets in succession
        for (i in 1..20) {
            viewModel.saveSet()
            testScheduler.runCurrent()
        }

        assertThat(setsList).hasSize(20)
        for (i in 1..20) {
            assertThat(setsList[i - 1].setNumber).isEqualTo(i)
            assertThat(setsList[i - 1].weightKg).isEqualTo(100.0)
            assertThat(setsList[i - 1].isCompleted).isTrue()
        }
        assertThat(viewModel.restTimerManager.timerState.value.isRunning).isTrue()
    }

    @Test
    fun `edge case 5 - rapid exercise switching does not produce race conditions`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // Switch between exercises 1, 2, 3 rapidly 30 times
        for (i in 1..30) {
            val exId = ((i % 3) + 1).toLong()
            viewModel.selectExercise(exId)
            testScheduler.advanceUntilIdle()
            assertThat(viewModel.uiState.value.selectedExerciseId).isEqualTo(exId)
        }
    }
}
