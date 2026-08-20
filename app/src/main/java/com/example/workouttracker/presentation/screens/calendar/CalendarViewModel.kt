package com.example.workouttracker.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.CloneWorkoutSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

enum class CalendarViewMode { MONTH, WEEK }

data class WorkoutDayInfo(
    val sessionId: Long,
    val status: WorkoutStatus,
    val setsCount: Int
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedDate: LocalDate? = LocalDate.now(),
    val workoutDays: Map<LocalDate, WorkoutDayInfo> = emptyMap(),
    val sessionsForSelectedDate: List<WorkoutSessionWithSets> = emptyList(),
    val isCloneDialogOpen: Boolean = false,
    val cloneSourceSessionId: Long? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class CalendarViewModel(
    private val workoutRepository: WorkoutRepository,
    private val cloneWorkoutSessionUseCase: CloneWorkoutSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutDays()
    }

    fun navigateMonth(delta: Int) {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(delta.toLong())) }
        loadWorkoutDays()
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == CalendarViewMode.MONTH)
                    CalendarViewMode.WEEK else CalendarViewMode.MONTH
            )
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSessionsForDate(date)
    }

    fun openCloneDialog(sessionId: Long) {
        _uiState.update { it.copy(isCloneDialogOpen = true, cloneSourceSessionId = sessionId) }
    }

    fun dismissCloneDialog() {
        _uiState.update { it.copy(isCloneDialogOpen = false, cloneSourceSessionId = null) }
    }

    fun cloneSessionToDate(sourceSessionId: Long, targetDate: LocalDate) {
        viewModelScope.launch {
            try {
                val targetMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                cloneWorkoutSessionUseCase.execute(sourceSessionId, targetMillis)
                _uiState.update {
                    it.copy(
                        isCloneDialogOpen = false,
                        cloneSourceSessionId = null,
                        userMessage = "Сессия скопирована на ${formatDate(targetDate)}"
                    )
                }
                loadWorkoutDays()
                loadSessionsForDate(targetDate)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Ошибка копирования: ${e.message}") }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun loadWorkoutDays() {
        viewModelScope.launch {
            val state = _uiState.value
            val (startDate, endDate) = getDateRange(state.currentMonth, state.viewMode, state.selectedDate)

            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            workoutRepository.getSessionsByDateRange(startMillis, endMillis).collect { sessions ->
                val dayMap = mutableMapOf<LocalDate, WorkoutDayInfo>()
                for (sessionWithSets in sessions) {
                    val sessionDate = Instant.ofEpochMilli(sessionWithSets.session.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    dayMap[sessionDate] = WorkoutDayInfo(
                        sessionId = sessionWithSets.session.id,
                        status = sessionWithSets.session.status,
                        setsCount = sessionWithSets.sets.size
                    )
                }
                _uiState.update { it.copy(workoutDays = dayMap, isLoading = false) }
            }
        }
    }

    private fun loadSessionsForDate(date: LocalDate) {
        viewModelScope.launch {
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            workoutRepository.getSessionsByDateRange(startMillis, endMillis).collect { sessions ->
                _uiState.update { it.copy(sessionsForSelectedDate = sessions) }
            }
        }
    }

    private fun getDateRange(
        month: YearMonth,
        mode: CalendarViewMode,
        selectedDate: LocalDate?
    ): Pair<LocalDate, LocalDate> {
        return when (mode) {
            CalendarViewMode.MONTH -> {
                val start = month.atDay(1)
                val end = month.atEndOfMonth()
                start to end
            }
            CalendarViewMode.WEEK -> {
                val anchor = selectedDate ?: LocalDate.now()
                val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                start to end
            }
        }
    }

    private fun formatDate(date: LocalDate): String {
        return "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}.${date.year}"
    }
}

class CalendarViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val cloneWorkoutSessionUseCase: CloneWorkoutSessionUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(workoutRepository, cloneWorkoutSessionUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
