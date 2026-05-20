package com.alarmapp.data.repository

import com.alarmapp.data.local.dao.PublicHolidayDao
import com.alarmapp.data.local.entity.PublicHolidayEntity
import com.alarmapp.domain.model.PublicHoliday
import com.alarmapp.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HolidayRepositoryImpl @Inject constructor(
    private val holidayDao: PublicHolidayDao
) : HolidayRepository {

    override fun getAllHolidays(): Flow<List<PublicHoliday>> {
        return holidayDao.getAllHolidays().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getHolidaysList(): List<PublicHoliday> {
        return holidayDao.getHolidaysList().map { it.toDomain() }
    }

    override suspend fun addHoliday(date: Long) {
        holidayDao.insertHoliday(PublicHolidayEntity(date = date))
    }

    override suspend fun deleteHoliday(id: Long) {
        holidayDao.deleteHoliday(PublicHolidayEntity(id = id, date = 0))
    }

    override suspend fun count(): Int = holidayDao.count()

    private fun PublicHolidayEntity.toDomain(): PublicHoliday {
        return PublicHoliday(id = id, date = date)
    }
}
