package com.example.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.workouttracker.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name
    )

    companion object {
        fun fromDomain(domain: Category): CategoryEntity = CategoryEntity(
            id = domain.id,
            name = domain.name
        )
    }
}
