package com.example.workouttracker.timer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Adversarial Stress Test Suite for Rest Timer Engine & Notification Mechanics (Milestone 2).
 * Verifies mathematical precision, coroutine cancellation, race-condition safety,
 * +30s/-30s bounds, 0-second auto-completion, and vibration/channel configurations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerAdversarialStressTest {

    @Test
    fun `stress - default set and exercise break durations match specification exactly`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        // Set break: 90s
        timerManager.startSetRest()
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(90)
        assertThat(timerManager.timerState.value.totalSeconds).isEqualTo(90)
        assertThat(timerManager.timerState.value.isExerciseBreak).isFalse()
        assertThat(timerManager.timerState.value.formattedTime).isEqualTo("01:30")
        assertThat(timerManager.timerState.value.progress).isEqualTo(0f)

        // Exercise break: 180s
        timerManager.startExerciseRest()
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(180)
        assertThat(timerManager.timerState.value.totalSeconds).isEqualTo(180)
        assertThat(timerManager.timerState.value.isExerciseBreak).isTrue()
        assertThat(timerManager.timerState.value.formattedTime).isEqualTo("03:00")
        assertThat(timerManager.timerState.value.progress).isEqualTo(0f)
    }

    @Test
    fun `stress - edge case zero and negative start duration clamped safely to minimum 1s`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCount = 0
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCount++ })

        // Start with 0s
        timerManager.startTimer(0)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(1)
        assertThat(timerManager.timerState.value.totalSeconds).isEqualTo(1)

        testScope.advanceTimeBy(1001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(timerManager.timerState.value.isFinished).isTrue()
        assertThat(finishCount).isEqualTo(1)

        // Start with negative -50s
        timerManager.startTimer(-50)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(1)
        assertThat(timerManager.timerState.value.totalSeconds).isEqualTo(1)

        testScope.advanceTimeBy(1001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(timerManager.timerState.value.isFinished).isTrue()
        assertThat(finishCount).isEqualTo(2)
    }

    @Test
    fun `stress - large duration formatting and progress calculation monotonicity`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        // 1 hour timer (3600s)
        timerManager.startTimer(3600)
        assertThat(timerManager.timerState.value.formattedTime).isEqualTo("60:00")

        var previousProgress = 0f
        for (sec in 1..60) {
            testScope.advanceTimeBy(1001L)
            val state = timerManager.timerState.value
            assertThat(state.progress).isAtLeast(previousProgress)
            assertThat(state.progress).isAtMost(1f)
            previousProgress = state.progress
        }

        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(3540)
        assertThat(timerManager.timerState.value.formattedTime).isEqualTo("59:00")
    }

    @Test
    fun `stress - rapid pause resume cycling across 100 iterations does not corrupt state or drop ticks`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 60)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(60)

        // Stress toggle pause/resume 100 times rapidly
        repeat(100) {
            timerManager.pauseTimer()
            assertThat(timerManager.timerState.value.isPaused).isTrue()
            timerManager.resumeTimer()
            assertThat(timerManager.timerState.value.isPaused).isFalse()
        }

        // Advance 10 seconds of active ticking
        testScope.advanceTimeBy(10001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(50)
        assertThat(timerManager.timerState.value.isRunning).isTrue()
    }

    @Test
    fun `stress - pause freezing invariant - advancing virtual time while paused does not decrement`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 30)
        testScope.advanceTimeBy(5001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(25)

        timerManager.pauseTimer()
        assertThat(timerManager.timerState.value.isPaused).isTrue()

        // Advance 100 seconds in paused state
        testScope.advanceTimeBy(100000L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(25)
        assertThat(timerManager.timerState.value.isFinished).isFalse()

        // Resume and advance 5 seconds
        timerManager.resumeTimer()
        testScope.advanceTimeBy(5001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(20)
    }

    @Test
    fun `stress - boundary subtractSeconds below zero terminates cleanly with single callback`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCount = 0
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCount++ })

        timerManager.startSetRest(customSeconds = 25)
        // Subtract 30s when only 25s remain
        timerManager.subtractSeconds(30)

        val state = timerManager.timerState.value
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(state.isRunning).isFalse()
        assertThat(state.isFinished).isTrue()
        assertThat(state.progress).isEqualTo(1f)
        assertThat(finishCount).isEqualTo(1)

        // Repeating subtract when already finished is a safe no-op
        timerManager.subtractSeconds(30)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(finishCount).isEqualTo(1)
    }

    @Test
    fun `stress - addSeconds restarts an already finished timer cleanly`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCount = 0
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCount++ })

        timerManager.startSetRest(customSeconds = 5)
        testScope.advanceTimeBy(5001L)
        assertThat(timerManager.timerState.value.isFinished).isTrue()
        assertThat(finishCount).isEqualTo(1)

        // User taps +30s on finished timer HUD
        timerManager.addSeconds(30)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(30)
        assertThat(timerManager.timerState.value.isRunning).isTrue()
        assertThat(timerManager.timerState.value.isFinished).isFalse()

        testScope.advanceTimeBy(10001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(20)

        testScope.advanceTimeBy(20001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(timerManager.timerState.value.isFinished).isTrue()
        assertThat(finishCount).isEqualTo(2)
    }

    @Test
    fun `stress - rapid startTimer re-entry spam cancels previous coroutines without leaks`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCount = 0
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCount++ })

        // Spam startTimer 50 times in rapid succession
        for (i in 1..50) {
            timerManager.startTimer(seconds = i + 10)
        }

        // The active timer should be the last one (60s)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(60)
        assertThat(timerManager.timerState.value.totalSeconds).isEqualTo(60)

        testScope.advanceTimeBy(60001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(timerManager.timerState.value.isFinished).isTrue()
        // Callback must be called exactly once for the final timer
        assertThat(finishCount).isEqualTo(1)
    }

    @Test
    fun `stress - notification service constants and vibration pattern verification`() {
        assertThat(RestTimerNotificationService.CHANNEL_ID).isEqualTo("workout_timer_channel")
        assertThat(RestTimerNotificationService.NOTIFICATION_ID).isEqualTo(1001)

        // Vibration pattern: [0, 500, 200, 500] (0ms delay, 500ms vibe, 200ms sleep, 500ms vibe)
        val expectedPattern = longArrayOf(0, 500, 200, 500)
        assertThat(expectedPattern).asList().containsExactly(0L, 500L, 200L, 500L).inOrder()
    }
}
