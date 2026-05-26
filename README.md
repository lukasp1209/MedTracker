# MedTracker

MedTracker is a simple Android app for managing medication intake. Users can add medications, define dosage and intake times, choose weekdays, receive reminders, mark medication as taken, and review an intake history.

The project is built with Kotlin, Jetpack Compose, Room, StateFlow, and Android's alarm and notification APIs.

## Main Features

- Add medications with name, dosage, notes, intake times, and weekdays.
- Show a daily overview of all planned medications.
- Show a weekly medication schedule grouped by weekday.
- Trigger exact medication reminders with Android `AlarmManager`.
- Show high-priority reminder notifications and a full-screen alarm screen.
- Mark medication as taken and store the intake in a history table.
- Restore alarms after device reboot or app update.
- Use a modern Jetpack Compose UI with Material Design 3.

## Project Structure

The main source code is located in:

```text
app/src/main/java/com/example/medtracker
```

```text
com.example.medtracker
+-- MainActivity.kt
+-- data
|   +-- MedicationRepository.kt
|   +-- local
|       +-- AppDatabase.kt
|       +-- MedicationDao.kt
|       +-- MedicationEntity.kt
+-- model
|   +-- Medication.kt
|   +-- IntakeTime.kt
+-- reminder
|   +-- ReminderScheduler.kt
|   +-- MedicationAlarmReceiver.kt
|   +-- BootCompletedReceiver.kt
+-- ui
    +-- AlarmAlertActivity.kt
    +-- HistoryScreen.kt
    +-- MedTrackerViewModel.kt
    +-- theme
        +-- Theme.kt
```

## Architecture Overview

The app is separated into several layers:

- `model`: Contains the domain models used by the app logic and UI.
- `data/local`: Contains the Room database, entities, type converters, and DAO queries.
- `data`: Contains the repository, which connects the database with the rest of the app.
- `ui`: Contains the ViewModel and Jetpack Compose screens.
- `reminder`: Contains alarm scheduling, notification handling, and boot rescheduling.

This separation keeps database logic, UI logic, and background reminder logic easier to understand and maintain.

## Important Classes

### Models

`Medication.kt` describes one medication in the app. It contains the id, name, dosage, intake times, notes, last intake timestamp, and selected weekdays.

`IntakeTime.kt` describes one daily intake time. Its `label()` function formats the time as a readable value such as `08:30`.

### Database Layer

`AppDatabase.kt` defines the Room database. It stores:

- `MedicationEntity`: medication records
- `IntakeHistoryEntity`: recorded intake events

The same file also contains `Converters`, which convert complex values such as `List<IntakeTime>` and `Set<DayOfWeek>` into database-friendly strings.

`MedicationDao.kt` defines all SQL operations. It can read, insert, update, and delete medications, and it can store and observe intake history entries.

`MedicationEntity.kt` contains the database representations of medications and intake history entries.

### Repository Layer

`MedicationRepository.kt` is the central data access class. The UI and reminder logic do not talk directly to Room. Instead, they use the repository.

The repository:

- observes all medications
- loads all medications once for background work
- adds new medications
- deletes medications
- marks medications as taken
- writes history entries
- converts database entities into domain models

### ViewModel Layer

`MedTrackerViewModel.kt` connects the UI with the repository and reminder scheduler.

It exposes:

- `medications`: current medication list as `StateFlow`
- `history`: current intake history as `StateFlow`

It also provides functions for user actions:

- `addMedication(...)`
- `deleteMedication(...)`
- `markTaken(...)`
- `canScheduleExactAlarms()`

### Reminder Layer

`ReminderScheduler.kt` schedules exact alarms for medication intake times. It can schedule all reminders for a medication, cancel reminders for a medication, and reschedule all reminders.

`MedicationAlarmReceiver.kt` is called when an alarm fires. It loads the correct medication, shows the reminder notification, handles the "mark as taken" action, and schedules the next reminder.

`BootCompletedReceiver.kt` restores all reminders after a device reboot or app package update.

### UI Layer

`MainActivity.kt` is the main entry point. It creates the ViewModel, applies the app theme, and switches between the main screens:

- daily overview
- weekly schedule
- intake history

Important Compose functions in `MainActivity.kt` include:

- `MedTrackerScreen`
- `HeroCard`
- `MedicationForm`
- `WeeklyScheduleScreen`
- `MedicationList`
- `MedicationCard`

`HistoryScreen.kt` displays the intake history.

`AlarmAlertActivity.kt` displays a full-screen alarm UI when a reminder needs immediate attention.

`Theme.kt` defines the Material Design 3 color scheme and typography.

## Data Flow

### Adding a medication

```text
User enters medication data
        |
        v
MedicationForm calls ViewModel.addMedication()
        |
        v
ViewModel calls Repository.add()
        |
        v
Repository creates a MedicationEntity
        |
        v
Room stores the entity in the database
        |
        v
Room emits the updated medication list through Flow
        |
        v
ViewModel exposes the list as StateFlow
        |
        v
Compose UI updates automatically
        |
        v
ReminderScheduler schedules the medication alarms
```

### Reminder alarm

```text
Android AlarmManager triggers an alarm
        |
        v
MedicationAlarmReceiver receives the alarm
        |
        v
Receiver loads the medication from the repository
        |
        v
Notification or full-screen alarm is shown
        |
        v
User marks the medication as taken
        |
        v
Repository updates lastTakenAt
        |
        v
Repository writes an intake history entry
        |
        v
UI and history update automatically
        |
        v
Next reminder is scheduled
```

## 10-Minute Presentation Plan

1. Explain what MedTracker does and which problem it solves.
2. Show the folder structure and name the main layers.
3. Explain the model classes `Medication` and `IntakeTime`.
4. Explain Room with `AppDatabase`, `MedicationEntity`, and `MedicationDao`.
5. Explain why `MedicationRepository` exists as a clean access point for data.
6. Explain `MedTrackerViewModel` and how `StateFlow` updates the Compose UI.
7. Explain the main Compose screens in `MainActivity.kt`.
8. Explain the reminder system with `ReminderScheduler` and `MedicationAlarmReceiver`.
9. Walk through the "add medication" data flow.
10. Walk through the "alarm fires and medication is marked as taken" data flow.

## Technologies

- Kotlin
- Jetpack Compose
- Material Design 3
- Room database
- Kotlin Coroutines
- Flow and StateFlow
- Android `AlarmManager`
- Android notifications
- Java Time API

## Requirements

- Android 8.0, API level 26, or newer.
- Exact alarm permission is required on Android 12 and newer.
- Notification permission is required on Android 13 and newer.

## Development

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync the project.
4. Run the app on an emulator or physical Android device.
