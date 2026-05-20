package com.alarmapp.ui.holiday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmapp.domain.model.PublicHoliday
import com.alarmapp.domain.repository.HolidayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HolidayUiState(
    val holidays: List<PublicHoliday> = emptyList(),
    val holidayCount: Int = 0
)

@HiltViewModel
class HolidayViewModel @Inject constructor(
    private val holidayRepository: HolidayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HolidayUiState())
    val uiState: StateFlow<HolidayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            holidayRepository.getAllHolidays().collect { holidays ->
                _uiState.update {
                    it.copy(holidays = holidays, holidayCount = holidays.size)
                }
            }
        }
    }

    fun addHoliday(dateMillis: Long) {
        viewModelScope.launch {
            val current = holidayRepository.count()
            if (current >= 20) return@launch
            holidayRepository.addHoliday(dateMillis)
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch {
            holidayRepository.deleteHoliday(id)
        }
    }
}
