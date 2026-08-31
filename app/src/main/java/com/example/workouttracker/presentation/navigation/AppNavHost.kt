package com.example.workouttracker.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.WorkoutApplication
import com.example.workouttracker.domain.usecase.CloneWorkoutSessionUseCase
import com.example.workouttracker.presentation.screens.active_workout.ActiveWorkoutScreen
import com.example.workouttracker.presentation.screens.active_workout.ActiveWorkoutViewModel
import com.example.workouttracker.presentation.screens.active_workout.ActiveWorkoutViewModelFactory
import com.example.workouttracker.presentation.screens.analytics.AnalyticsScreen
import com.example.workouttracker.presentation.screens.analytics.AnalyticsViewModel
import com.example.workouttracker.presentation.screens.analytics.AnalyticsViewModelFactory
import com.example.workouttracker.presentation.screens.calendar.CalendarScreen
import com.example.workouttracker.presentation.screens.calendar.CalendarViewModel
import com.example.workouttracker.presentation.screens.calendar.CalendarViewModelFactory
import com.example.workouttracker.presentation.screens.export.ExportScreen
import com.example.workouttracker.presentation.screens.export.ExportViewModel
import com.example.workouttracker.presentation.screens.export.ExportViewModelFactory
import com.example.workouttracker.presentation.screens.history.HistoryScreen
import com.example.workouttracker.presentation.screens.history.HistoryViewModel
import com.example.workouttracker.presentation.screens.history.HistoryViewModelFactory

/**
 * Main Application NavHost with Bottom Navigation Bar and 100% Russian Localization.
 * All screens are now fully implemented (no placeholders).
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext as WorkoutApplication
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.titleRu)
                        },
                        label = {
                            Text(text = screen.titleRu)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ActiveWorkout.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ActiveWorkout.route) {
                val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
                    factory = ActiveWorkoutViewModelFactory(
                        workoutRepository = context.workoutRepository,
                        exerciseRepository = context.exerciseRepository
                    )
                )
                ActiveWorkoutScreen(viewModel = activeWorkoutViewModel)
            }

            composable(Screen.Calendar.route) {
                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModelFactory(
                        workoutRepository = context.workoutRepository,
                        cloneWorkoutSessionUseCase = CloneWorkoutSessionUseCase(context.workoutRepository)
                    )
                )
                CalendarScreen(viewModel = calendarViewModel)
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(
                        workoutRepository = context.workoutRepository,
                        exerciseRepository = context.exerciseRepository
                    )
                )
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModelFactory(
                        workoutRepository = context.workoutRepository,
                        exerciseRepository = context.exerciseRepository
                    )
                )
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Body.route) {
                val bodyViewModel: com.example.workouttracker.presentation.screens.body.BodyMeasurementsViewModel = viewModel(
                    factory = com.example.workouttracker.presentation.screens.body.BodyMeasurementsViewModelFactory(
                        bodyMeasurementDao = context.bodyMeasurementDao
                    )
                )
                com.example.workouttracker.presentation.screens.body.BodyMeasurementsScreen(viewModel = bodyViewModel)
            }

            composable(Screen.Export.route) {
                val exportViewModel: ExportViewModel = viewModel(
                    factory = ExportViewModelFactory(
                        workoutRepository = context.workoutRepository,
                        exerciseRepository = context.exerciseRepository
                    )
                )
                ExportScreen(viewModel = exportViewModel)
            }
        }
    }
}
