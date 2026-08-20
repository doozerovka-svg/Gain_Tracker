package com.example.workouttracker.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val defaultRestTimeSeconds: Int = 90,
    val defaultExerciseRestTimeSeconds: Int = 180,
    val isBodyweight: Boolean = false
)
