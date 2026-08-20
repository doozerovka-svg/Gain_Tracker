package com.example.workouttracker.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<WorkoutSessionWithSets> = emptyList(),
    val exercises: Map<Long, Exercise> = emptyMap(),
    val expandedSessionId: Long? = null,
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Long? = null
)

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllSessions(),
                exerciseRepository.getAllExercises()
            ) { sessions, exercises ->
                val completedSessions = sessions
                    .filter { it.session.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.session.date }
                val exerciseMap = exercises.associateBy { it.id }
                Pair(completedSessions, exerciseMap)
            }.collect { (sessions, exerciseMap) ->
                _uiState.update {
                    it.copy(
                        sessions = sessions,
                        exercises = exerciseMap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleSessionExpand(sessionId: Long) {
        _uiState.update {
            it.copy(
                expandedSessionId = if (it.expandedSessionId == sessionId) null else sessionId
            )
        }
    }

    fun requestDeleteSession(sessionId: Long) {
        _uiState.update { it.copy(showDeleteConfirmation = sessionId) }
    }

    fun confirmDeleteSession() {
        val sessionId = _uiState.value.showDeleteConfirmation ?: return
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
            _uiState.update { it.copy(showDeleteConfirmation = null) }
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }
}

class HistoryViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(workoutRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
