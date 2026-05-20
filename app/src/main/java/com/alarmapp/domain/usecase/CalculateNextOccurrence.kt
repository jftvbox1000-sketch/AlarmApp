package com.alarmapp.domain.usecase

import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.RecurrenceType
import com.alarmapp.domain.repository.HolidayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class CalculateNextOccurrence @Inject constructor(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(alarm: Alarm): Long? {
        val now = LocalDateTime.now()
        return when {
            alarm.specificDate != null -> {
                val alarmTime = combine(alarm.specificDate, alarm.hour, alarm.minute)
                if (alarmTime > now) toEpochMillis(alarmTime) else null
            }
            else -> findNextRecurrence(alarm, now)
        }
    }

    private suspend fun findNextRecurrence(alarm: Alarm, now: LocalDateTime): Long? {
        val holidays = holidayRepository.getHolidaysList().map { h ->
            LocalDate.ofEpochDay(h.date / 86400000)
        }.toSet()

        val time = LocalTime.of(alarm.hour, alarm.minute)
        var current = now.toLocalDate()

        repeat(366) {
            val candidate = when (alarm.recurrenceType) {
                RecurrenceType.DAYS_OF_WEEK -> {
                    if (current.dayOfWeek in alarm.daysOfWeek) {
                        LocalDateTime.of(current, time)
                    } else null
                }
                RecurrenceType.WEEKDAYS -> {
                    if (current.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                        LocalDateTime.of(current, time)
                    } else null
                }
                RecurrenceType.MONTHLY_NTH_DAY -> {
                    val day = (alarm.dayOfMonth ?: 1).coerceAtMost(current.lengthOfMonth())
                    LocalDateTime.of(current.withDayOfMonth(day), time)
                }
                RecurrenceType.ONE_TIME -> null
            }
            if (candidate != null && candidate.isAfter(now) && candidate.toLocalDate() !in holidays) {
                return toEpochMillis(candidate)
            }
            current = when (alarm.recurrenceType) {
                RecurrenceType.MONTHLY_NTH_DAY -> current.plusMonths(1)
                else -> current.plusDays(1)
            }
        }
        return null
    }

    private fun combine(dateMillis: Long, hour: Int, minute: Int): LocalDateTime {
        val date = LocalDate.ofEpochDay(dateMillis / 86400000)
        return LocalDateTime.of(date, LocalTime.of(hour, minute))
    }

    private fun toEpochMillis(dt: LocalDateTime): Long {
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
