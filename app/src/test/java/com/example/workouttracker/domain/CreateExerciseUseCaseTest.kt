package com.example.workouttracker.domain

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.usecase.CreateExerciseUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CreateExerciseUseCaseTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var useCase: CreateExerciseUseCase

    @Before
    fun setUp() {
        exerciseRepository = mockk(relaxed = true)
        useCase = CreateExerciseUseCase(exerciseRepository)
    }

    @Test
    fun `creating exercise with blank name returns failure`() = runTest {
        val result = useCase.execute(
            name = "   ",
            categoryId = 1
        )
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `creating valid exercise saves exercise and updates progress config`() = runTest {
        coEvery { exerciseRepository.insertExercise(any()) } returns 42L

        val configSlot = slot<ProgressConfig>()
        coEvery { exerciseRepository.updateProgressConfig(capture(configSlot)) } returns Unit

        val result = useCase.execute(
            name = "Жим гантелей под углом",
            categoryId = 1,
            isBodyweight = false,
            defaultRestTimeSeconds = 90,
            minStepKg = 2.5,
            targetReps = 10
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42L)

        coVerify(exactly = 1) { exerciseRepository.insertExercise(any()) }
        coVerify(exactly = 1) { exerciseRepository.updateProgressConfig(any()) }

        val captured = configSlot.captured
        assertThat(captured.exerciseId).isEqualTo(42L)
        assertThat(captured.minStepKg).isEqualTo(2.5)
        assertThat(captured.targetReps).isEqualTo(10)
    }

    @Test
    fun `creating bodyweight exercise enforces 1_25kg minimum step`() = runTest {
        coEvery { exerciseRepository.insertExercise(any()) } returns 77L

        val configSlot = slot<ProgressConfig>()
        coEvery { exerciseRepository.updateProgressConfig(capture(configSlot)) } returns Unit

        val result = useCase.execute(
            name = "Подтягивания с весом",
            categoryId = 2,
            isBodyweight = true,
            defaultRestTimeSeconds = 120,
            minStepKg = 5.0,
            targetReps = 8
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(configSlot.captured.minStepKg).isEqualTo(1.25)
    }
}
