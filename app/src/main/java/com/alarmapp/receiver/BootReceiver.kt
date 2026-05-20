package com.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alarmapp.data.local.AlarmDatabase
import com.alarmapp.data.repository.AlarmRepositoryImpl
import com.alarmapp.data.repository.HolidayRepositoryImpl
import com.alarmapp.domain.usecase.CalculateNextOccurrence
import com.alarmapp.domain.usecase.ScheduleAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(context)
            val alarmDao = db.alarmDao()
            val holidayDao = db.publicHolidayDao()
            val alarmRepo = AlarmRepositoryImpl(alarmDao)
            val holidayRepo = HolidayRepositoryImpl(holidayDao)
            val calc = CalculateNextOccurrence(holidayRepo)
            val scheduler = ScheduleAlarm(context)

            alarmRepo.getAllAlarms().collect { alarms ->
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    val nextTime = calc(alarm)
                    if (nextTime != null) {
                        scheduler.schedule(alarm, nextTime)
                    }
                }
            }
        }
    }
}
