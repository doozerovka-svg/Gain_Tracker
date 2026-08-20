package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.ProgressionResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class CalculateProgressionUseCase {

    fun execute(
        previousWeightKg: Double,
        actualReps: Int,
        actualRir: Int,
        config: ProgressConfig
    ): ProgressionResult {
        val effectiveStep = if (config.minStepKg <= 0.0) 2.5 else config.minStepKg

        // Edge Case: Bodyweight (0.0 kg or negative)
        if (previousWeightKg <= 0.0) {
            return if (actualReps >= config.targetReps) {
                ProgressionResult(
                    recommendedWeightKg = effectiveStep,
                    recommendedReps = config.targetReps,
                    deltaApplied = 0.0,
                    explanationRu = String.format(
                        Locale.US,
                        "План выполнен с собственным весом. Рекомендуется добавить отягощение %.2f кг.",
                        effectiveStep
                    )
                )
            } else {
                ProgressionResult(
                    recommendedWeightKg = 0.0,
                    recommendedReps = config.targetReps,
                    deltaApplied = 0.0,
                    explanationRu = "План повторений не выполнен. Продолжайте тренировки с собственным весом."
                )
            }
        }

        // Branch 1: High Effort (RIR in 0..1) and Plan Completed (actualReps >= targetReps)
        if (actualReps >= config.targetReps && actualRir in 0..1) {
            val delta = config.progressionPercentHeavy
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, effectiveStep)
            val finalWeight = if (roundedNext <= previousWeightKg) {
                roundToStep(previousWeightKg + effectiveStep, effectiveStep)
            } else {
                roundedNext
            }
            val weightIncrease = roundToStep(finalWeight - previousWeightKg, effectiveStep)
            return ProgressionResult(
                recommendedWeightKg = finalWeight,
                recommendedReps = config.targetReps,
                deltaApplied = delta,
                explanationRu = String.format(
                    Locale.US,
                    "Отличная работа (RIR %d). Нагрузка увеличена на 5%% (+%.2f кг).",
                    actualRir,
                    weightIncrease
                )
            )
        }

        // Branch 2: Moderate Effort (RIR >= 2) and Plan Completed (actualReps >= targetReps)
        if (actualReps >= config.targetReps && actualRir >= 2) {
            val delta = config.progressionPercentModerate
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, effectiveStep)

            return if (roundedNext > previousWeightKg) {
                ProgressionResult(
                    recommendedWeightKg = roundedNext,
                    recommendedReps = config.targetReps,
                    deltaApplied = delta,
                    explanationRu = String.format(
                        Locale.US,
                        "План выполнен с запасом (RIR %d). Вес увеличен до %.2f кг (+2%%).",
                        actualRir,
                        roundedNext
                    )
                )
            } else {
                // Deadband detected: step swallowed the 2% increase -> recommend reps progression
                val nextReps = actualReps + 1
                ProgressionResult(
                    recommendedWeightKg = previousWeightKg,
                    recommendedReps = nextReps,
                    deltaApplied = delta,
                    explanationRu = String.format(
                        Locale.US,
                        "План выполнен с запасом (RIR %d). Рекомендуется увеличить повторения до %d.",
                        actualRir,
                        nextReps
                    )
                )
            }
        }

        // Branch 3: Plan Not Met (actualReps < targetReps) or 0 reps
        return ProgressionResult(
            recommendedWeightKg = previousWeightKg,
            recommendedReps = config.targetReps,
            deltaApplied = 0.0,
            explanationRu = String.format(
                Locale.US,
                "План повторений не выполнен (%d / %d). Вес удерживается: %.2f кг.",
                actualReps,
                config.targetReps,
                previousWeightKg
            )
        )
    }

    fun roundToStep(weight: Double, minStepKg: Double): Double {
        if (minStepKg <= 0.0) {
            return BigDecimal.valueOf(weight).setScale(2, RoundingMode.HALF_UP).toDouble()
        }
        val steps = Math.round(weight / minStepKg)
        val rounded = steps * minStepKg
        return BigDecimal.valueOf(rounded).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
