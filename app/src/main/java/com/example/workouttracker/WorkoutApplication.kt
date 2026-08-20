package com.example.workouttracker

import android.app.Application
import com.example.workouttracker.data.local.AppDatabase
import com.example.workouttracker.data.repository.ExerciseRepositoryImpl
import com.example.workouttracker.data.repository.WorkoutRepositoryImpl
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository

class WorkoutApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepositoryImpl(
            sessionDao = database.workoutSessionDao(),
            setDao = database.setEntryDao()
        )
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepositoryImpl(
            exerciseDao = database.exerciseDao(),
            categoryDao = database.categoryDao(),
            progressConfigDao = database.progressConfigDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
