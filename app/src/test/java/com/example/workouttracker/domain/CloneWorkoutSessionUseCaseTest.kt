package com.example.workouttracker.domain

import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.CloneWorkoutSessionUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CloneWorkoutSessionUseCaseTest {

    private val workoutRepository: WorkoutRepository = mockk()
    private lateinit var useCase: CloneWorkoutSessionUseCase

    @Before
    fun setUp() {
        useCase = CloneWorkoutSessionUseCase(workoutRepository)
    }

    @Test
    fun `cloning session delegates to repository and returns new session id`() = runTest {
        val sourceSessionId = 42L
        val targetDate = 1755600000000L
        val expectedNewSessionId = 101L

        coEvery { workoutRepository.cloneSession(sourceSessionId, targetDate) } returns expectedNewSessionId

        val actualNewSessionId = useCase.execute(sourceSessionId, targetDate)

        assertThat(actualNewSessionId).isEqualTo(expectedNewSessionId)
        coVerify(exactly = 1) { workoutRepository.cloneSession(sourceSessionId, targetDate) }
    }
}
