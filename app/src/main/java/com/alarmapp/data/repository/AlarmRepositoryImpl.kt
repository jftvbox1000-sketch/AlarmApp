package com.alarmapp.data.repository

import com.alarmapp.data.local.dao.AlarmDao
import com.alarmapp.data.local.entity.AlarmEntity
import com.alarmapp.domain.model.Alarm
import com.alarmapp.domain.model.RecurrenceType
import com.alarmapp.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)?.toDomain()
    }

    override suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm.toEntity())
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm.toEntity())
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm.toEntity())
    }

    override suspend fun toggleAlarm(id: Long, enabled: Boolean) {
        alarmDao.toggleAlarm(id, enabled)
    }

    private fun AlarmEntity.toDomain(): Alarm {
        return Alarm(
            id = id,
            description = description,
            hour = hour,
            minute = minute,
            specificDate = specificDate,
            isEnabled = isEnabled,
            recurrenceType = RecurrenceType.valueOf(recurrenceType),
            daysOfWeek = if (daysOfWeek.isBlank()) emptySet()
                else daysOfWeek.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet(),
            dayOfMonth = dayOfMonth,
            createdAt = createdAt
        )
    }

    private fun Alarm.toEntity(): AlarmEntity {
        return AlarmEntity(
            id = id,
            description = description,
            hour = hour,
            minute = minute,
            specificDate = specificDate,
            isEnabled = isEnabled,
            recurrenceType = recurrenceType.name,
            daysOfWeek = daysOfWeek.joinToString(",") { it.name },
            dayOfMonth = dayOfMonth,
            createdAt = createdAt
        )
    }
}
