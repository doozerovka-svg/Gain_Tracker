package com.example.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workouttracker.data.local.dao.CategoryDao
import com.example.workouttracker.data.local.dao.ExerciseDao
import com.example.workouttracker.data.local.dao.ProgressConfigDao
import com.example.workouttracker.data.local.dao.SetEntryDao
import com.example.workouttracker.data.local.dao.WorkoutSessionDao
import com.example.workouttracker.data.local.entity.CategoryEntity
import com.example.workouttracker.data.local.entity.ExerciseEntity
import com.example.workouttracker.data.local.entity.ProgressConfigEntity
import com.example.workouttracker.data.local.entity.SetEntryEntity
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
        ProgressConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun progressConfigDao(): ProgressConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.categoryDao().insertCategories(PrepopulateData.categories)
                                database.exerciseDao().insertExercises(PrepopulateData.exercises)
                                database.progressConfigDao().insertProgressConfigs(PrepopulateData.defaultConfigs())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
