package com.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.alarmapp.service.AlarmService

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
            ).apply {
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        }

        fun releaseWakeLock() {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1L)
        if (alarmId == -1L) return

        acquireWakeLock(context)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        context.startForegroundService(serviceIntent)
    }
}
