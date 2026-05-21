package com.alarmapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.alarmapp.data.local.entity.AlarmEntity
import com.alarmapp.data.local.entity.PublicHolidayEntity
import com.alarmapp.data.local.dao.AlarmDao
import com.alarmapp.data.local.dao.PublicHolidayDao
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(value: Set<DayOfWeek>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet()
    }
}

@Database(entities = [AlarmEntity::class, PublicHolidayEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun publicHolidayDao(): PublicHolidayDao

    companion object {
        @Volatile private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AlarmDatabase::class.java, "alarm_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
