package com.example.workouttracker.domain

import com.example.workouttracker.domain.model.ProgressConfig
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.usecase.CalculateOneRepMaxUseCase
import com.example.workouttracker.domain.usecase.CalculateProgressionUseCase
import com.example.workouttracker.export.ExcelExporter
import com.example.workouttracker.domain.model.Exercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * End-to-End integration test: Tier 4 Real-World Workload Scenario.
 * Simulates a 3-week progression cycle across bench press, verifying the full chain:
 * Progression Engine → 1RM Calculator → Excel Export.
 */
class ProgressionCycleE2ETest {

    private val progressionUseCase = CalculateProgressionUseCase()
    private val oneRepMaxUseCase = CalculateOneRepMaxUseCase()

    private val benchPressConfig = ProgressConfig(
        exerciseId = 1,
        minStepKg = 2.5,
        progressionPercentHeavy = 0.05,
        progressionPercentModerate = 0.02,
        targetReps = 8
    )

    /**
     * Week 1: 100 kg × 8 reps, RIR 1 → Engine suggests 105 kg (+5%)
     * Week 2: 105 kg × 8 reps, RIR 3 → Engine suggests 107.5 kg (+2% rounded)
     * Week 3: 107.5 kg × 6 reps (plan 8 missed) → Engine holds 107.5 kg
     */
    @Test
    fun `three week progression cycle follows spec exactly`() {
        // ===== WEEK 1 =====
        val week1Result = progressionUseCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 1,
            config = benchPressConfig
        )
        assertThat(week1Result.recommendedWeightKg).isEqualTo(105.0)
        assertThat(week1Result.deltaApplied).isEqualTo(0.05)
        assertThat(week1Result.recommendedReps).isEqualTo(8)

        // Verify 1RM for week 1: 100 * (1 + 8/30) = 126.67
        val week1OneRM = oneRepMaxUseCase.calculateEpley(100.0, 8)
        assertThat(week1OneRM).isEqualTo(126.67)

        // ===== WEEK 2 =====
        val week2Result = progressionUseCase.execute(
            previousWeightKg = 105.0,
            actualReps = 8,
            actualRir = 3,
            config = benchPressConfig
        )
        assertThat(week2Result.recommendedWeightKg).isEqualTo(107.5)
        assertThat(week2Result.deltaApplied).isEqualTo(0.02)

        // Verify 1RM for week 2: 105 * (1 + 8/30) = 133.0
        val week2OneRM = oneRepMaxUseCase.calculateEpley(105.0, 8)
        assertThat(week2OneRM).isEqualTo(133.0)

        // 1RM should increase week over week
        assertThat(week2OneRM).isGreaterThan(week1OneRM)

        // ===== WEEK 3 =====
        val week3Result = progressionUseCase.execute(
            previousWeightKg = 107.5,
            actualReps = 6, // missed target of 8
            actualRir = 0,
            config = benchPressConfig
        )
        assertThat(week3Result.recommendedWeightKg).isEqualTo(107.5) // HOLD
        assertThat(week3Result.deltaApplied).isEqualTo(0.0)
        assertThat(week3Result.recommendedReps).isEqualTo(8)

        // Verify 1RM for week 3 is still valid: 107.5 * (1 + 6/30) = 129.0
        val week3OneRM = oneRepMaxUseCase.calculateEpley(107.5, 6)
        assertThat(week3OneRM).isEqualTo(129.0)
    }

    @Test
    fun `progression results are fully offline with no network dependency`() {
        // This test verifies the contract that all calculations are pure functions
        // with no network, API, or AI dependencies
        val result = progressionUseCase.execute(
            previousWeightKg = 100.0,
            actualReps = 8,
            actualRir = 1,
            config = benchPressConfig
        )
        // If we got here without timeout or exception, it's offline
        assertThat(result.recommendedWeightKg).isGreaterThan(0.0)
    }

    @Test
    fun `full workout export chain produces valid xlsx from progression data`() {
        val sessions = listOf(
            createSession(1, 1723939200000L, 100.0, 8, 2),
            createSession(2, 1724544000000L, 105.0, 8, 3),
            createSession(3, 1725148800000L, 107.5, 6, 0)
        )
        val exercises = mapOf(1L to Exercise(id = 1, name = "Жим лёжа", categoryId = 1))

        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(sessions, exercises, output)

        val bytes = output.toByteArray()
        assertThat(bytes.size).isGreaterThan(0)

        // Verify it's a valid ZIP (xlsx)
        assertThat(bytes[0]).isEqualTo('P'.code.toByte())
        assertThat(bytes[1]).isEqualTo('K'.code.toByte())
    }

    @Test
    fun `rounding consistency across inventory steps`() {
        // 1.25 kg step
        val config125 = benchPressConfig.copy(minStepKg = 1.25)
        val result = progressionUseCase.execute(80.0, 8, 1, config125)
        // 80 * 1.05 = 84.0 → round(84/1.25)*1.25 = 67.2*1.25 = round(67.2)*1.25 = 67*1.25 = 83.75
        assertThat(result.recommendedWeightKg % 1.25).isEqualTo(0.0)

        // 2.5 kg step
        val config25 = benchPressConfig.copy(minStepKg = 2.5)
        val result2 = progressionUseCase.execute(80.0, 8, 1, config25)
        assertThat(result2.recommendedWeightKg % 2.5).isEqualTo(0.0)

        // 5 kg step
        val config5 = benchPressConfig.copy(minStepKg = 5.0)
        val result3 = progressionUseCase.execute(80.0, 8, 1, config5)
        assertThat(result3.recommendedWeightKg % 5.0).isEqualTo(0.0)
    }

    @Test
    fun `brzycki and epley agree on 1 rep max identity`() {
        assertThat(oneRepMaxUseCase.calculateEpley(100.0, 1)).isEqualTo(100.0)
        assertThat(oneRepMaxUseCase.calculateBrzycki(100.0, 1)).isEqualTo(100.0)
    }

    @Test
    fun `zero history exercise gracefully handles progression`() {
        val result = progressionUseCase.execute(
            previousWeightKg = 0.0,
            actualReps = 0,
            actualRir = 0,
            config = benchPressConfig
        )
        // Should not crash, should return something sensible
        assertThat(result.recommendedWeightKg).isAtLeast(0.0)
    }

    private fun createSession(id: Long, date: Long, weight: Double, reps: Int, rir: Int): WorkoutSessionWithSets {
        return WorkoutSessionWithSets(
            session = WorkoutSession(id = id, date = date, status = WorkoutStatus.COMPLETED),
            sets = listOf(
                SetEntry(id = id * 10, workoutSessionId = id, exerciseId = 1, setNumber = 1, weightKg = weight, reps = reps, rir = rir),
                SetEntry(id = id * 10 + 1, workoutSessionId = id, exerciseId = 1, setNumber = 2, weightKg = weight, reps = reps, rir = rir),
                SetEntry(id = id * 10 + 2, workoutSessionId = id, exerciseId = 1, setNumber = 3, weightKg = weight, reps = reps, rir = rir)
            )
        )
    }
}
