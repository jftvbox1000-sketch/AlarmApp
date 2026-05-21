#  AlarmApp — Specification

## Overview
Android alarm clock app built with **Kotlin + Jetpack Compose**. Uses clean architecture (data/domain/ui layers).

## Tech Stack
| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material3 |
| DI | Dagger Hilt |
| Database | Room (SQLite) |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Build | AGP 8.5.0, Kotlin 2.0.10, Gradle 8.7 |

## Screens (4)

### 1. Alarm List (`AlarmListScreen`)
- Shows all alarms in a scrollable list
- Each card: time, description, date, recurrence pattern, enable/disable toggle, delete button
- FAB to add new alarm
- Sort button cycles: description A→Z, description Z→A, date ascending, date descending
- Top bar has holidays button

### 2. Alarm Editor (`AlarmEditorScreen`)
- Create or edit an alarm (pass `alarmId` via navigation, -1 = new)
- Fields: description text, time picker, optional date picker (for one-time alarms)
- Recurrence type: none/one-time, custom days of week, weekdays (Mon–Fri), monthly (Nth day)
- Day-of-week chip selector, day-of-month number input
- Save button with loading state

### 3. Alarm Ring (`AlarmRingActivity`)
- Full-screen activity that plays alarm sound + vibrates
- **Dismiss**: stops alarm. If recurring, schedules next occurrence. If one-time, disables it.
- **Snooze** (5 min): creates a new PendingIntent for 5 minutes later

### 4. Holiday Manager (`HolidayScreen`)
- Lists public holidays (max 20)
- Add holiday via DatePicker dialog
- Delete holiday via icon
- Used by `CalculateNextOccurrence` to skip alarms on holidays

## Architecture

```
ui/          → Composable screens + ViewModels
domain/      → Models (Alarm, PublicHoliday), Repository interfaces, Use cases
data/        → Room DB, DAOs, Entity → Domain mappers, Repository implementations
di/          → Hilt module (provides DB, DAOs, binds repos)
service/     → AlarmService (foreground service, plays sound/vibration)
receiver/    → AlarmReceiver (triggers from AlarmManager), BootReceiver (reschedule on boot)
```

## Permissions
`SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`

## Key Behaviors
- **Boot**: `BootReceiver` reschedules all enabled alarms via `AlarmManager.setExactAndAllowWhileIdle`
- **Recurrence**: `CalculateNextOccurrence` use case computes the next alarm time based on recurrence type + holidays
- **One-time alarms**: use a `specificDate` field; skipped after firing (disabled)
- **Notifications**: foreground service notification with full-screen intent to `AlarmRingActivity`
- **Database**: alarm entities + public holiday entities with `TypeConverters` for `Set<DayOfWeek>`
