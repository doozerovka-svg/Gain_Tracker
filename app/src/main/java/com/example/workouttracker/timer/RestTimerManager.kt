package com.example.workouttracker.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * State representing the rest timer status and countdown progress.
 */
data class RestTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isExerciseBreak: Boolean = false,
    val isFinished: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            ((totalSeconds - remainingSeconds).coerceAtLeast(0).toFloat() / totalSeconds).coerceIn(0f, 1f)
        } else 0f

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
}

/**
 * Reactive Rest Timer Engine with StateFlow emissions.
 * Auto-starts on set completion (default 90s between sets, 180s between exercises).
 */
class RestTimerManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val onTimerFinished: (() -> Unit)? = null
) {
    companion object {
        const val DEFAULT_REST_SET_SECONDS = 90
        const val DEFAULT_REST_EXERCISE_SECONDS = 180
    }

    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Start rest timer between sets (default 90s or custom).
     */
    fun startSetRest(customSeconds: Int? = null) {
        val duration = (customSeconds ?: DEFAULT_REST_SET_SECONDS).coerceAtLeast(1)
        startTimer(duration, isExerciseBreak = false)
    }

    /**
     * Start rest timer between exercises (default 180s or custom).
     */
    fun startExerciseRest(customSeconds: Int? = null) {
        val duration = (customSeconds ?: DEFAULT_REST_EXERCISE_SECONDS).coerceAtLeast(1)
        startTimer(duration, isExerciseBreak = true)
    }

    /**
     * Start timer with specified seconds and break type.
     */
    fun startTimer(seconds: Int, isExerciseBreak: Boolean = false) {
        val total = seconds.coerceAtLeast(1)
        timerJob?.cancel()

        _timerState.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = total,
            totalSeconds = total,
            isExerciseBreak = isExerciseBreak,
            isFinished = false
        )

        launchTicker()
    }

    /**
     * Pause the active timer.
     */
    fun pauseTimer() {
        if (!_timerState.value.isRunning || _timerState.value.isPaused) return
        timerJob?.cancel()
        _timerState.update { it.copy(isPaused = true) }
    }

    /**
     * Resume a paused timer.
     */
    fun resumeTimer() {
        if (!_timerState.value.isRunning || !_timerState.value.isPaused) return
        _timerState.update { it.copy(isPaused = false) }
        launchTicker()
    }

    /**
     * Add time (e.g. +30 seconds) to active countdown.
     */
    fun addSeconds(seconds: Int = 30) {
        if (!_timerState.value.isRunning && !_timerState.value.isFinished) return
        val currentRemaining = _timerState.value.remainingSeconds
        val currentTotal = _timerState.value.totalSeconds
        val newRemaining = currentRemaining + seconds
        val newTotal = maxOf(currentTotal + seconds, newRemaining)

        _timerState.update {
            it.copy(
                remainingSeconds = newRemaining,
                totalSeconds = newTotal,
                isRunning = true,
                isFinished = false
            )
        }

        if (timerJob == null || timerJob?.isActive == false) {
            launchTicker()
        }
    }

    /**
     * Subtract time (e.g. -30 seconds) from active countdown.
     */
    fun subtractSeconds(seconds: Int = 30) {
        if (!_timerState.value.isRunning) return
        val currentRemaining = _timerState.value.remainingSeconds
        val newRemaining = currentRemaining - seconds

        if (newRemaining <= 0) {
            finishTimer()
        } else {
            _timerState.update { it.copy(remainingSeconds = newRemaining) }
        }
    }

    /**
     * Skip/Stop the timer immediately.
     */
    fun skipTimer() {
        timerJob?.cancel()
        _timerState.value = RestTimerState(
            isRunning = false,
            isPaused = false,
            remainingSeconds = 0,
            totalSeconds = 0,
            isExerciseBreak = false,
            isFinished = false
        )
    }

    /**
     * Reset the timer to clean initial state.
     */
    fun reset() {
        skipTimer()
    }

    private fun finishTimer() {
        timerJob?.cancel()
        _timerState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                remainingSeconds = 0,
                isFinished = true
            )
        }
        onTimerFinished?.invoke()
    }

    private fun launchTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timerState.value.isRunning && !_timerState.value.isPaused) {
                delay(1000L)
                val remaining = _timerState.value.remainingSeconds - 1
                if (remaining <= 0) {
                    finishTimer()
                    break
                } else {
                    _timerState.update { it.copy(remainingSeconds = remaining) }
                }
            }
        }
    }
}
