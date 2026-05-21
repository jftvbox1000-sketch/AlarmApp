package com.alarmapp

import android.app.Application
import com.alarmapp.util.InAppDebugLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isDebugBuild()) {
            Timber.plant(Timber.DebugTree())
        }
        InAppDebugLogger.log("AlarmApp", "Application started")
    }

    private fun isDebugBuild(): Boolean {
        return try {
            Class.forName("com.alarmapp.BuildConfig")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
