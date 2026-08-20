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
    fun `high effort RIR 1 and plan met increases weight by 5 percent rounded to step`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentHeavy = 0.05,
            targetReps = 8
        )
        // 100 * 1.05 = 105.0 -> rounded to step 2.5 = 105.0
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 1,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(105.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.05)
        assertThat(result.explanationRu).contains("5%")
    }

    @Test
    fun `high effort RIR 0 and reps exceeded increases weight by 5 percent`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentHeavy = 0.05,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 10,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(105.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.05)
    }

    @Test
    fun `high effort on light weight ensures at least one step increase`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentHeavy = 0.05,
            targetReps = 8
        )
        // 20 * 1.05 = 21.0 -> round(21/2.5)*2.5 = 20.0 <= 20.0 -> bumps to 22.5
        val result = useCase.execute(
            previousWeightKg = 20.0,
            actualReps = 8,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(22.5)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.05)
    }

    @Test
    fun `moderate effort RIR 3 and plan met increases weight by 2 percent when step allows`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentModerate = 0.02,
            targetReps = 8
        )
        // 100 * 1.02 = 102.0 -> round(102.0 / 2.5) * 2.5 = 41 * 2.5 = 102.5
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 3,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(102.5)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.02)
        assertThat(result.explanationRu).contains("2%")
    }

    @Test
    fun `moderate effort RIR 2 on heavy weight rounds to nearest inventory step`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentModerate = 0.02,
            targetReps = 8
        )
        // 150 * 1.02 = 153.0 -> round(153/2.5)*2.5 = round(61.2)*2.5 = 61 * 2.5 = 152.5
        val result = useCase.execute(
            previousWeightKg = 150.0,
            actualReps = 8,
            actualRir = 2,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(152.5)
        assertThat(result.recommendedReps).isEqualTo(8)
    }

    @Test
    fun `moderate effort deadband recommends plus one rep progression`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentModerate = 0.02,
            targetReps = 8
        )
        // 20 * 1.02 = 20.4 -> round(20.4/2.5)*2.5 = 20.0 (deadband!)
        val result = useCase.execute(
            previousWeightKg = 20.0,
            actualReps = 8,
            actualRir = 2,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(20.0)
        assertThat(result.recommendedReps).isEqualTo(9) // +1 rep
        assertThat(result.deltaApplied).isEqualTo(0.02)
        assertThat(result.explanationRu).contains("увеличить повторения")
    }

    @Test
    fun `moderate effort deadband with high reps recommends plus one rep`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentModerate = 0.02,
            targetReps = 10
        )
        // 10 * 1.02 = 10.2 -> round(10.2/2.5)*2.5 = 10.0
        val result = useCase.execute(
            previousWeightKg = 10.0,
            actualReps = 12,
            actualRir = 4,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(10.0)
        assertThat(result.recommendedReps).isEqualTo(13)
    }

    @Test
    fun `plan missed holds weight and resets recommended reps to target`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 100.0,
            actualReps = 6, // missed target of 8
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(100.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.0)
        assertThat(result.explanationRu).contains("не выполнен")
    }

    @Test
    fun `zero reps complete failure holds weight safely`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            targetReps = 8
        )
        val result = useCase.execute(
            previousWeightKg = 120.0,
            actualReps = 0,
            actualRir = 0,
            config = config
        )

        assertThat(result.recommendedWeightKg).isEqualTo(120.0)
        assertThat(result.recommendedReps).isEqualTo(8)
        assertThat(result.deltaApplied).isEqualTo(0.0)
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
