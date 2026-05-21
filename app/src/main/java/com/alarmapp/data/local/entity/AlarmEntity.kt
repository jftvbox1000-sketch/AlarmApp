package com.alarmapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val hour: Int,
    val minute: Int,
    val specificDate: Long?,
    val isEnabled: Boolean = true,
    val recurrenceType: String = "ONE_TIME",
    val daysOfWeek: String = "",
    val dayOfMonth: Int? = null,
    val skipHolidays: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
