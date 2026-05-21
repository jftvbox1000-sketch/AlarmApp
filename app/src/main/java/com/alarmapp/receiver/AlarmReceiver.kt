package com.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.alarmapp.service.AlarmService
import com.alarmapp.util.InAppDebugLogger
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val WAKELOCK_TAG = "AlarmApp:AlarmWakelock"
        private const val WAKELOCK_TIMEOUT_MS = 60_000L
        var wakeLock: android.os.PowerManager.WakeLock? = null
            private set

        fun acquireWakeLock(context: Context) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            ).apply { acquire(WAKELOCK_TIMEOUT_MS) }
        }

        fun releaseWakeLock() {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            InAppDebugLogger.log("AlarmReceiver", "onReceive called: action=${intent.action}")
            Timber.d("AlarmReceiver.onReceive called: %s", intent.action)

            val alarmId = intent.getLongExtra("alarm_id", -1L)
            if (alarmId == -1L) {
                InAppDebugLogger.log("AlarmReceiver", "Invalid alarm_id received", com.alarmapp.util.LogLevel.ERROR)
                Timber.w("Received alarm intent with invalid alarm_id")
                return
            }

            InAppDebugLogger.log("AlarmReceiver", "Received alarm intent for alarm_id=$alarmId")

            acquireWakeLock(context)

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("release_wakelock", true)
            }
            context.startForegroundService(serviceIntent)
            InAppDebugLogger.log("AlarmReceiver", "Started AlarmService for alarm_id=$alarmId")
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmReceiver", "Error in onReceive", e)
            Timber.e(e, "Error in AlarmReceiver.onReceive")
            releaseWakeLock()
        }
    }
}
