package com.example.workouttracker.domain.usecase

import java.util.Locale

enum class StrengthRank(
    val titleRu: String,
    val minDots: Double,
    val iconEmoji: String,
    val descriptionRu: String
) {
    NOVICE("Новичок", 0.0, "🌱", "Начало силового пути"),
    AMATEUR("Любитель", 200.0, "⚡", "Базовый силовой фундамент"),
    ATHLETE("Атлет", 275.0, "🛡️", "Уверенный атлетический уровень"),
    SPECIALIST("Разрядник", 350.0, "⚔️", "Соревновательный уровень"),
    CANDIDATE_MASTER("КМС", 425.0, "👑", "Кандидат в мастера спорта"),
    ELITE("Элита", 500.0, "🔥", "Элитный силовой уровень");

    companion object {
        fun fromDots(dots: Double): StrengthRank {
            return entries.lastOrNull { dots >= it.minDots } ?: NOVICE
        }
    }
}

data class StrengthProfile(
    val bodyweightKg: Double,
    val benchPress1RM: Double,
    val squat1RM: Double,
    val deadlift1RM: Double,
    val ohp1RM: Double,
    val totalBig3Kg: Double,
    val dotsScore: Double,
    val wilksScore: Double,
    val rank: StrengthRank,
    val nextRank: StrengthRank?,
    val dotsToNextRank: Double
)

class CalculateStrengthRankUseCase {

    /**
     * Calculates DOTS coefficient:
     * DOTS = (500 / (a*x^4 + b*x^3 + c*x^2 + d*x + e)) * TotalWeight
     * Male polynomial constants
     */
    fun calculateDots(totalWeightKg: Double, bodyweightKg: Double): Double {
        if (bodyweightKg <= 30.0 || totalWeightKg <= 0.0) return 0.0
        val bw = bodyweightKg.coerceIn(40.0, 200.0)

        val a = -0.0000010930
        val b = 0.0007391293
        val c = -0.1918759221
        val d = 24.0900786
        val e = -307.75076

        val denominator = a * Math.pow(bw, 4.0) +
                b * Math.pow(bw, 3.0) +
                c * Math.pow(bw, 2.0) +
                d * bw +
                e

        if (denominator <= 0.0) return 0.0
        val dots = (500.0 / denominator) * totalWeightKg
        return Math.round(dots * 100.0) / 100.0
    }

    fun calculateWilks(totalWeightKg: Double, bodyweightKg: Double): Double {
        if (bodyweightKg <= 30.0 || totalWeightKg <= 0.0) return 0.0
        val bw = bodyweightKg.coerceIn(40.0, 200.0)

        val a = -216.0475144
        val b = 16.2606339
        val c = -0.002388645
        val d = -0.00113732
        val e = 7.01863E-06
        val f = -1.291E-08

        val denominator = a + b * bw + c * Math.pow(bw, 2.0) + d * Math.pow(bw, 3.0) + e * Math.pow(bw, 4.0) + f * Math.pow(bw, 5.0)
        if (denominator <= 0.0) return 0.0
        val wilks = (500.0 / denominator) * totalWeightKg
        return Math.round(wilks * 100.0) / 100.0
    }

    fun buildProfile(
        bodyweightKg: Double,
        benchPress1RM: Double,
        squat1RM: Double,
        deadlift1RM: Double,
        ohp1RM: Double
    ): StrengthProfile {
        val totalBig3 = benchPress1RM + squat1RM + deadlift1RM
        val totalForDots = if (totalBig3 > 0.0) totalBig3 else (benchPress1RM + ohp1RM) * 2.5
        val effectiveBw = if (bodyweightKg > 30.0) bodyweightKg else 75.0

        val dots = calculateDots(totalForDots, effectiveBw)
        val wilks = calculateWilks(totalForDots, effectiveBw)
        val rank = StrengthRank.fromDots(dots)

        val rankIndex = StrengthRank.entries.indexOf(rank)
        val nextRank = StrengthRank.entries.getOrNull(rankIndex + 1)
        val dotsToNext = nextRank?.let { Math.max(0.0, it.minDots - dots) } ?: 0.0

        return StrengthProfile(
            bodyweightKg = effectiveBw,
            benchPress1RM = benchPress1RM,
            squat1RM = squat1RM,
            deadlift1RM = deadlift1RM,
            ohp1RM = ohp1RM,
            totalBig3Kg = totalBig3,
            dotsScore = dots,
            wilksScore = wilks,
            rank = rank,
            nextRank = nextRank,
            dotsToNextRank = dotsToNext
        )
    }
}
