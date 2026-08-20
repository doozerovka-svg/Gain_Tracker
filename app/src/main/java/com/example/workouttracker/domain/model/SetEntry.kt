package com.example.workouttracker.domain.model

data class SetEntry(
    val id: Long = 0,
    val workoutSessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true
)
