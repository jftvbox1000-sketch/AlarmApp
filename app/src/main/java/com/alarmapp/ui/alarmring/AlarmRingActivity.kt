package com.alarmapp.ui.alarmring

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alarmapp.domain.repository.AlarmRepository
import com.alarmapp.domain.usecase.CalculateNextOccurrence
import com.alarmapp.domain.usecase.ScheduleAlarm
import com.alarmapp.receiver.AlarmReceiver
import com.alarmapp.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingActivity : ComponentActivity() {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var calculateNextOccurrence: CalculateNextOccurrence
    @Inject lateinit var scheduleAlarm: ScheduleAlarm

    private var alarmId: Long = -1L
    private var description: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("AlarmRingActivity.onCreate called with intent: %s", intent)
        
        alarmId = intent.getLongExtra("alarm_id", -1L)
        description = intent.getStringExtra("description") ?: "Alarm"
        
        if (alarmId == -1L) {
            Timber.w("AlarmRingActivity started with invalid alarm_id")
            finish()
            return
        }
        
        Timber.d("Showing alarm ring UI for alarm %d: %s", alarmId, description)

        setContent {
            AlarmRingScreen(
                description = description,
                onDismiss = { dismissAlarm() },
                onSnooze = { snoozeAlarm() }
            )
        }
    }

    private fun dismissAlarm() {
        try {
            Timber.d("Dismissing alarm %d", alarmId)
            CoroutineScope(Dispatchers.IO).launch {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.isRecurring) {
                        val nextTime = calculateNextOccurrence(alarm)
                        if (nextTime != null) {
                            scheduleAlarm.schedule(alarm, nextTime)
                            Timber.d("Scheduled next occurrence for recurring alarm %d", alarmId)
                        } else {
                            Timber.w("No next occurrence found for recurring alarm %d", alarmId)
                        }
                    } else {
                        alarmRepository.toggleAlarm(alarmId, false)
                        Timber.d("Disabled one-time alarm %d", alarmId)
                    }
                } else {
                    Timber.w("Alarm %d not found when dismissing", alarmId)
                }
            }
            stopAlarmService()
            finish()
        } catch (e: Exception) {
            Timber.e(e, "Error dismissing alarm %d", alarmId)
        }
    }

    private fun snoozeAlarm() {
        try {
            Timber.d("Snoozing alarm %d", alarmId)
            val snoozeTime = LocalDateTime.now().plusMinutes(5)
            val triggerMillis = snoozeTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = "com.alarmapp.ALARM_ACTION"
                putExtra("alarm_id", alarmId)
            }
            
            // Use a unique request code for snooze
            val requestCode = ((alarmId * 1000 + 999) and 0x7FFFFFFF).toInt()
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                this, requestCode, snoozeIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                    )
                    Timber.d("Scheduled snooze alarm %d with setExactAndAllowWhileIdle at %d", alarmId, triggerMillis)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Timber.w("Scheduled snooze alarm %d with set (no exact alarm permission) at %d", alarmId, triggerMillis)
                }
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                Timber.d("Scheduled snooze alarm %d with setExact (pre-S) at %d", alarmId, triggerMillis)
            }
            stopAlarmService()
            finish()
        } catch (e: Exception) {
            Timber.e(e, "Error snoozing alarm %d", alarmId)
        }
    }

    private fun stopAlarmService() {
        try {
            val intent = Intent(this, AlarmService::class.java)
            stopService(intent)
            Timber.d("Stopped AlarmService for alarm %d", alarmId)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping AlarmService for alarm %d", alarmId)
        }
    }
}

@Composable
private fun AlarmRingScreen(
    description: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ALARM",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Dismiss", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Snooze (5 min)", style = MaterialTheme.typography.titleLarge)
        }
    }
}
