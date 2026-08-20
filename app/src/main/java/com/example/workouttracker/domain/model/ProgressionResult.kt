package com.example.workouttracker.domain.model

data class ProgressionResult(
    val recommendedWeightKg: Double,
    val recommendedReps: Int,
    val deltaApplied: Double,
    val explanationRu: String
)
