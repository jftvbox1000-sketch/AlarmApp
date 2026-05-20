package com.alarmapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alarmapp.data.local.entity.PublicHolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicHolidayDao {
    @Query("SELECT * FROM public_holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<PublicHolidayEntity>>

    @Query("SELECT * FROM public_holidays ORDER BY date ASC")
    suspend fun getHolidaysList(): List<PublicHolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: PublicHolidayEntity): Long

    @Delete
    suspend fun deleteHoliday(holiday: PublicHolidayEntity)

    @Query("SELECT COUNT(*) FROM public_holidays")
    suspend fun count(): Int
}
