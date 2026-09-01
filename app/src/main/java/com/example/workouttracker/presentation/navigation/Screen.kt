package com.example.workouttracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
        icon = Icons.Default.History
    )

    data object History : Screen(
        route = "history",
        titleRu = "История",
        icon = Icons.Default.History
    )

    data object Analytics : Screen(
        route = "analytics",
        titleRu = "Прогресс",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    )

    data object Body : Screen(
        route = "body",
        titleRu = "Замеры",
        icon = Icons.Default.History
    )

    data object Export : Screen(
        route = "export",
        titleRu = "Ещё",
        icon = Icons.Default.MoreHoriz
    )

    companion object {
        val bottomNavItems = listOf(
            ActiveWorkout,
            History,
            Analytics,
            Export
        )
    }
}
