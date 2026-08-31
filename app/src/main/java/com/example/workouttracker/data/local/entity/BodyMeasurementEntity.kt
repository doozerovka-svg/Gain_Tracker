package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.BodyMeasurement

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val bicepsCm: Double? = null,
    val thighsCm: Double? = null,
    val calvesCm: Double? = null,
    val neckCm: Double? = null,
    val notes: String = ""
) {
    fun toDomain(): BodyMeasurement = BodyMeasurement(
        id = id,
        date = date,
        weightKg = weightKg,
        bodyFatPercentage = bodyFatPercentage,
        chestCm = chestCm,
        waistCm = waistCm,
        bicepsCm = bicepsCm,
        thighsCm = thighsCm,
        calvesCm = calvesCm,
        neckCm = neckCm,
        notes = notes
    )

    companion object {
        fun fromDomain(domain: BodyMeasurement): BodyMeasurementEntity = BodyMeasurementEntity(
            id = domain.id,
            date = domain.date,
            weightKg = domain.weightKg,
            bodyFatPercentage = domain.bodyFatPercentage,
            chestCm = domain.chestCm,
            waistCm = domain.waistCm,
            bicepsCm = domain.bicepsCm,
            thighsCm = domain.thighsCm,
            calvesCm = domain.calvesCm,
            neckCm = domain.neckCm,
            notes = domain.notes
        )
    }
}
