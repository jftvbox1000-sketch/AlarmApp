package com.alarmapp.ui.alarmeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.RecurrenceType
import com.alarmapp.domain.repository.AlarmRepository
import com.alarmapp.domain.usecase.CalculateNextOccurrence
import com.alarmapp.domain.usecase.ScheduleAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class AlarmEditorUiState(
    val description: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val specificDate: Long? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.ONE_TIME,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
    val isEnabled: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val calculateNextOccurrence: CalculateNextOccurrence,
    private val scheduleAlarm: ScheduleAlarm
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditorUiState())
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    private var existingId: Long? = null

    fun loadAlarm(alarmId: Long?) {
        if (alarmId == null) return
        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
            existingId = alarm.id
            _uiState.update {
                it.copy(
                    description = alarm.description,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    specificDate = alarm.specificDate,
                    recurrenceType = alarm.recurrenceType,
                    daysOfWeek = alarm.daysOfWeek,
                    dayOfMonth = alarm.dayOfMonth,
                    isEnabled = alarm.isEnabled,
                    isEditing = true
                )
            }
        }
    }

    fun updateDescription(value: String) { _uiState.update { it.copy(description = value) } }
    fun updateHour(value: Int) { _uiState.update { it.copy(hour = value.coerceIn(0, 23)) } }
    fun updateMinute(value: Int) { _uiState.update { it.copy(minute = value.coerceIn(0, 59)) } }
    fun updateDate(millis: Long?) { _uiState.update { it.copy(specificDate = millis) } }
    fun updateRecurrenceType(type: RecurrenceType) { _uiState.update { it.copy(recurrenceType = type) } }
    fun updateDaysOfWeek(days: Set<DayOfWeek>) { _uiState.update { it.copy(daysOfWeek = days) } }
    fun updateDayOfMonth(day: Int?) { _uiState.update { it.copy(dayOfMonth = day?.coerceIn(1, 31)) } }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val alarm = Alarm(
                id = existingId ?: 0,
                description = state.description,
                hour = state.hour,
                minute = state.minute,
                specificDate = state.specificDate,
                isEnabled = state.isEnabled,
                recurrenceType = state.recurrenceType,
                daysOfWeek = state.daysOfWeek,
                dayOfMonth = state.dayOfMonth
            )
            val id = if (existingId != null) {
                alarmRepository.updateAlarm(alarm)
                existingId!!
            } else {
                alarmRepository.insertAlarm(alarm)
            }
            val savedAlarm = alarm.copy(id = id)

            scheduleAlarm.cancel(savedAlarm)
            if (savedAlarm.isEnabled) {
                val nextTime = calculateNextOccurrence(savedAlarm)
                if (nextTime != null) {
                    scheduleAlarm.schedule(savedAlarm, nextTime)
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
