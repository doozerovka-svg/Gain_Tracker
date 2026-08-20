package com.example.workouttracker.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.workouttracker.domain.model.WorkoutStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val rusLocale = Locale("ru")
private val dayHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            MonthHeader(
                currentMonth = state.currentMonth,
                viewMode = state.viewMode,
                onPrev = { viewModel.navigateMonth(-1) },
                onNext = { viewModel.navigateMonth(1) },
                onToggleMode = { viewModel.toggleViewMode() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            DayOfWeekHeader()

            Spacer(modifier = Modifier.height(4.dp))

            when (state.viewMode) {
                CalendarViewMode.MONTH -> MonthGrid(
                    currentMonth = state.currentMonth,
                    workoutDays = state.workoutDays,
                    selectedDate = state.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
                CalendarViewMode.WEEK -> WeekRow(
                    selectedDate = state.selectedDate ?: LocalDate.now(),
                    workoutDays = state.workoutDays,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.sessionsForSelectedDate.isNotEmpty()) {
                Text(
                    text = "Тренировки за ${state.selectedDate?.let { "${it.dayOfMonth}.${it.monthValue.toString().padStart(2, '0')}" } ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.sessionsForSelectedDate, key = { it.session.id }) { sessionWithSets ->
                        val session = sessionWithSets.session
                        val setsCount = sessionWithSets.sets.size
                        val volume = sessionWithSets.sets.sumOf { it.weightKg * it.reps }
                        val statusText = if (session.status == WorkoutStatus.COMPLETED) "Завершена" else "Черновик"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (session.status == WorkoutStatus.COMPLETED)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Подходов: $setsCount • Объём: ${"%.1f".format(volume)} кг",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (session.notes.isNotBlank()) {
                                        Text(
                                            text = session.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.openCloneDialog(session.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Копировать тренировку"
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (state.selectedDate != null && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет тренировок за этот день",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (state.isCloneDialogOpen && state.cloneSourceSessionId != null) {
        CloneSessionDialog(
            onDismiss = { viewModel.dismissCloneDialog() },
            onConfirm = { targetDate ->
                viewModel.cloneSessionToDate(state.cloneSourceSessionId!!, targetDate)
            },
            defaultDate = state.selectedDate ?: LocalDate.now()
        )
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    viewMode: CalendarViewMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleMode: () -> Unit
) {
    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, rusLocale)
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$monthName ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (viewMode == CalendarViewMode.MONTH) "Месяц" else "Неделя",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            IconButton(onClick = onToggleMode, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Переключить вид")
            }
            IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
            }
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        dayHeaders.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonth: YearMonth,
    workoutDays: Map<LocalDate, WorkoutDayInfo>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = currentMonth.atDay(1)
    val firstDayOfWeek = firstDay.dayOfWeek.value // Monday=1
    val daysInMonth = currentMonth.lengthOfMonth()
    val today = LocalDate.now()

    val emptyCells = firstDayOfWeek - 1
    val totalCells = emptyCells + daysInMonth
    val cells = (0 until totalCells).map { index ->
        if (index < emptyCells) null
        else currentMonth.atDay(index - emptyCells + 1)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().height(((totalCells / 7 + 1) * 52).dp),
        userScrollEnabled = false
    ) {
        items(cells) { date ->
            if (date == null) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                DayCell(
                    date = date,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    workoutInfo = workoutDays[date],
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun WeekRow(
    selectedDate: LocalDate,
    workoutDays: Map<LocalDate, WorkoutDayInfo>,
    onDateSelected: (LocalDate) -> Unit
) {
    val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val today = LocalDate.now()
    val weekDays = (0L..6L).map { monday.plusDays(it) }

    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEach { date ->
            Box(modifier = Modifier.weight(1f)) {
                DayCell(
                    date = date,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    workoutInfo = workoutDays[date],
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    workoutInfo: WorkoutDayInfo?,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val indicatorColor = when (workoutInfo?.status) {
        WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        WorkoutStatus.DRAFT -> MaterialTheme.colorScheme.tertiary
        null -> null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }
    }
}

@Composable
private fun CloneSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    defaultDate: LocalDate
) {
    val targetDate = defaultDate.plusDays(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Копировать тренировку") },
        text = {
            Text("Скопировать все упражнения и подходы на ${targetDate.dayOfMonth}.${targetDate.monthValue.toString().padStart(2, '0')}.${targetDate.year}?")
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(targetDate) }) {
                Text("Копировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
