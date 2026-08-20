import type { ProgressConfig, ProgressionResult } from './types';

export class ProgressionEngine {
  static calculateProgression(
    previousWeightKg: number,
    actualReps: number,
    actualRir: number,
    config: ProgressConfig
  ): ProgressionResult {
    const minStep = config.minStepKg > 0 ? config.minStepKg : 2.5;
    const targetReps = config.targetReps > 0 ? config.targetReps : 8;

    // Plan missed
    if (actualReps < targetReps) {
      return {
        recommendedWeightKg: previousWeightKg,
        recommendedReps: targetReps,
        deltaApplied: 0.0,
        explanationRu: `План повторений (${targetReps}) не выполнен (сделано ${actualReps}). Удержание веса ${previousWeightKg} кг для закрепления нагрузки.`
      };
    }

    // High effort: RIR 0-1
    if (actualRir <= 1) {
      const delta = config.progressionPercentHeavy || 0.05;
      let calculated = previousWeightKg * (1.0 + delta);
      let rounded = this.roundToStep(calculated, minStep);

      // Light weight safeguard: ensure at least +1 step bump if rounded equal
      if (rounded <= previousWeightKg && previousWeightKg > 0) {
        rounded = this.roundToStep(previousWeightKg + minStep, minStep);
      } else if (previousWeightKg === 0) {
        rounded = minStep;
      }

      return {
        recommendedWeightKg: rounded,
        recommendedReps: targetReps,
        deltaApplied: delta,
        explanationRu: `Высокая интенсивность (RIR ${actualRir}, запас минимален). Применена максимальная прогрессия +${(delta * 100).toFixed(0)}%: целевой вес ${rounded} кг.`
      };
    }

    // Moderate/Easy effort: RIR >= 2
    const delta = config.progressionPercentModerate || 0.02;
    const calculated = previousWeightKg * (1.0 + delta);
    const rounded = this.roundToStep(calculated, minStep);

    if (previousWeightKg === 0) {
      return {
        recommendedWeightKg: minStep,
        recommendedReps: targetReps,
        deltaApplied: delta,
        explanationRu: `Упражнение с собственным весом выполнено уверенно. Рекомендуется добавить минимальное отягощение ${minStep} кг.`
      };
    }

    // Deadband check: if +2% does not reach minStep
    if (rounded <= previousWeightKg) {
      return {
        recommendedWeightKg: previousWeightKg,
        recommendedReps: actualReps + 1,
        deltaApplied: delta,
        explanationRu: `Умеренная нагрузка (RIR ${actualRir}). Прирост в пределах шага ${minStep} кг. Рекомендуется увеличить повторения до ${actualReps + 1} при весе ${previousWeightKg} кг.`
      };
    }

    return {
      recommendedWeightKg: rounded,
      recommendedReps: targetReps,
      deltaApplied: delta,
      explanationRu: `Умеренная нагрузка (RIR ${actualRir}). Применена консервативная прогрессия +${(delta * 100).toFixed(0)}%: целевой вес ${rounded} кг.`
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
