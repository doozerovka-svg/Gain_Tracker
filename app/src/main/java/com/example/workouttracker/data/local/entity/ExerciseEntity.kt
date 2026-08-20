package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.Exercise

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val defaultRestTimeSeconds: Int = 90,
    val defaultExerciseRestTimeSeconds: Int = 180,
    val isBodyweight: Boolean = false
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        categoryId = categoryId,
        defaultRestTimeSeconds = defaultRestTimeSeconds,
        defaultExerciseRestTimeSeconds = defaultExerciseRestTimeSeconds,
        isBodyweight = isBodyweight
    )

    companion object {
        fun fromDomain(domain: Exercise): ExerciseEntity = ExerciseEntity(
            id = domain.id,
            name = domain.name,
            categoryId = domain.categoryId,
            defaultRestTimeSeconds = domain.defaultRestTimeSeconds,
            defaultExerciseRestTimeSeconds = domain.defaultExerciseRestTimeSeconds,
            isBodyweight = domain.isBodyweight
        )
    }
}
