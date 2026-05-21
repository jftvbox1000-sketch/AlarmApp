package com.alarmapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.alarmapp.domain.repository.AlarmRepository
import com.alarmapp.receiver.AlarmReceiver
import com.alarmapp.util.InAppDebugLogger
import com.alarmapp.ui.alarmring.AlarmRingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject lateinit var alarmRepository: AlarmRepository

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: Long = -1L
    private var keepAwake: PowerManager.WakeLock? = null
    private var shouldReleaseReceiverWakelock: Boolean = false

    override fun onCreate() {
        super.onCreate()
        InAppDebugLogger.log("AlarmService", "onCreate")
        Timber.d("AlarmService.onCreate")
        createNotificationChannel()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        keepAwake = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmApp:AlarmService"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        InAppDebugLogger.log("AlarmService", "onStartCommand called")
        Timber.d("AlarmService.onStartCommand called with intent: %s", intent)

        val alarmId = intent?.getLongExtra("alarm_id", -1L) ?: -1L
        currentAlarmId = alarmId
        shouldReleaseReceiverWakelock = intent?.getBooleanExtra("release_wakelock", false) ?: false
        
        if (alarmId == -1L) {
            InAppDebugLogger.log("AlarmService", "Invalid alarm_id - stopping self", com.alarmapp.util.LogLevel.ERROR)
            Timber.w("AlarmService started with invalid alarm_id")
            stopSelf()
            return START_NOT_STICKY
        }
        
        InAppDebugLogger.log("AlarmService", "Starting alarm $alarmId")
        Timber.d("Starting alarm %d, shouldReleaseReceiverWakelock: %b", alarmId, shouldReleaseReceiverWakelock)

        // Acquire our own wakelock
        keepAwake?.acquire(60_000L)

        showNotification("Alarm")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    val description = alarm.description.ifBlank { "Alarm" }
                    InAppDebugLogger.log("AlarmService", "DB lookup success for alarm $alarmId: $description")
                    Timber.d("DB lookup success: alarm %d: %s", alarmId, description)
                    showNotification(description)
                } else {
                    InAppDebugLogger.log("AlarmService", "Alarm $alarmId not found in DB", com.alarmapp.util.LogLevel.WARN)
                    Timber.w("Alarm %d not found in database", alarmId)
                }
            } catch (e: Exception) {
                InAppDebugLogger.logError("AlarmService", "DB error for alarm $alarmId", e)
                Timber.e(e, "Error getting alarm %d from database", alarmId)
            }
        }

        startAlarm()

        // Release the receiver's wakelock only after we've acquired our own
        if (shouldReleaseReceiverWakelock) {
            AlarmReceiver.releaseWakeLock()
            InAppDebugLogger.log("AlarmService", "Released receiver wakelock for alarm $alarmId")
            Timber.d("Released receiver wakelock for alarm %d", alarmId)
        }

        return START_NOT_STICKY
    }

    private fun showNotification(description: String) {
        try {
            val _alarmId = currentAlarmId
            val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
                putExtra("alarm_id", _alarmId)
                putExtra("description", description)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this, _alarmId.toInt(), fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(description)
                .setContentText(description)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            InAppDebugLogger.log("AlarmService", "Foreground notification started for alarm $_alarmId")
            Timber.d("Started foreground notification for alarm %d", _alarmId)
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error showing notification for alarm $currentAlarmId", e)
        }
    }

    private fun startAlarm() {
        try {
            InAppDebugLogger.log("AlarmService", "Starting sound and vibration for alarm $currentAlarmId")
            Timber.d("Starting alarm sound and vibration for alarm %d", currentAlarmId)
            playSound()
            vibrate()
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error starting alarm", e)
            Timber.e(e, "Error starting alarm for alarm %d", currentAlarmId)
        }
    }

    private fun playSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            InAppDebugLogger.log("AlarmService", "Sound started for alarm $currentAlarmId")
            Timber.d("Started alarm sound for alarm %d", currentAlarmId)
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error playing sound", e)
            Timber.e(e, "Error playing alarm sound for alarm %d", currentAlarmId)
        }
    }

    private fun vibrate() {
        try {
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            InAppDebugLogger.log("AlarmService", "Vibration started for alarm $currentAlarmId")
            Timber.d("Started vibration for alarm %d", currentAlarmId)
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error vibrating", e)
            Timber.e(e, "Error vibrating for alarm %d", currentAlarmId)
        }
    }

    fun stopAlarm() {
        try {
            InAppDebugLogger.log("AlarmService", "Stopping alarm $currentAlarmId")
            Timber.d("Stopping alarm %d", currentAlarmId)
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            vibrator?.cancel()
            keepAwake?.takeIf { it.isHeld }?.release()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error stopping alarm", e)
            Timber.e(e, "Error stopping alarm %d", currentAlarmId)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        try {
            InAppDebugLogger.log("AlarmService", "onDestroy for alarm $currentAlarmId")
            Timber.d("AlarmService.onDestroy for alarm %d", currentAlarmId)
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
            keepAwake?.takeIf { it.isHeld }?.release()
            
            // Make sure to release receiver wakelock if we haven't already
            if (shouldReleaseReceiverWakelock) {
                AlarmReceiver.releaseWakeLock()
            }
            
            super.onDestroy()
        } catch (e: Exception) {
            Timber.e(e, "Error in AlarmService.onDestroy")
        }
    }

    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            InAppDebugLogger.log("AlarmService", "Notification channel created")
            Timber.d("Created notification channel")
        } catch (e: Exception) {
            InAppDebugLogger.logError("AlarmService", "Error creating notification channel", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
    }
}
