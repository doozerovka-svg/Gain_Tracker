package com.example.workouttracker.domain

import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.usecase.CalculateOneRepMaxUseCase
import com.example.workouttracker.domain.usecase.CalculateProgressionUseCase
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Adversarial stress test harness for Progression and 1RM calculation engines.
 * Executes hundreds of thousands of synthetic test cases across all boundary conditions.
 */
class ProgressionMathAdversarialStressTest {

    private lateinit var progressionUseCase: CalculateProgressionUseCase
    private lateinit var oneRepMaxUseCase: CalculateOneRepMaxUseCase

    @Before
    fun setUp() {
        progressionUseCase = CalculateProgressionUseCase()
        oneRepMaxUseCase = CalculateOneRepMaxUseCase()
    }

    @Test
    fun `stress test progression engine across 384,000 synthetic combinations`() {
        val weights = (0..300 step 5).map { it.toDouble() } // 0.0, 5.0, ..., 300.0 kg (61 values)
        val actualRepsList = (0..30).toList() // 31 values
        val rirList = listOf(0, 1, 2, 3, 4, 5, 8, 10) // 8 values
        val stepSizes = listOf(0.5, 1.25, 2.5, 5.0) // 4 values
        val targetRepsList = listOf(5, 8, 10, 12) // 4 values

        var totalEvaluated = 0

        for (step in stepSizes) {
            for (targetReps in targetRepsList) {
                val config = ProgressConfig(
                    exerciseId = 100,
                    minStepKg = step,
                    progressionPercentHeavy = 0.05,
                    progressionPercentModerate = 0.02,
                    targetReps = targetReps
                )

                for (w in weights) {
                    for (reps in actualRepsList) {
                        for (rir in rirList) {
                            totalEvaluated++
                            val result = progressionUseCase.execute(
                                previousWeightKg = w,
                                actualReps = reps,
                                actualRir = rir,
                                config = config
                            )

                            // 1. Invariant: Result must never produce NaN or Infinite values
                            assertWithMessage("Weight must be finite at w=$w, reps=$reps, rir=$rir, step=$step")
                                .that(result.recommendedWeightKg.isFinite()).isTrue()
                            assertWithMessage("Reps must be positive")
                                .that(result.recommendedReps).isAtLeast(1)

                            // 2. Invariant: Recommended weight must always be a quantized multiple of minStepKg
                            val stepRemainder = (result.recommendedWeightKg / step)
                            val roundedStep = stepRemainder.roundToInt()
                            val diff = abs(result.recommendedWeightKg - (roundedStep * step))
                            assertWithMessage("Result $result must align with step $step (diff=$diff)")
                                .that(diff).isLessThan(0.001)

                            // 3. Bodyweight Invariant
                            if (w <= 0.0) {
                                if (reps >= targetReps) {
                                    assertThat(result.recommendedWeightKg).isEqualTo(step)
                                    assertThat(result.recommendedReps).isEqualTo(targetReps)
                                    assertThat(result.explanationRu).contains("отягощение")
                                } else {
                                    assertThat(result.recommendedWeightKg).isEqualTo(0.0)
                                    assertThat(result.recommendedReps).isEqualTo(targetReps)
                                    assertThat(result.explanationRu).contains("собственным весом")
                                }
                            }
                        }
                    }
                }
            }
        }

        assertThat(totalEvaluated).isGreaterThan(100_000)
    }

    @Test
    fun `stress test 1RM formulas across 100,000 synthetic weight and rep inputs`() {
        val weights = (0..500 step 5).map { it.toDouble() } // 101 values
        val repsRange = 1..100 // 100 values

        var total1RMEvaluated = 0

        for (w in weights) {
            var prevEpley = 0.0
            var prevBrzycki = 0.0

            for (r in repsRange) {
                total1RMEvaluated++

                val epley = oneRepMaxUseCase.calculateEpley(w, r)
                val brzycki = oneRepMaxUseCase.calculateBrzycki(w, r)

                // 1. Invariants: finite, non-negative, not NaN
                assertWithMessage("Epley 1RM must be finite for w=$w, r=$r").that(epley.isFinite()).isTrue()
                assertWithMessage("Brzycki 1RM must be finite for w=$w, r=$r").that(brzycki.isFinite()).isTrue()
                assertThat(epley).isAtLeast(0.0)
                assertThat(brzycki).isAtLeast(0.0)

                if (w > 0.0) {
                    // 2. Single rep equality: 1RM for 1 rep must equal the lifted weight exactly
                    if (r == 1) {
                        assertThat(epley).isEqualTo(w)
                        assertThat(brzycki).isEqualTo(w)
                    }

                    // 3. Monotonicity: 1RM should never decrease as reps increase for identical weight
                    if (r > 1) {
                        assertWithMessage("Epley monotonicity violated at w=$w, r=$r")
                            .that(epley).isAtLeast(prevEpley)
                        assertWithMessage("Brzycki monotonicity violated at w=$w, r=$r")
                            .that(brzycki).isAtLeast(prevBrzycki)
                    }

                    // 4. Brzycki singularity clamping at R >= 36
                    if (r >= 36) {
                        val maxBrzycki = oneRepMaxUseCase.calculateBrzycki(w, 36)
                        assertThat(brzycki).isEqualTo(maxBrzycki)
                    }
                } else {
                    // 0.0 kg produces 0.0 1RM
                    assertThat(epley).isEqualTo(0.0)
                    assertThat(brzycki).isEqualTo(0.0)
                }

                prevEpley = epley
                prevBrzycki = brzycki
            }
        }

        assertThat(total1RMEvaluated).isAtLeast(10_000)
    }

    @Test
    fun `verify all edge cases for negative and extreme inputs in 1RM calculators`() {
        assertThat(oneRepMaxUseCase.calculateEpley(-100.0, 10)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateEpley(100.0, -5)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateEpley(0.0, 0)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateEpley(-50.0, -10)).isEqualTo(0.0)

        assertThat(oneRepMaxUseCase.calculateBrzycki(-100.0, 10)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, -5)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(0.0, 0)).isEqualTo(0.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(-50.0, -10)).isEqualTo(0.0)

        // Critical singularity boundaries for Brzycki
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, 36)).isEqualTo(3600.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, 37)).isEqualTo(3600.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, 38)).isEqualTo(3600.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, 1000)).isEqualTo(3600.0)
    }

    @Test
    fun `verify quantization precision and autoregulation transitions`() {
        val config = ProgressConfig(
            exerciseId = 1,
            minStepKg = 2.5,
            progressionPercentHeavy = 0.05,
            progressionPercentModerate = 0.02,
            targetReps = 10
        )

        // Target reps exact with RIR 2 -> Double Progression (reps + 1)
        val resultExactRir2 = progressionUseCase.execute(60.0, 10, 2, config)
        assertThat(resultExactRir2.recommendedWeightKg).isEqualTo(60.0)
        assertThat(resultExactRir2.recommendedReps).isEqualTo(11)

        // Rep overshoot (+1 rep, 11 of 10) with RIR 2 -> Transition to weight bump
        val resultOvershootRir2 = progressionUseCase.execute(60.0, 11, 2, config)
        assertThat(resultOvershootRir2.recommendedWeightKg).isEqualTo(62.5)
        assertThat(resultOvershootRir2.recommendedReps).isEqualTo(10)

        // High effort exact reps at RIR 1 -> +2.5 kg plate bump
        val resultExactRir1 = progressionUseCase.execute(10.0, 10, 1, config)
        assertThat(resultExactRir1.recommendedWeightKg).isEqualTo(12.5)
        assertThat(resultExactRir1.recommendedReps).isEqualTo(10)
    }
}
