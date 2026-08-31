package com.example.workouttracker.domain.usecase

import com.example.workouttracker.domain.model.SetType
import com.example.workouttracker.domain.model.WorkoutSessionWithSets

data class DeloadAdvice(
    val isRecommended: Boolean,
    val reasonRu: String,
    val averageRir: Double,
    val consecutiveHardSessions: Int,
    val suggestedWeightReductionPercent: Double = 0.20
)

class CheckDeloadRecommendationUseCase {

    fun evaluate(recentCompletedSessions: List<WorkoutSessionWithSets>): DeloadAdvice {
        if (recentCompletedSessions.size < 3) {
            return DeloadAdvice(
                isRecommended = false,
                reasonRu = "Недостаточно данных (требуется от 3 завершенных тренировок)",
                averageRir = 2.0,
                consecutiveHardSessions = 0
            )
        }

        // Take last 3-5 sessions
        val window = recentCompletedSessions.take(5)
        var hardSessionsCount = 0
        var totalRirSum = 0.0
        var totalSetsCount = 0

        for (sessionWithSets in window) {
            val workingSets = sessionWithSets.sets.filter { it.setType != SetType.WARMUP && it.isCompleted }
            if (workingSets.isEmpty()) continue

            val sessionAvgRir = workingSets.map { it.rir }.average()
            val hasFailureSets = workingSets.any { it.setType == SetType.FAILURE || it.rir == 0 }

            totalRirSum += workingSets.sumOf { it.rir.toDouble() }
            totalSetsCount += workingSets.size

            if (sessionAvgRir < 1.3 || hasFailureSets) {
                hardSessionsCount++
            }
        }

        val overallAvgRir = if (totalSetsCount > 0) totalRirSum / totalSetsCount else 2.0

        if (hardSessionsCount >= 3 || overallAvgRir < 1.0) {
            return DeloadAdvice(
                isRecommended = true,
                reasonRu = "Высокое накопленное утомление ЦНС (средний RIR ${String.format("%.1f", overallAvgRir)} в последних $hardSessionsCount тренировках). Рекомендуется разгрузочная неделя (Deload -20% по весам) для суперкомпенсации.",
                averageRir = overallAvgRir,
                consecutiveHardSessions = hardSessionsCount,
                suggestedWeightReductionPercent = 0.20
            )
        }

        return DeloadAdvice(
            isRecommended = false,
            reasonRu = "Нагрузка в норме (средний RIR ${String.format("%.1f", overallAvgRir)}). Продолжайте тренировки по плану.",
            averageRir = overallAvgRir,
            consecutiveHardSessions = hardSessionsCount
        )
    }
}
