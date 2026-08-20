package com.example.workouttracker.domain

import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.domain.usecase.GetAutoPopulatedValuesUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AutoPopulateUseCaseTest {

    private val workoutRepository: WorkoutRepository = mockk()
    private lateinit var useCase: GetAutoPopulatedValuesUseCase

    @Before
    fun setUp() {
        useCase = GetAutoPopulatedValuesUseCase(workoutRepository)
    }

    @Test
    fun `auto populate returns last completed set values when history exists`() = runTest {
        val exerciseId = 1L
        val beforeDate = 1755600000000L
        val lastSet = SetEntry(
            id = 5,
            workoutSessionId = 2,
            exerciseId = exerciseId,
            setNumber = 3,
            weightKg = 100.0,
            reps = 8,
            rir = 2,
            isCompleted = true
        )

        coEvery { workoutRepository.getLastCompletedSetForExercise(exerciseId, beforeDate) } returns lastSet

        val result = useCase.execute(exerciseId, beforeDate)

        assertThat(result).isNotNull()
        assertThat(result?.weightKg).isEqualTo(100.0)
        assertThat(result?.reps).isEqualTo(8)
        assertThat(result?.rir).isEqualTo(2)
    }

    @Test
    fun `auto populate returns null when no history exists without error`() = runTest {
        val exerciseId = 99L
        val beforeDate = 1755600000000L

        coEvery { workoutRepository.getLastCompletedSetForExercise(exerciseId, beforeDate) } returns null

        val result = useCase.execute(exerciseId, beforeDate)

        assertThat(result).isNull()
    }
}
