package com.alarmapp.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alarmapp.domain.model.Alarm
import com.alarmapp.receiver.AlarmReceiver
import com.alarmapp.util.AlarmPermissionHelper
import com.alarmapp.util.InAppDebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleAlarm @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(alarm: Alarm, triggerTimeMillis: Long) {
        try {
            InAppDebugLogger.log("ScheduleAlarm", "Scheduling alarm ${alarm.id} for $triggerTimeMillis")
            
            // Log permission status for debugging
            AlarmPermissionHelper.logPermissionStatus(context)
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.alarmapp.ALARM_ACTION"
                putExtra("alarm_id", alarm.id)
            }
            
            // Use a unique request code to prevent conflicts
            val requestCode = (alarm.id and 0x7FFFFFFF).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTimeMillis,
                pendingIntent
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    InAppDebugLogger.log("ScheduleAlarm", "Scheduled alarm ${alarm.id} with setAlarmClock at $triggerTimeMillis")
                    Timber.d("Scheduled alarm %d with setAlarmClock at %d", alarm.id, triggerTimeMillis)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    InAppDebugLogger.log("ScheduleAlarm", "Scheduled alarm ${alarm.id} with setExact (no exact alarm permission) at $triggerTimeMillis")
                    Timber.w("Scheduled alarm %d with setExact (no exact alarm permission) at %d", alarm.id, triggerTimeMillis)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                InAppDebugLogger.log("ScheduleAlarm", "Scheduled alarm ${alarm.id} with setAlarmClock (pre-S) at $triggerTimeMillis")
                Timber.d("Scheduled alarm %d with setAlarmClock (pre-S) at %d", alarm.id, triggerTimeMillis)
            }
        } catch (e: Exception) {
            InAppDebugLogger.logError("ScheduleAlarm", "Failed to schedule alarm ${alarm.id}", e)
            Timber.e(e, "Failed to schedule alarm %d", alarm.id)
        }
    }

    fun cancel(alarm: Alarm) {
        try {
            InAppDebugLogger.log("ScheduleAlarm", "Cancelling alarm ${alarm.id}")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.alarmapp.ALARM_ACTION"
                putExtra("alarm_id", alarm.id)
            }
            
            // Use the same unique request code as in schedule
            val requestCode = (alarm.id and 0x7FFFFFFF).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            InAppDebugLogger.log("ScheduleAlarm", "Cancelled alarm ${alarm.id}")
            Timber.d("Cancelled alarm %d", alarm.id)
        } catch (e: Exception) {
            InAppDebugLogger.logError("ScheduleAlarm", "Failed to cancel alarm ${alarm.id}", e)
            Timber.e(e, "Failed to cancel alarm %d", alarm.id)
        }
    }
}
