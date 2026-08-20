package com.example.workouttracker.domain.model

data class WorkoutSessionWithSets(
    val session: WorkoutSession,
    val sets: List<SetEntry> = emptyList()
)
