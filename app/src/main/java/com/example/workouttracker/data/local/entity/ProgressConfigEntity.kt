package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.ProgressConfig

@Entity(
    tableName = "progress_configs",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"], unique = true)]
)
data class ProgressConfigEntity(
    @PrimaryKey
    val exerciseId: Long,
    val minStepKg: Double = 2.5,
    val progressionPercentHeavy: Double = 0.05,
    val progressionPercentModerate: Double = 0.02,
    val targetReps: Int = 8,
    val targetSets: Int = 3,
    val deloadPercent: Double = 0.10
) {
    fun toDomain(): ProgressConfig = ProgressConfig(
        exerciseId = exerciseId,
        minStepKg = minStepKg,
        progressionPercentHeavy = progressionPercentHeavy,
        progressionPercentModerate = progressionPercentModerate,
        targetReps = targetReps,
        targetSets = targetSets,
        deloadPercent = deloadPercent
    )

    companion object {
        fun fromDomain(domain: ProgressConfig): ProgressConfigEntity = ProgressConfigEntity(
            exerciseId = domain.exerciseId,
            minStepKg = domain.minStepKg,
            progressionPercentHeavy = domain.progressionPercentHeavy,
            progressionPercentModerate = domain.progressionPercentModerate,
            targetReps = domain.targetReps,
            targetSets = domain.targetSets,
            deloadPercent = domain.deloadPercent
        )
    }
}
