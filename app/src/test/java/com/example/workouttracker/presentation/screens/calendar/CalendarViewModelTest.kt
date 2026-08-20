package com.example.workouttracker.presentation.screens.calendar

import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.CloneWorkoutSessionUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val workoutRepository: WorkoutRepository = mockk(relaxed = true)
    private val cloneUseCase: CloneWorkoutSessionUseCase = mockk()
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { workoutRepository.getSessionsByDateRange(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CalendarViewModel(workoutRepository, cloneUseCase)
    }

    @Test
    fun `initial state has current month and MONTH view mode`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.currentMonth).isEqualTo(YearMonth.now())
        assertThat(state.viewMode).isEqualTo(CalendarViewMode.MONTH)
        assertThat(state.selectedDate).isEqualTo(LocalDate.now())
    }

    @Test
    fun `navigateMonth forward changes month by plus one`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.currentMonth
        viewModel.navigateMonth(1)

        assertThat(viewModel.uiState.value.currentMonth).isEqualTo(initialMonth.plusMonths(1))
    }

    @Test
    fun `navigateMonth backward changes month by minus one`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.currentMonth
        viewModel.navigateMonth(-1)

        assertThat(viewModel.uiState.value.currentMonth).isEqualTo(initialMonth.minusMonths(1))
    }

    @Test
    fun `toggleViewMode switches between MONTH and WEEK`() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.viewMode).isEqualTo(CalendarViewMode.MONTH)

        viewModel.toggleViewMode()
        assertThat(viewModel.uiState.value.viewMode).isEqualTo(CalendarViewMode.WEEK)

        viewModel.toggleViewMode()
        assertThat(viewModel.uiState.value.viewMode).isEqualTo(CalendarViewMode.MONTH)
    }

    @Test
    fun `selectDate updates selectedDate`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val target = LocalDate.of(2026, 7, 15)
        viewModel.selectDate(target)

        assertThat(viewModel.uiState.value.selectedDate).isEqualTo(target)
    }

    @Test
    fun `workoutDays maps sessions correctly with status and set count`() = runTest {
        val sessionDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sessions = listOf(
            WorkoutSessionWithSets(
                session = WorkoutSession(id = 1, date = sessionDate, status = WorkoutStatus.COMPLETED),
                sets = listOf(
                    SetEntry(id = 1, workoutSessionId = 1, exerciseId = 1, setNumber = 1, weightKg = 80.0, reps = 8, rir = 2),
                    SetEntry(id = 2, workoutSessionId = 1, exerciseId = 1, setNumber = 2, weightKg = 80.0, reps = 7, rir = 1)
                )
            )
        )
        every { workoutRepository.getSessionsByDateRange(any(), any()) } returns flowOf(sessions)

        createViewModel()
        advanceUntilIdle()

        val dayInfo = viewModel.uiState.value.workoutDays[LocalDate.now()]
        assertThat(dayInfo).isNotNull()
        assertThat(dayInfo!!.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(dayInfo.setsCount).isEqualTo(2)
        assertThat(dayInfo.sessionId).isEqualTo(1L)
    }

    @Test
    fun `openCloneDialog and dismissCloneDialog toggle state correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.openCloneDialog(42L)
        assertThat(viewModel.uiState.value.isCloneDialogOpen).isTrue()
        assertThat(viewModel.uiState.value.cloneSourceSessionId).isEqualTo(42L)

        viewModel.dismissCloneDialog()
        assertThat(viewModel.uiState.value.isCloneDialogOpen).isFalse()
        assertThat(viewModel.uiState.value.cloneSourceSessionId).isNull()
    }

    @Test
    fun `cloneSessionToDate calls use case and shows success message`() = runTest {
        coEvery { cloneUseCase.execute(any(), any()) } returns 101L
        createViewModel()
        advanceUntilIdle()

        val targetDate = LocalDate.of(2026, 8, 25)
        viewModel.cloneSessionToDate(42L, targetDate)
        advanceUntilIdle()

        coVerify(exactly = 1) { cloneUseCase.execute(42L, any()) }
        assertThat(viewModel.uiState.value.isCloneDialogOpen).isFalse()
        assertThat(viewModel.uiState.value.userMessage).contains("скопирована")
    }

    @Test
    fun `clearUserMessage resets message to null`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.cloneSessionToDate(42L, LocalDate.now())
        advanceUntilIdle()

        viewModel.clearUserMessage()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }
}
