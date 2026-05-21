package com.alarmapp.ui.alarmlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.SortType
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onManageHolidays: () -> Unit,
    onDebug: () -> Unit,
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                actions = {
                    IconButton(onClick = onManageHolidays) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Holidays")
                    }
                    IconButton(onClick = onDebug) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug")
                    }
                    SortButton(sortType) { viewModel.setSortType(it) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Default.Add, contentDescription = "Add alarm")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { viewModel.toggleAlarm(alarm) },
                    onClick = { onEditAlarm(alarm.id) },
                    onDelete = { viewModel.deleteAlarm(alarm) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
    val dateStr = alarm.specificDate?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
    val recurrenceStr = when {
        alarm.specificDate != null -> null
        alarm.recurrenceType.name == "ONE_TIME" -> "Once"
        alarm.recurrenceType.name == "WEEKDAYS" -> "Weekdays"
        alarm.recurrenceType.name == "DAYS_OF_WEEK" && alarm.daysOfWeek.isNotEmpty() ->
            alarm.daysOfWeek.joinToString(", ") { it.name.take(3) }
        alarm.recurrenceType.name == "MONTHLY_NTH_DAY" -> "Day ${alarm.dayOfMonth ?: 1} monthly"
        else -> "Every day"
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onDelete),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.description.ifBlank { "(no description)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(timeStr)
                        if (dateStr != null) append(" · $dateStr")
                        if (recurrenceStr != null) append(" · $recurrenceStr")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SortButton(currentSort: SortType, onSortChange: (SortType) -> Unit) {
    val nextSort = when (currentSort) {
        SortType.DESCRIPTION_ASC -> SortType.DESCRIPTION_DESC
        SortType.DESCRIPTION_DESC -> SortType.DATETIME_ASC
        SortType.DATETIME_ASC -> SortType.DATETIME_DESC
        SortType.DATETIME_DESC -> SortType.DESCRIPTION_ASC
    }
    val icon = when (currentSort) {
        SortType.DESCRIPTION_ASC -> Icons.Default.SortByAlpha
        SortType.DESCRIPTION_DESC -> Icons.Default.SortByAlpha
        SortType.DATETIME_ASC -> Icons.Default.ArrowUpward
        SortType.DATETIME_DESC -> Icons.Default.ArrowDownward
    }
    IconButton(onClick = { onSortChange(nextSort) }) {
        Icon(icon, contentDescription = "Sort")
    }
}
