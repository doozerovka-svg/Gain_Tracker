package com.example.workouttracker.domain

import com.example.workouttracker.domain.usecase.CalculateOneRepMaxUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class CalculateOneRepMaxUseCaseTest {

    private lateinit var useCase: CalculateOneRepMaxUseCase

    @Before
    fun setUp() {
        useCase = CalculateOneRepMaxUseCase()
    }

    @Test
    fun `epley single rep returns exact weight`() {
        val result = useCase.calculateEpley(100.0, 1)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `epley 10 reps calculates correctly`() {
        // 100 * (1 + 10/30) = 100 * 1.33333... = 133.33
        val result = useCase.calculateEpley(100.0, 10)
        assertThat(result).isEqualTo(133.33)
    }

    @Test
    fun `epley 8 reps at 80kg calculates correctly`() {
        // 80 * (1 + 8/30) = 80 * (1 + 0.26666...) = 80 * 1.26666... = 101.33
        val result = useCase.calculateEpley(80.0, 8)
        assertThat(result).isEqualTo(101.33)
    }

    @Test
    fun `epley handles zero weight or zero reps gracefully`() {
        assertThat(useCase.calculateEpley(0.0, 10)).isEqualTo(0.0)
        assertThat(useCase.calculateEpley(100.0, 0)).isEqualTo(0.0)
        assertThat(useCase.calculateEpley(-50.0, 5)).isEqualTo(0.0)
        assertThat(useCase.calculateEpley(100.0, -2)).isEqualTo(0.0)
    }

    @Test
    fun `brzycki single rep returns exact weight`() {
        val result = useCase.calculateBrzycki(100.0, 1)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `brzycki 10 reps calculates correctly`() {
        // 100 * (36 / (37 - 10)) = 100 * (36 / 27) = 100 * 1.33333... = 133.33
        val result = useCase.calculateBrzycki(100.0, 10)
        assertThat(result).isEqualTo(133.33)
    }

    @Test
    fun `brzycki boundary 36 reps calculates max finite value`() {
        // 100 * (36 / (37 - 36)) = 100 * 36 = 3600.0
        val result = useCase.calculateBrzycki(100.0, 36)
        assertThat(result).isEqualTo(3600.0)
    }

    @Test
    fun `brzycki 37 reps triggers guard preventing division by zero`() {
        // R >= 37 is clamped to 36 -> 3600.0
        val result = useCase.calculateBrzycki(100.0, 37)
        assertThat(result).isEqualTo(3600.0)
    }

    @Test
    fun `brzycki high reps 50 triggers guard`() {
        val result = useCase.calculateBrzycki(100.0, 50)
        assertThat(result).isEqualTo(3600.0)
    }

    @Test
    fun `brzycki handles zero weight or zero reps gracefully`() {
        assertThat(useCase.calculateBrzycki(0.0, 10)).isEqualTo(0.0)
        assertThat(useCase.calculateBrzycki(100.0, 0)).isEqualTo(0.0)
        assertThat(useCase.calculateBrzycki(-50.0, 5)).isEqualTo(0.0)
        assertThat(useCase.calculateBrzycki(100.0, -3)).isEqualTo(0.0)
    }
}
