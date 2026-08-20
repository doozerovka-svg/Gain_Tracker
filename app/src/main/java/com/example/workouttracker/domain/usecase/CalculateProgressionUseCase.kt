package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.ProgressionResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Multi-factor deterministic progression engine without AI.
 * Implements Double Progression and RIR-based Autoregulation:
 * - Rep overshoot with high RIR (3-5+) -> Aggressive bump (+7.5% - +10%) or 2 plate steps
 * - Target reps met with moderate RIR (2-3) -> Double Progression (target reps + 1)
 * - Target reps met with RIR 1 -> Standard +1 inventory plate step
 * - Target reps met with RIR 0 -> Hold weight for consolidation
 * - Rep undershoot -> Hold weight
 * - Severe failure (<70% target reps at RIR 0) -> Deload (-10%)
 */
class CalculateProgressionUseCase {

    fun execute(
        previousWeightKg: Double,
        actualReps: Int,
        actualRir: Int,
        config: ProgressConfig
    ): ProgressionResult {
        val effectiveStep = if (config.minStepKg <= 0.0) 2.5 else config.minStepKg
        val targetReps = if (config.targetReps <= 0) 8 else config.targetReps
        val repDelta = actualReps - targetReps
        val clampedRir = actualRir.coerceIn(0, 5)

        // Edge Case: Bodyweight (0.0 kg or negative)
        if (previousWeightKg <= 0.0) {
            return if (actualReps >= targetReps) {
                ProgressionResult(
                    recommendedWeightKg = effectiveStep,
                    recommendedReps = targetReps,
                    deltaApplied = 0.0,
                    explanationRu = String.format(
                        Locale.US,
                        "План выполнен с собственным весом (%d повт.). Рекомендуется добавить отягощение %.2f кг.",
                        actualReps,
                        effectiveStep
                    )
                )
            } else {
                ProgressionResult(
                    recommendedWeightKg = 0.0,
                    recommendedReps = targetReps,
                    deltaApplied = 0.0,
                    explanationRu = String.format(
                        Locale.US,
                        "План повторений не выполнен (%d/%d). Продолжайте тренировки с собственным весом.",
                        actualReps,
                        targetReps
                    )
                )
            }
        }

        // Scenario 1: Severe failure (<70% of target reps and complete failure RIR 0) -> Deload
        if (actualReps < Math.ceil(targetReps * 0.70) && clampedRir == 0) {
            val deloadPercent = if (config.deloadPercent > 0.0) config.deloadPercent else 0.10
            val rawDeload = previousWeightKg * (1.0 - deloadPercent)
            val roundedDeload = Math.max(effectiveStep, roundToStep(rawDeload, effectiveStep))
            return ProgressionResult(
                recommendedWeightKg = roundedDeload,
                recommendedReps = targetReps,
                deltaApplied = -deloadPercent,
                explanationRu = String.format(
                    Locale.US,
                    "Значительный срыв плана (%d из %d повт. в отказ). Рекомендуется разгрузка (Deload -10%%): %.2f кг.",
                    actualReps,
                    targetReps,
                    roundedDeload
                )
            )
        }

        // Scenario 2: Rep undershoot (e.g. 6-7 reps out of 8) -> Hold weight
        if (repDelta < 0) {
            return ProgressionResult(
                recommendedWeightKg = previousWeightKg,
                recommendedReps = targetReps,
                deltaApplied = 0.0,
                explanationRu = String.format(
                    Locale.US,
                    "План повторений не выполнен (%d из %d). Вес удерживается: %.2f кг для закрепления.",
                    actualReps,
                    targetReps,
                    previousWeightKg
                )
            )
        }

        // Scenario 3: Super-easy / Underloaded (RIR >= 4 and rep overshoot >= +2) -> Aggressive +7.5% - +10%
        if (clampedRir >= 4 && repDelta >= 2) {
            val delta = 0.075
            val rawNext = previousWeightKg * (1.0 + delta)
            val minIncrease = effectiveStep * 2
            val calculated = Math.max(roundToStep(previousWeightKg + minIncrease, effectiveStep), roundToStep(rawNext, effectiveStep))
            return ProgressionResult(
                recommendedWeightKg = calculated,
                recommendedReps = targetReps,
                deltaApplied = delta,
                explanationRu = String.format(
                    Locale.US,
                    "Нагрузка слишком легкая (%d повт., RIR %d). Агрессивный прирост веса до %.2f кг.",
                    actualReps,
                    clampedRir,
                    calculated
                )
            )
        }

        // Scenario 4: Confident reserve (RIR 3..4 with reps in target or +1 rep) -> Standard +5% (min +1 step)
        if (clampedRir >= 3 && repDelta >= 0) {
            val delta = config.progressionPercentHeavy.coerceAtLeast(0.05)
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, effectiveStep)
            val finalWeight = if (roundedNext <= previousWeightKg) {
                roundToStep(previousWeightKg + effectiveStep, effectiveStep)
            } else {
                roundedNext
            }
            return ProgressionResult(
                recommendedWeightKg = finalWeight,
                recommendedReps = targetReps,
                deltaApplied = delta,
                explanationRu = String.format(
                    Locale.US,
                    "Отличный запас сил (%d повт., RIR %d). Шаг нагрузки (+5%%): %.2f кг.",
                    actualReps,
                    clampedRir,
                    finalWeight
                )
            )
        }

        // Scenario 5: Solid working set with rep overshoot (+1 rep at RIR 1..2) -> Transition to higher weight
        if (repDelta >= 1 && clampedRir in 1..2) {
            val delta = 0.035
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, effectiveStep)
            val finalWeight = if (roundedNext <= previousWeightKg) {
                roundToStep(previousWeightKg + effectiveStep, effectiveStep)
            } else {
                roundedNext
            }
            return ProgressionResult(
                recommendedWeightKg = finalWeight,
                recommendedReps = targetReps,
                deltaApplied = delta,
                explanationRu = String.format(
                    Locale.US,
                    "План перевыполнен (%d повт., RIR %d). Переход на новый вес: %.2f кг.",
                    actualReps,
                    clampedRir,
                    finalWeight
                )
            )
        }

        // Scenario 6: Target reps exact (repDelta == 0) with comfortable reserve (RIR 2..3) -> Double Progression (reps +1)
        if (repDelta == 0 && clampedRir in 2..3) {
            val nextReps = actualReps + 1
            return ProgressionResult(
                recommendedWeightKg = previousWeightKg,
                recommendedReps = nextReps,
                deltaApplied = 0.0,
                explanationRu = String.format(
                    Locale.US,
                    "Комфортный рабочий сет (ровно %d повт., RIR %d). Двойная прогрессия: цель %d повторений при весе %.2f кг.",
                    actualReps,
                    clampedRir,
                    nextReps,
                    previousWeightKg
                )
            )
        }

        // Scenario 7: Target reps exact at high effort (repDelta == 0, RIR 1) -> +1 plate step bump
        if (repDelta == 0 && clampedRir == 1) {
            val nextWeight = roundToStep(previousWeightKg + effectiveStep, effectiveStep)
            return ProgressionResult(
                recommendedWeightKg = nextWeight,
                recommendedReps = targetReps,
                deltaApplied = effectiveStep / previousWeightKg,
                explanationRu = String.format(
                    Locale.US,
                    "План выполнен на высоком усилии (RIR 1). Шаг веса (+%.2f кг): %.2f кг.",
                    effectiveStep,
                    nextWeight
                )
            )
        }

        // Scenario 8: Target reps exact at absolute failure (repDelta == 0, RIR 0) -> Hold weight for adaptation
        return ProgressionResult(
            recommendedWeightKg = previousWeightKg,
            recommendedReps = targetReps,
            deltaApplied = 0.0,
            explanationRu = String.format(
                Locale.US,
                "План выполнен на пределе отказа (RIR 0). Вес %.2f кг удерживается для адаптации связок и ЦНС.",
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
