package com.example.workouttracker.data.local

import com.example.workouttracker.data.local.entity.CategoryEntity
import com.example.workouttracker.data.local.entity.ExerciseEntity
import com.example.workouttracker.data.local.entity.ProgressConfigEntity

object PrepopulateData {

    val categories = listOf(
        CategoryEntity(id = 1, name = "Грудные"),
        CategoryEntity(id = 2, name = "Спина"),
        CategoryEntity(id = 3, name = "Ноги"),
        CategoryEntity(id = 4, name = "Плечи"),
        CategoryEntity(id = 5, name = "Руки"),
        CategoryEntity(id = 6, name = "Пресс и кор")
    )

    val exercises = listOf(
        // Грудные
        ExerciseEntity(id = 1, name = "Жим штанги лежа", categoryId = 1, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 2, name = "Жим гантелей", categoryId = 1, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 3, name = "Брусья", categoryId = 1, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = true),

        // Спина
        ExerciseEntity(id = 4, name = "Подтягивания", categoryId = 2, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = true),
        ExerciseEntity(id = 5, name = "Тяга штанги в наклоне", categoryId = 2, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 6, name = "Тяга верхнего блока", categoryId = 2, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 7, name = "Становая тяга", categoryId = 2, defaultRestTimeSeconds = 120, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),

        // Ноги
        ExerciseEntity(id = 8, name = "Приседания со штангой", categoryId = 3, defaultRestTimeSeconds = 120, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 9, name = "Румынская тяга", categoryId = 3, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 10, name = "Жим ногами", categoryId = 3, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 11, name = "Выпады с гантелями", categoryId = 3, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),

        // Плечи
        ExerciseEntity(id = 12, name = "Армейский жим", categoryId = 4, defaultRestTimeSeconds = 90, defaultExerciseRestTimeSeconds = 180, isBodyweight = false),
        ExerciseEntity(id = 13, name = "Махи гантелями в стороны", categoryId = 4, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 120, isBodyweight = false),

        // Руки
        ExerciseEntity(id = 14, name = "Сгибания на бицепс", categoryId = 5, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 120, isBodyweight = false),
        ExerciseEntity(id = 15, name = "Французский жим", categoryId = 5, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 120, isBodyweight = false),
        ExerciseEntity(id = 16, name = "Молотковые сгибания", categoryId = 5, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 120, isBodyweight = false),

        // Пресс и кор
        ExerciseEntity(id = 17, name = "Планка", categoryId = 6, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 90, isBodyweight = true),
        ExerciseEntity(id = 18, name = "Скручивания", categoryId = 6, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 90, isBodyweight = true),
        ExerciseEntity(id = 19, name = "Подъем ног в висе", categoryId = 6, defaultRestTimeSeconds = 60, defaultExerciseRestTimeSeconds = 90, isBodyweight = true)
    )

    fun defaultConfigs(): List<ProgressConfigEntity> {
        return exercises.map { exercise ->
            ProgressConfigEntity(
                exerciseId = exercise.id,
                minStepKg = if (exercise.isBodyweight) 1.25 else 2.5,
                progressionPercentHeavy = 0.05,
                progressionPercentModerate = 0.02,
                targetReps = 8,
                targetSets = 3,
                deloadPercent = 0.10
            )
        }
    }
}
