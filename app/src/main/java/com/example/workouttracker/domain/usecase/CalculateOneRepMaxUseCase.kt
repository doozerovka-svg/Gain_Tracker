package com.example.workouttracker.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode

class CalculateOneRepMaxUseCase {

    fun calculateEpley(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return round(weightKg)
        val result = weightKg * (1.0 + reps / 30.0)
        return round(result)
    }

    fun calculateBrzycki(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return round(weightKg)
        val effectiveReps = reps.coerceAtMost(36)
        val result = weightKg * (36.0 / (37.0 - effectiveReps))
        return round(result)
    }

    private fun round(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
