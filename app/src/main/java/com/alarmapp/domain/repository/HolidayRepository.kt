package com.alarmapp.domain.repository

import com.alarmapp.domain.model.PublicHoliday
import kotlinx.coroutines.flow.Flow

interface HolidayRepository {
    fun getAllHolidays(): Flow<List<PublicHoliday>>
    suspend fun getHolidaysList(): List<PublicHoliday>
    suspend fun addHoliday(date: Long)
    suspend fun deleteHoliday(id: Long)
    suspend fun count(): Int
}
