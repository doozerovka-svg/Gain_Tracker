package com.example.workouttracker.presentation.screens.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.data.local.dao.BodyMeasurementDao
import com.example.workouttracker.data.local.entity.BodyMeasurementEntity
import com.example.workouttracker.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BodyMeasurementsUiState(
    val measurements: List<BodyMeasurement> = emptyList(),
    val latestMeasurement: BodyMeasurement? = null,
    val isAddDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val userMessage: String? = null
)

class BodyMeasurementsViewModel(
    private val bodyMeasurementDao: BodyMeasurementDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementsUiState())
    val uiState: StateFlow<BodyMeasurementsUiState> = _uiState.asStateFlow()

    init {
        observeMeasurements()
    }

    private fun observeMeasurements() {
        viewModelScope.launch {
            bodyMeasurementDao.getAllMeasurements().collect { list ->
                val domainList = list.map { it.toDomain() }
                _uiState.update {
                    it.copy(
                        measurements = domainList,
                        latestMeasurement = domainList.firstOrNull(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun openAddDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isAddDialogOpen = isOpen) }
    }

    fun saveMeasurement(
        weightKg: Double?,
        bodyFatPercentage: Double?,
        chestCm: Double?,
        waistCm: Double?,
        bicepsCm: Double?,
        thighsCm: Double?,
        calvesCm: Double?,
        neckCm: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            val entity = BodyMeasurementEntity(
                date = System.currentTimeMillis(),
                weightKg = weightKg,
                bodyFatPercentage = bodyFatPercentage,
                chestCm = chestCm,
                waistCm = waistCm,
                bicepsCm = bicepsCm,
                thighsCm = thighsCm,
                calvesCm = calvesCm,
                neckCm = neckCm,
                notes = notes
            )
            bodyMeasurementDao.insertMeasurement(entity)
            _uiState.update {
                it.copy(
                    isAddDialogOpen = false,
                    userMessage = "Замер успешно сохранен"
                )
            }
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            bodyMeasurementDao.deleteMeasurement(id)
            _uiState.update { it.copy(userMessage = "Замер удален") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

class BodyMeasurementsViewModelFactory(
    private val bodyMeasurementDao: BodyMeasurementDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BodyMeasurementsViewModel(bodyMeasurementDao) as T
    }
}
