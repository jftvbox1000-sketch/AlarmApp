package com.alarmapp.ui.alarmeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarmapp.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(
    alarmId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AlarmEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.hour, initialMinute = state.minute, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(
                        state = timeState,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isPm = timeState.hour >= 12
                        FilterChip(
                            selected = !isPm,
                            onClick = {
                                if (timeState.hour >= 12) {
                                    timeState.hour = if (timeState.hour == 12) 0
                                        else timeState.hour - 12
                                }
                            },
                            label = { Text("AM") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isPm,
                            onClick = {
                                if (timeState.hour < 12) {
                                    timeState.hour = if (timeState.hour == 0) 12
                                        else timeState.hour + 12
                                }
                            },
                            label = { Text("PM") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateHour(timeState.hour)
                    viewModel.updateMinute(timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.specificDate
                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId != null) "Edit Alarm" else "New Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    val hour12 = when { state.hour == 0 -> 12; state.hour > 12 -> state.hour - 12; else -> state.hour }
                    val amPm = if (state.hour < 12) "AM" else "PM"
                    Text("${hour12}:${String.format("%02d", state.minute)} $amPm")
                }
                Spacer(Modifier.width(8.dp))
                Text("tap to change", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    val dateText = state.specificDate?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    } ?: "Set date (optional)"
                    Text(if (state.specificDate != null) dateText else dateText)
                }
                if (state.specificDate != null) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.updateDate(null) }) {
                        Text("Clear")
                    }
                }
            }

            if (state.specificDate == null) {
                Text("Recurrence", style = MaterialTheme.typography.titleSmall)
                RecurrenceTypeSelector(
                    selected = state.recurrenceType,
                    onSelect = { viewModel.updateRecurrenceType(it) }
                )

                when (state.recurrenceType) {
                    RecurrenceType.DAYS_OF_WEEK -> {
                        DayOfWeekSelector(
                            selected = state.daysOfWeek,
                            onToggle = { day ->
                                val updated = if (day in state.daysOfWeek)
                                    state.daysOfWeek - day else state.daysOfWeek + day
                                viewModel.updateDaysOfWeek(updated)
                            }
                        )
                    }
                    RecurrenceType.MONTHLY_NTH_DAY -> {
                        OutlinedTextField(
                            value = (state.dayOfMonth ?: 1).toString(),
                            onValueChange = { viewModel.updateDayOfMonth(it.toIntOrNull()) },
                            label = { Text("Day of month (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp)
                        )
                    }
                    else -> {}
                }
            } else {
                Text("This alarm will fire once on the selected date.", style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Skip holidays",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = state.skipHolidays,
                    onCheckedChange = { viewModel.updateSkipHolidays(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save(onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "Saving..." else "Save Alarm")
            }
        }
    }
}

@Composable
private fun RecurrenceTypeSelector(selected: RecurrenceType, onSelect: (RecurrenceType) -> Unit) {
    Column {
        listOf(
            RecurrenceType.ONE_TIME to "Once",
            RecurrenceType.DAYS_OF_WEEK to "Custom days",
            RecurrenceType.WEEKDAYS to "Weekdays (Mon-Fri)",
            RecurrenceType.MONTHLY_NTH_DAY to "Monthly"
        ).forEach { (type, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = selected == type, onClick = { onSelect(type) })
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfWeekSelector(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(day.name.take(3)) }
            )
        }
    }
}
