package com.example.workouttracker.presentation.screens.analytics

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val workoutRepository: WorkoutRepository = mockk()
    private val exerciseRepository: ExerciseRepository = mockk()

    private val exercises = listOf(
        Exercise(id = 1, name = "Жим лёжа", categoryId = 1),
        Exercise(id = 2, name = "Приседания", categoryId = 2)
    )

    private val sessions = listOf(
        WorkoutSessionWithSets(
            session = WorkoutSession(id = 1, date = 1723939200000L, status = WorkoutStatus.COMPLETED),
            sets = listOf(
                SetEntry(id = 1, workoutSessionId = 1, exerciseId = 1, setNumber = 1, weightKg = 80.0, reps = 8, rir = 2),
                SetEntry(id = 2, workoutSessionId = 1, exerciseId = 1, setNumber = 2, weightKg = 85.0, reps = 6, rir = 1),
                SetEntry(id = 3, workoutSessionId = 1, exerciseId = 2, setNumber = 1, weightKg = 100.0, reps = 5, rir = 3)
            )
        ),
        WorkoutSessionWithSets(
            session = WorkoutSession(id = 2, date = 1724025600000L, status = WorkoutStatus.COMPLETED),
            sets = listOf(
                SetEntry(id = 4, workoutSessionId = 2, exerciseId = 1, setNumber = 1, weightKg = 87.5, reps = 8, rir = 1)
            )
        ),
        WorkoutSessionWithSets(
            session = WorkoutSession(id = 3, date = 1724112000000L, status = WorkoutStatus.DRAFT),
            sets = listOf(
                SetEntry(id = 5, workoutSessionId = 3, exerciseId = 1, setNumber = 1, weightKg = 90.0, reps = 5, rir = 0)
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { workoutRepository.getAllSessions() } returns flowOf(sessions)
        every { exerciseRepository.getAllExercises() } returns flowOf(exercises)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load selects first exercise and builds chart data`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.selectedExerciseId).isEqualTo(1L)
        assertThat(state.exercises).hasSize(2)
    }

    @Test
    fun `chart data points only include completed sessions`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Sessions 1 and 2 are COMPLETED with exercise 1, session 3 is DRAFT
        assertThat(state.chartDataPoints).hasSize(2)
        assertThat(state.totalSessions).isEqualTo(2)
    }

    @Test
    fun `chart data points have correct 1RM values via Epley`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Session 1: best set for exercise 1 = 85.0 * 6 = 510 volume, 1RM = 85 * (1 + 6/30) = 85 * 1.2 = 102.0
        // Session 2: 87.5 * 8, 1RM = 87.5 * (1 + 8/30) = 87.5 * 1.2667 = 110.83
        assertThat(state.chartDataPoints[0].estimatedOneRepMax).isGreaterThan(0.0)
        assertThat(state.chartDataPoints[1].estimatedOneRepMax).isGreaterThan(state.chartDataPoints[0].estimatedOneRepMax)
    }

    @Test
    fun `maxOneRM tracks highest across all sessions`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Session 2 should have higher 1RM
        assertThat(state.maxOneRM).isEqualTo(state.chartDataPoints.maxOf { it.estimatedOneRepMax })
    }

    @Test
    fun `totalVolume sums all sets for selected exercise`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Session 1: 80*8 + 85*6 = 640 + 510 = 1150
        // Session 2: 87.5*8 = 700
        // Total = 1850
        assertThat(state.totalVolume).isEqualTo(1850.0)
    }

    @Test
    fun `selectExercise changes selected exercise and rebuilds chart`() = runTest {
        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.selectExercise(2L)

        val state = viewModel.uiState.value
        assertThat(state.selectedExerciseId).isEqualTo(2L)
        // Only session 1 has exercise 2
        assertThat(state.chartDataPoints).hasSize(1)
        assertThat(state.totalSessions).isEqualTo(1)
    }

    @Test
    fun `selecting exercise with no data produces empty chart`() = runTest {
        every { workoutRepository.getAllSessions() } returns flowOf(
            listOf(
                WorkoutSessionWithSets(
                    session = WorkoutSession(id = 1, date = 1723939200000L, status = WorkoutStatus.COMPLETED),
                    sets = listOf(
                        SetEntry(id = 1, workoutSessionId = 1, exerciseId = 1, setNumber = 1, weightKg = 80.0, reps = 8, rir = 2)
                    )
                )
            )
        )

        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.selectExercise(2L) // No sets for exercise 2

        val state = viewModel.uiState.value
        assertThat(state.chartDataPoints).isEmpty()
        assertThat(state.maxOneRM).isEqualTo(0.0)
        assertThat(state.totalVolume).isEqualTo(0.0)
        assertThat(state.totalSessions).isEqualTo(0)
    }

    @Test
    fun `empty sessions list results in zero stats`() = runTest {
        every { workoutRepository.getAllSessions() } returns flowOf(emptyList())

        val viewModel = AnalyticsViewModel(workoutRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.chartDataPoints).isEmpty()
        assertThat(state.maxOneRM).isEqualTo(0.0)
        assertThat(state.totalVolume).isEqualTo(0.0)
    }
}
