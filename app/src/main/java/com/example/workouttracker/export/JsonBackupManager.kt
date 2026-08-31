package com.example.workouttracker.export

import android.content.Context
import com.example.workouttracker.data.local.AppDatabase
import com.example.workouttracker.data.local.entity.CategoryEntity
import com.example.workouttracker.data.local.entity.ExerciseEntity
import com.example.workouttracker.data.local.entity.SetEntryEntity
import com.example.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonBackupManager(private val database: AppDatabase) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // Categories
        val categories = database.categoryDao().getAllCategoriesList()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Exercises
        val exercises = database.exerciseDao().getAllExercisesList()
        val exArray = JSONArray()
        for (e in exercises) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("name", e.name)
            obj.put("categoryId", e.categoryId)
            obj.put("defaultRestTimeSeconds", e.defaultRestTimeSeconds)
            obj.put("defaultExerciseRestTimeSeconds", e.defaultExerciseRestTimeSeconds)
            obj.put("isBodyweight", e.isBodyweight)
            exArray.put(obj)
        }
        root.put("exercises", exArray)

        // Sessions
        val sessions = database.workoutSessionDao().getAllSessionsList()
        val sessArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("date", s.date)
            obj.put("status", s.status)
            obj.put("notes", s.notes)
            sessArray.put(obj)
        }
        root.put("sessions", sessArray)

        // Sets
        val allSets = database.setEntryDao().getAllSetsList()
        val setArray = JSONArray()
        for (st in allSets) {
            val obj = JSONObject()
            obj.put("id", st.id)
            obj.put("workoutSessionId", st.workoutSessionId)
            obj.put("exerciseId", st.exerciseId)
            obj.put("setNumber", st.setNumber)
            obj.put("weightKg", st.weightKg)
            obj.put("reps", st.reps)
            obj.put("rir", st.rir)
            obj.put("setType", st.setType)
            obj.put("superSetId", st.superSetId ?: JSONObject.NULL)
            obj.put("timestamp", st.timestamp)
            obj.put("isCompleted", st.isCompleted)
            setArray.put(obj)
        }
        root.put("sets", setArray)

        // Body measurements
        val measurements = database.bodyMeasurementDao().getAllMeasurementsSync()
        val mArray = JSONArray()
        for (m in measurements) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("date", m.date)
            obj.put("weightKg", m.weightKg ?: JSONObject.NULL)
            obj.put("bodyFatPercentage", m.bodyFatPercentage ?: JSONObject.NULL)
            obj.put("chestCm", m.chestCm ?: JSONObject.NULL)
            obj.put("waistCm", m.waistCm ?: JSONObject.NULL)
            obj.put("bicepsCm", m.bicepsCm ?: JSONObject.NULL)
            obj.put("thighsCm", m.thighsCm ?: JSONObject.NULL)
            obj.put("calvesCm", m.calvesCm ?: JSONObject.NULL)
            obj.put("neckCm", m.neckCm ?: JSONObject.NULL)
            obj.put("notes", m.notes)
            mArray.put(obj)
        }
        root.put("bodyMeasurements", mArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonString)
            var count = 0

            // Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                val catList = mutableListOf<CategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    catList.add(CategoryEntity(id = obj.getLong("id"), name = obj.getString("name")))
                }
                database.categoryDao().insertCategories(catList)
                count += catList.size
            }

            // Exercises
            if (root.has("exercises")) {
                val exArray = root.getJSONArray("exercises")
                val exList = mutableListOf<ExerciseEntity>()
                for (i in 0 until exArray.length()) {
                    val obj = exArray.getJSONObject(i)
                    exList.add(
                        ExerciseEntity(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            categoryId = obj.getLong("categoryId"),
                            defaultRestTimeSeconds = obj.optInt("defaultRestTimeSeconds", 90),
                            defaultExerciseRestTimeSeconds = obj.optInt("defaultExerciseRestTimeSeconds", 180),
                            isBodyweight = obj.optBoolean("isBodyweight", false)
                        )
                    )
                }
                database.exerciseDao().insertExercises(exList)
                count += exList.size
            }

            // Sessions
            if (root.has("sessions")) {
                val sessArray = root.getJSONArray("sessions")
                for (i in 0 until sessArray.length()) {
                    val obj = sessArray.getJSONObject(i)
                    database.workoutSessionDao().insertSession(
                        WorkoutSessionEntity(
                            id = obj.getLong("id"),
                            date = obj.getLong("date"),
                            status = obj.getString("status"),
                            notes = obj.optString("notes", "")
                        )
                    )
                    count++
                }
            }

            // Sets
            if (root.has("sets")) {
                val setArray = root.getJSONArray("sets")
                val setList = mutableListOf<SetEntryEntity>()
                for (i in 0 until setArray.length()) {
                    val obj = setArray.getJSONObject(i)
                    setList.add(
                        SetEntryEntity(
                            id = obj.getLong("id"),
                            workoutSessionId = obj.getLong("workoutSessionId"),
                            exerciseId = obj.getLong("exerciseId"),
                            setNumber = obj.getInt("setNumber"),
                            weightKg = obj.getDouble("weightKg"),
                            reps = obj.getInt("reps"),
                            rir = obj.getInt("rir"),
                            setType = obj.optString("setType", "NORMAL"),
                            superSetId = if (obj.isNull("superSetId")) null else obj.optLong("superSetId"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isCompleted = obj.optBoolean("isCompleted", true)
                        )
                    )
                }
                database.setEntryDao().insertSets(setList)
                count += setList.size
            }

            // Body Measurements
            if (root.has("bodyMeasurements")) {
                val mArray = root.getJSONArray("bodyMeasurements")
                for (i in 0 until mArray.length()) {
                    val obj = mArray.getJSONObject(i)
                    database.bodyMeasurementDao().insertMeasurement(
                        com.example.workouttracker.data.local.entity.BodyMeasurementEntity(
                            id = obj.getLong("id"),
                            date = obj.getLong("date"),
                            weightKg = if (obj.isNull("weightKg")) null else obj.optDouble("weightKg"),
                            bodyFatPercentage = if (obj.isNull("bodyFatPercentage")) null else obj.optDouble("bodyFatPercentage"),
                            chestCm = if (obj.isNull("chestCm")) null else obj.optDouble("chestCm"),
                            waistCm = if (obj.isNull("waistCm")) null else obj.optDouble("waistCm"),
                            bicepsCm = if (obj.isNull("bicepsCm")) null else obj.optDouble("bicepsCm"),
                            thighsCm = if (obj.isNull("thighsCm")) null else obj.optDouble("thighsCm"),
                            calvesCm = if (obj.isNull("calvesCm")) null else obj.optDouble("calvesCm"),
                            neckCm = if (obj.isNull("neckCm")) null else obj.optDouble("neckCm"),
                            notes = obj.optString("notes", "")
                        )
                    )
                    count++
                }
            }

            count
        }
    }
}
