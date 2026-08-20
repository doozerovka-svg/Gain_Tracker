package com.example.workouttracker.domain.model

data class WorkoutSession(
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val status: WorkoutStatus = WorkoutStatus.DRAFT,
    val notes: String = ""
)
