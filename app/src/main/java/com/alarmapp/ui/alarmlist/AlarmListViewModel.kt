package com.alarmapp.ui.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.SortType
import com.alarmapp.domain.repository.AlarmRepository
import com.alarmapp.domain.usecase.CalculateNextOccurrence
import com.alarmapp.domain.usecase.ScheduleAlarm
import com.alarmapp.domain.usecase.SortAlarms
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val sortType: SortType = SortType.DATETIME_DESC
)

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val sortAlarms: SortAlarms,
    private val scheduleAlarm: ScheduleAlarm,
    private val calculateNextOccurrence: CalculateNextOccurrence
) : ViewModel() {

    private val _sortType = MutableStateFlow(SortType.DATETIME_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _uiState = MutableStateFlow(AlarmListUiState())
    val uiState: StateFlow<AlarmListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                alarmRepository.getAllAlarms(),
                _sortType
            ) { alarms, sort ->
                sortAlarms(alarms, sort)
            }.collect { sorted ->
                _uiState.update { it.copy(alarms = sorted, sortType = _sortType.value) }
            }
        }
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newEnabled = !alarm.isEnabled
            alarmRepository.toggleAlarm(alarm.id, newEnabled)
            if (newEnabled) {
                val nextTime = calculateNextOccurrence(alarm)
                if (nextTime != null) {
                    scheduleAlarm.schedule(alarm, nextTime)
                }
            } else {
                scheduleAlarm.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduleAlarm.cancel(alarm)
            alarmRepository.deleteAlarm(alarm)
        }
    }
}
