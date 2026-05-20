package com.alarmapp.domain.usecase

import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.SortType
import javax.inject.Inject

class SortAlarms @Inject constructor() {
    operator fun invoke(alarms: List<Alarm>, sortType: SortType): List<Alarm> {
        return when (sortType) {
            SortType.DESCRIPTION_ASC -> alarms.sortedBy { it.description.lowercase() }
            SortType.DESCRIPTION_DESC -> alarms.sortedByDescending { it.description.lowercase() }
            SortType.DATETIME_ASC -> alarms.sortedWith(compareBy<Alarm> { it.triggerTimeMillis ?: Long.MAX_VALUE }.thenBy { it.createdAt })
            SortType.DATETIME_DESC -> alarms.sortedWith(compareByDescending<Alarm> { it.triggerTimeMillis ?: Long.MIN_VALUE }.thenByDescending { it.createdAt })
        }
    }
}
