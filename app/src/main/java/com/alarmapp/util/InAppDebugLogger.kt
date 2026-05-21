package com.alarmapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebugLogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

object InAppDebugLogger {
    private val logs = mutableListOf<DebugLogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private const val MAX_LOGS = 1000

    fun log(tag: String, message: String, level: LogLevel = LogLevel.DEBUG) {
        try {
            val entry = DebugLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = tag,
                message = message
            )
            synchronized(logs) {
                if (logs.size >= MAX_LOGS) logs.removeAt(0)
                logs.add(entry)
            }
        } catch (_: Exception) { }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        log(tag, "$message${if (throwable != null) " - ${throwable.message}" else ""}", LogLevel.ERROR)
    }

    fun getLogs(): List<DebugLogEntry> = synchronized(logs) { logs.toList() }
    fun clearLogs() = synchronized(logs) { logs.clear() }

    fun formatLogEntry(entry: DebugLogEntry): String {
        val time = dateFormat.format(Date(entry.timestamp))
        return "[$time] ${entry.level.name}/${entry.tag}: ${entry.message}"
    }
}
