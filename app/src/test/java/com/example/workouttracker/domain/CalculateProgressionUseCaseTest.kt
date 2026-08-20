package com.example.workouttracker.domain

import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.usecase.CalculateProgressionUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class CalculateProgressionUseCaseTest {

    private lateinit var useCase: CalculateProgressionUseCase

    @Before
    fun setUp() {
        useCase = CalculateProgressionUseCase()
    }

    @Test
    fun `super-easy rep overshoot RIR 4 with 10 reps on 8 target gives aggressive bump`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        // 100 * 1.075 = 107.5 kg
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 10,
            actualRir = 4,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(107.5)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("легкая")
    }

    @Test
    fun `confident reserve RIR 3 with 9 reps gives standard 5 percent increase`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        // 100 * 1.05 = 105.0 kg
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 9,
            actualRir = 3,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(105.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("5%")
    }

    @Test
    fun `exact target reps with comfortable RIR 2 applies double progression reps plus one`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 2,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(100.0) // weight holds
        assertThat(result.recommendedReps).isEqualTo(9) // reps increase!
        assertThat(result.explanationRu).contains("Двойная прогрессия")
    }

    @Test
    fun `exact target reps with high effort RIR 1 gives plus one plate step bump`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 1,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(102.5) // +2.5 kg step
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("Шаг веса")
    }

    @Test
    fun `exact target reps at absolute failure RIR 0 holds weight for adaptation`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(100.0) // holds
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("адаптации")
    }

    @Test
    fun `mild plan undershoot 7 of 8 holds weight`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 7,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(100.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("не выполнен")
    }

    @Test
    fun `severe failure 4 of 8 at RIR 0 triggers deload minus 10 percent`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        // 100 * 0.90 = 90.0 kg
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 4,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(90.0)
        assertThat(result.deltaApplied).isEqualTo(-0.10)
        assertThat(result.explanationRu).contains("Deload")
    }

    @Test
    fun `bodyweight exercise with plan met recommends adding min step plate`() {
        val config = ProgressConfig(
            exerciseId = 4,
            minStepKg = 1.25,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 0.0,
            actualReps = 8,
            actualRir = 2,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(1.25)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("добавить отягощение")
    }

    @Test
    fun `bodyweight exercise with plan missed recommends continuing bodyweight`() {
        val config = ProgressConfig(
            exerciseId = 4,
            minStepKg = 1.25,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 0.0,
            actualReps = 5,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(0.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.explanationRu).contains("собственным весом")
    }

    @Test
    fun `roundToStep correctly quantizes various inventory steps`() {
        assertThat(useCase.roundToStep(52.6, 2.5)).isEqualTo(52.5)
        assertThat(useCase.roundToStep(53.8, 2.5)).isEqualTo(55.0)
        assertThat(useCase.roundToStep(51.2, 1.25)).isEqualTo(51.25)
        assertThat(useCase.roundToStep(50.6, 1.25)).isEqualTo(50.0)
        assertThat(useCase.roundToStep(50.7, 0.5)).isEqualTo(50.5)
        assertThat(useCase.roundToStep(50.8, 0.5)).isEqualTo(51.0)
        assertThat(useCase.roundToStep(53.256, 0.0)).isEqualTo(53.26)
    }
}
