import type { ProgressConfig, ProgressionResult } from './types';

export class ProgressionEngine {
  static calculateProgression(
    previousWeightKg: number,
    actualReps: number,
    actualRir: number,
    config: ProgressConfig
  ): ProgressionResult {
    const effectiveStep = config.minStepKg > 0 ? config.minStepKg : 2.5;
    const targetReps = config.targetReps > 0 ? config.targetReps : 8;
    const repDelta = actualReps - targetReps;
    const clampedRir = Math.max(0, Math.min(5, actualRir));

    // Edge Case: Bodyweight (0 kg)
    if (previousWeightKg <= 0) {
      if (actualReps >= targetReps) {
        return {
          recommendedWeightKg: effectiveStep,
          recommendedReps: targetReps,
          deltaApplied: 0.0,
          explanationRu: `План выполнен с собственным весом (${actualReps} повт.). Рекомендуется добавить отягощение ${effectiveStep} кг.`,
        };
      } else {
        return {
          recommendedWeightKg: 0,
          recommendedReps: targetReps,
          deltaApplied: 0.0,
          explanationRu: `План повторений не выполнен (${actualReps}/${targetReps}). Продолжайте тренировки с собственным весом.`,
        };
      }
    }

    // Scenario 1: Severe failure (<70% target reps at RIR 0) -> Deload (-10%)
    if (actualReps < Math.ceil(targetReps * 0.7) && clampedRir === 0) {
      const deload = Math.max(effectiveStep, this.roundToStep(previousWeightKg * 0.9, effectiveStep));
      return {
        recommendedWeightKg: deload,
        recommendedReps: targetReps,
        deltaApplied: -0.1,
        explanationRu: `Значительный срыв плана (${actualReps} из ${targetReps} в отказ). Разгрузка (Deload -10%): ${deload} кг.`,
      };
    }

    // Scenario 2: Rep undershoot -> Hold weight
    if (repDelta < 0) {
      return {
        recommendedWeightKg: previousWeightKg,
        recommendedReps: targetReps,
        deltaApplied: 0.0,
        explanationRu: `План повторений не выполнен (${actualReps} из ${targetReps}). Вес удерживается: ${previousWeightKg} кг.`,
      };
    }

    // Scenario 3: Super-easy / Underloaded (RIR >= 4 and repDelta >= 2) -> Aggressive jump (+7.5% - +10%)
    if (clampedRir >= 4 && repDelta >= 2) {
      const delta = 0.075;
      const calculated = Math.max(
        this.roundToStep(previousWeightKg + effectiveStep * 2, effectiveStep),
        this.roundToStep(previousWeightKg * (1.0 + delta), effectiveStep)
      );
      return {
        recommendedWeightKg: calculated,
        recommendedReps: targetReps,
        deltaApplied: delta,
        explanationRu: `Слишком легкий вес (${actualReps} повт., RIR ${clampedRir}). Скачок нагрузки (+${(delta * 100).toFixed(1)}%): ${calculated} кг.`,
      };
    }

    // Scenario 4: Confident reserve (RIR >= 3 and repDelta >= 0) -> Standard +5% (min +1 step)
    if (clampedRir >= 3 && repDelta >= 0) {
      const delta = config.progressionPercentHeavy || 0.05;
      let calculated = this.roundToStep(previousWeightKg * (1.0 + delta), effectiveStep);
      if (calculated <= previousWeightKg) {
        calculated = this.roundToStep(previousWeightKg + effectiveStep, effectiveStep);
      }
      return {
        recommendedWeightKg: calculated,
        recommendedReps: targetReps,
        deltaApplied: delta,
        explanationRu: `Отличный запас сил (${actualReps} повт., RIR ${clampedRir}). Шаг нагрузки (+5%): ${calculated} кг.`,
      };
    }

    // Scenario 5: Solid working set with rep overshoot (+1 rep at RIR 1..2) -> Transition to higher weight
    if (repDelta >= 1 && clampedRir >= 1 && clampedRir <= 2) {
      const delta = 0.035;
      let calculated = this.roundToStep(previousWeightKg * (1.0 + delta), effectiveStep);
      if (calculated <= previousWeightKg) {
        calculated = this.roundToStep(previousWeightKg + effectiveStep, effectiveStep);
      }
      return {
        recommendedWeightKg: calculated,
        recommendedReps: targetReps,
        deltaApplied: delta,
        explanationRu: `План перевыполнен (${actualReps} повт., RIR ${clampedRir}). Новый вес: ${calculated} кг.`,
      };
    }

    // Scenario 6: Target reps exact with comfortable reserve (RIR 2..3) -> Double Progression (reps +1)
    if (repDelta === 0 && clampedRir >= 2 && clampedRir <= 3) {
      const nextReps = actualReps + 1;
      return {
        recommendedWeightKg: previousWeightKg,
        recommendedReps: nextReps,
        deltaApplied: 0.0,
        explanationRu: `Комфортный сет (ровно ${actualReps} повт., RIR ${clampedRir}). Двойная прогрессия: цель ${nextReps} повторений при весе ${previousWeightKg} кг.`,
      };
    }

    // Scenario 7: Target reps exact at high effort (RIR 1) -> +1 plate step bump
    if (repDelta === 0 && clampedRir === 1) {
      const nextWeight = this.roundToStep(previousWeightKg + effectiveStep, effectiveStep);
      return {
        recommendedWeightKg: nextWeight,
        recommendedReps: targetReps,
        deltaApplied: effectiveStep / previousWeightKg,
        explanationRu: `Высокое усилие (RIR 1, сделано ${actualReps} повт.). Шаг веса (+${effectiveStep} кг): ${nextWeight} кг.`,
      };
    }

    // Scenario 8: Target reps exact at absolute failure (RIR 0) -> Hold weight for adaptation
    return {
      recommendedWeightKg: previousWeightKg,
      recommendedReps: targetReps,
      deltaApplied: 0.0,
      explanationRu: `План выполнен на пределе (RIR 0). Вес ${previousWeightKg} кг удерживается для закрепления.`,
    };
  }

  static roundToStep(weight: number, step: number): number {
    if (step <= 0) return Math.round(weight * 100) / 100;
    return Math.round(weight / step) * step;
  }

  static calculateEpley(weightKg: number, reps: number): number {
    if (weightKg <= 0 || reps <= 0) return 0;
    if (reps === 1) return weightKg;
    return Math.round((weightKg * (1 + reps / 30)) * 100) / 100;
  }

  static calculateBrzycki(weightKg: number, reps: number): number {
    if (weightKg <= 0 || reps <= 0) return 0;
    if (reps === 1) return weightKg;
    const effectiveReps = Math.min(reps, 36);
    return Math.round((weightKg * (36 / (37 - effectiveReps))) * 100) / 100;
  }
}
