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

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    @Test
    fun `initial state is idle with zero seconds`() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isFalse()
        assertThat(state.isPaused).isFalse()
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(state.totalSeconds).isEqualTo(0)
        assertThat(state.isFinished).isFalse()
        assertThat(state.progress).isEqualTo(0f)
        assertThat(state.formattedTime).isEqualTo("00:00")
    }

    @Test
    fun `startSetRest initializes 90 seconds set countdown`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest()

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isTrue()
        assertThat(state.isPaused).isFalse()
        assertThat(state.remainingSeconds).isEqualTo(90)
        assertThat(state.totalSeconds).isEqualTo(90)
        assertThat(state.isExerciseBreak).isFalse()
        assertThat(state.isFinished).isFalse()
        assertThat(state.formattedTime).isEqualTo("01:30")
    }

    @Test
    fun `startExerciseRest initializes 180 seconds exercise countdown`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startExerciseRest()

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isTrue()
        assertThat(state.isPaused).isFalse()
        assertThat(state.remainingSeconds).isEqualTo(180)
        assertThat(state.totalSeconds).isEqualTo(180)
        assertThat(state.isExerciseBreak).isTrue()
        assertThat(state.isFinished).isFalse()
        assertThat(state.formattedTime).isEqualTo("03:00")
    }

    @Test
    fun `custom duration overrides default rest time`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 60)

        val state = timerManager.timerState.value
        assertThat(state.remainingSeconds).isEqualTo(60)
        assertThat(state.totalSeconds).isEqualTo(60)
        assertThat(state.formattedTime).isEqualTo("01:00")
    }

    @Test
    fun `timer ticks down every second`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 10)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(10)

        testScope.advanceTimeBy(3001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(7)

        testScope.advanceTimeBy(4001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(3)
    }

    @Test
    fun `pause stops countdown and resume continues it`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 20)
        testScope.advanceTimeBy(5001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(15)

        timerManager.pauseTimer()
        assertThat(timerManager.timerState.value.isPaused).isTrue()

        // Time passes while paused
        testScope.advanceTimeBy(10000L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(15)

        timerManager.resumeTimer()
        assertThat(timerManager.timerState.value.isPaused).isFalse()

        testScope.advanceTimeBy(3001L)
        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(12)
    }

    @Test
    fun `addSeconds adds duration to active countdown`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 30)
        timerManager.addSeconds(30)

        val state = timerManager.timerState.value
        assertThat(state.remainingSeconds).isEqualTo(60)
        assertThat(state.totalSeconds).isEqualTo(60)
    }

    @Test
    fun `subtractSeconds decreases duration`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 60)
        timerManager.subtractSeconds(30)

        assertThat(timerManager.timerState.value.remainingSeconds).isEqualTo(30)
    }

    @Test
    fun `subtractSeconds below zero immediately finishes timer`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCallbackCalled = false
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCallbackCalled = true })

        timerManager.startSetRest(customSeconds = 20)
        timerManager.subtractSeconds(30)

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isFalse()
        assertThat(state.isFinished).isTrue()
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(finishCallbackCalled).isTrue()
    }

    @Test
    fun `timer completes and invokes callback at zero seconds`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var finishCallbackCalled = false
        val timerManager = RestTimerManager(scope = testScope, onTimerFinished = { finishCallbackCalled = true })

        timerManager.startSetRest(customSeconds = 5)
        testScope.advanceTimeBy(6000L)

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isFalse()
        assertThat(state.isFinished).isTrue()
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(finishCallbackCalled).isTrue()
    }

    @Test
    fun `skipTimer resets countdown immediately`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timerManager = RestTimerManager(scope = testScope)

        timerManager.startSetRest(customSeconds = 90)
        timerManager.skipTimer()

        val state = timerManager.timerState.value
        assertThat(state.isRunning).isFalse()
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(state.totalSeconds).isEqualTo(0)
        assertThat(state.isFinished).isFalse()
    }
}
