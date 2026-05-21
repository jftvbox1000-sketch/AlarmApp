package com.alarmapp.domain.model

import java.time.DayOfWeek

data class Alarm(
    val id: Long = 0,
    val description: String,
    val hour: Int,
    val minute: Int,
    val specificDate: Long?,
    val isEnabled: Boolean = true,
    val recurrenceType: RecurrenceType = RecurrenceType.ONE_TIME,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
    val skipHolidays: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isRecurring: Boolean get() = specificDate == null

    val triggerTimeMillis: Long?
        get() = specificDate?.let { it + hour * 3600000L + minute * 60000L }
}
