package com.alarmapp.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alarmapp.domain.model.Alarm
import com.alarmapp.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleAlarm @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(alarm: Alarm, triggerTimeMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.alarmapp.ALARM_ACTION"
                putExtra("alarm_id", alarm.id)
            }
            
            val requestCode = (alarm.id and 0x7FFFFFFF).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTimeMillis,
                pendingIntent
            )

            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: Exception) { }
    }

    fun cancel(alarm: Alarm) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.alarmapp.ALARM_ACTION"
                putExtra("alarm_id", alarm.id)
            }
            
            val requestCode = (alarm.id and 0x7FFFFFFF).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) { }
    }
}
