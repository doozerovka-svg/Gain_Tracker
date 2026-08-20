package com.example.workouttracker.domain.model

data class ProgressConfig(
    val exerciseId: Long,
    val minStepKg: Double = 2.5,
    val progressionPercentHeavy: Double = 0.05,
    val progressionPercentModerate: Double = 0.02,
    val targetReps: Int = 8,
    val targetSets: Int = 3,
    val deloadPercent: Double = 0.10
)
