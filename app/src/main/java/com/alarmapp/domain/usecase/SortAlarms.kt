package com.alarmapp.domain.usecase

import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.SortType
import javax.inject.Inject

class SortAlarms @Inject constructor(
    private val calculateNextOccurrence: CalculateNextOccurrence
) {
    suspend operator fun invoke(alarms: List<Alarm>, sortType: SortType): List<Alarm> {
        val alarmNextTimes = alarms.associateWith { alarm ->
            calculateNextOccurrence(alarm) ?: Long.MAX_VALUE
        }

        return when (sortType) {
            SortType.DESCRIPTION_ASC -> alarms.sortedBy { it.description.lowercase() }
            SortType.DESCRIPTION_DESC -> alarms.sortedByDescending { it.description.lowercase() }
            SortType.DATETIME_ASC -> alarms.sortedWith(
                compareBy<Alarm> { alarmNextTimes[it] }.thenBy { it.createdAt }
            )
            SortType.DATETIME_DESC -> alarms.sortedWith(
                compareByDescending<Alarm> { alarmNextTimes[it] }.thenByDescending { it.createdAt }
            )
        }
    }
}
