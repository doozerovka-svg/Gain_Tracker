package com.example.workouttracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Screen destinations for application navigation.
 */
sealed class Screen(
    val route: String,
    val titleRu: String,
    val icon: ImageVector
) {
    data object ActiveWorkout : Screen(
        route = "active_workout",
        titleRu = "Тренировка",
        icon = Icons.Default.FitnessCenter
    )

    data object Calendar : Screen(
        route = "calendar",
        titleRu = "Календарь",
        icon = Icons.Default.CalendarMonth
    )

    data object History : Screen(
        route = "history",
        titleRu = "История",
        icon = Icons.Default.History
    )

    data object Analytics : Screen(
        route = "analytics",
        titleRu = "Аналитика",
        icon = Icons.Default.TrendingUp
    )

    data object Body : Screen(
        route = "body",
        titleRu = "Замеры",
        icon = Icons.AutoMirrored.Filled.List
    )

    data object Export : Screen(
        route = "export",
        titleRu = "Экспорт",
        icon = Icons.Default.Share
    )

    companion object {
        val bottomNavItems = listOf(
            ActiveWorkout,
            Calendar,
            History,
            Analytics,
            Body,
            Export
        )
    }
}
