package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutStatus

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val status: String = WorkoutStatus.DRAFT.name,
    val notes: String = ""
) {
    fun toDomain(): WorkoutSession = WorkoutSession(
        id = id,
        date = date,
        status = try { WorkoutStatus.valueOf(status) } catch (_: Exception) { WorkoutStatus.DRAFT },
        notes = notes
    )

    companion object {
        fun fromDomain(domain: WorkoutSession): WorkoutSessionEntity = WorkoutSessionEntity(
            id = domain.id,
            date = domain.date,
            status = domain.status.name,
            notes = domain.notes
        )
    }
}
