package com.example.workouttracker.presentation.screens.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.example.workouttracker.domain.repository.ExerciseRepository
import com.example.workouttracker.domain.repository.WorkoutRepository
import com.example.workouttracker.export.ExcelExporter
import com.example.workouttracker.export.PdfReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExportUiState(
    val startDate: LocalDate = LocalDate.now().minusDays(30),
    val endDate: LocalDate = LocalDate.now(),
    val isExporting: Boolean = false,
    val exportResult: String? = null,
    val sessionsCount: Int = 0,
    val isLoading: Boolean = true
)

class ExportViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var allSessions: List<WorkoutSessionWithSets> = emptyList()
    private var exerciseMap: Map<Long, Exercise> = emptyMap()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllSessions(),
                exerciseRepository.getAllExercises()
            ) { sessions, exercises ->
                Pair(
                    sessions.filter { it.session.status == WorkoutStatus.COMPLETED },
                    exercises.associateBy { it.id }
                )
            }.collect { (sessions, exercises) ->
                allSessions = sessions
                exerciseMap = exercises
                updateSessionsCount()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
        updateSessionsCount()
    }

    fun setEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
        updateSessionsCount()
    }

    private fun updateSessionsCount() {
        val state = _uiState.value
        val startMillis = state.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = state.endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val count = getFilteredSessions(startMillis, endMillis).size
        _uiState.update { it.copy(sessionsCount = count) }
    }

    private fun getFilteredSessions(startMillis: Long, endMillis: Long): List<WorkoutSessionWithSets> {
        return allSessions.filter { sw ->
            sw.session.date in startMillis until endMillis
        }
    }

    fun exportExcel(context: Context) {
        export(context, "xlsx") { sessions, exercises, file ->
            FileOutputStream(file).use { fos ->
                ExcelExporter.exportToStream(sessions, exercises, fos)
            }
        }
    }

    fun exportPdf(context: Context) {
        export(context, "pdf") { sessions, exercises, file ->
            val state = _uiState.value
            val startMillis = state.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = state.endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            FileOutputStream(file).use { fos ->
                PdfReportExporter.generateReportToStream(context, sessions, exercises, startMillis, endMillis, fos)
            }
        }
    }

    private fun export(
        context: Context,
        extension: String,
        writer: suspend (List<WorkoutSessionWithSets>, Map<Long, Exercise>, File) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }
            try {
                val state = _uiState.value
                val startMillis = state.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = state.endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val sessions = getFilteredSessions(startMillis, endMillis)

                if (sessions.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, exportResult = "Нет тренировок за выбранный период") }
                    return@launch
                }

                val fileName = "тренировки_${state.startDate}_${state.endDate}.$extension"
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()
                val file = File(exportDir, fileName)

                withContext(Dispatchers.IO) {
                    writer(sessions, exerciseMap, file)
                }

                // Share via intent
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = when (extension) {
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться файлом").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                _uiState.update { it.copy(isExporting = false, exportResult = "Файл .$extension успешно создан") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportResult = "Ошибка экспорта: ${e.message}") }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }
}

class ExportViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            return ExportViewModel(workoutRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
