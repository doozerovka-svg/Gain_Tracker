package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.SetEntry

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workoutSessionId"),
        Index("exerciseId")
    ]
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutSessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true
) {
    fun toDomain(): SetEntry = SetEntry(
        id = id,
        workoutSessionId = workoutSessionId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weightKg = weightKg,
        reps = reps,
        rir = rir,
        timestamp = timestamp,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(domain: SetEntry): SetEntryEntity = SetEntryEntity(
            id = domain.id,
            workoutSessionId = domain.workoutSessionId,
            exerciseId = domain.exerciseId,
            setNumber = domain.setNumber,
            weightKg = domain.weightKg,
            reps = domain.reps,
            rir = domain.rir,
            timestamp = domain.timestamp,
            isCompleted = domain.isCompleted
        )
    }
}
