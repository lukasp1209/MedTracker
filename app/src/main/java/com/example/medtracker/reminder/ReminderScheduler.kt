package com.example.medtracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medtracker.model.Medication
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules and cancels exact Android alarms for medication reminders.
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules every configured intake time for one medication.
     */
    fun scheduleMedication(medication: Medication) {
        medication.intakeTimes.forEachIndexed { index, _ ->
            scheduleMedicationSlot(medication, index)
        }
    }

    /**
     * Cancels all possible alarm slots for the medication with the given id.
     */
    fun cancelMedication(id: Int) {
        DirectBootReminderStore.remove(context, id)
        for (slotIndex in 0 until MAX_SLOTS_PER_MEDICATION) {
            val intent = MedicationAlarmReceiver.createIntent(context, id, slotIndex)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(id, slotIndex),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules reminders for all medications in the provided list.
     */
    fun rescheduleAll(medications: List<Medication>) {
        DirectBootReminderStore.save(context, medications)
        medications.forEach { scheduleMedication(it) }
    }

    /**
     * Checks whether the app is allowed to schedule exact alarms on this Android version.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules one specific intake slot for the next matching time.
     */
    fun scheduleMedicationSlot(medication: Medication, slotIndex: Int) {
        val intakeTime = medication.intakeTimes.getOrNull(slotIndex) ?: return
        val triggerAtMillis = nextTriggerTimeMillis(intakeTime.hour, intakeTime.minute)
        val receiverIntent = MedicationAlarmReceiver.createIntent(context, medication.id, slotIndex)
        val requestCode = requestCode(medication.id, slotIndex)
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val infoIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, com.example.medtracker.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClock = AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent)
        alarmManager.setAlarmClock(alarmClock, alarmIntent)
    }

    /**
     * Calculates the next future timestamp for the given hour and minute.
     */
    private fun nextTriggerTimeMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Creates a unique request code for a medication and one of its intake slots.
     */
    private fun requestCode(medicationId: Int, slotIndex: Int): Int = medicationId * SLOT_FACTOR + slotIndex

    companion object {
        private const val SLOT_FACTOR = 100
        private const val MAX_SLOTS_PER_MEDICATION = SLOT_FACTOR
    }
}
