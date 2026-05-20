package com.alarmapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "public_holidays")
data class PublicHolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long
)
