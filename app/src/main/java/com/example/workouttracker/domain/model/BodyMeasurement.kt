package com.example.workouttracker.domain.model

data class BodyMeasurement(
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val bicepsCm: Double? = null,
    val thighsCm: Double? = null,
    val calvesCm: Double? = null,
    val neckCm: Double? = null,
    val notes: String = ""
)
