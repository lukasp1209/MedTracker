package com.example.medtracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medtracker.model.Medication
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleMedication(medication: Medication) {
        medication.intakeTimes.forEachIndexed { index, _ ->
            scheduleMedicationSlot(medication, index)
        }
    }

    fun cancelMedication(id: Int) {
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

    fun rescheduleAll(medications: List<Medication>) {
        medications.forEach { scheduleMedication(it) }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

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

    private fun nextTriggerTimeMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun requestCode(medicationId: Int, slotIndex: Int): Int = medicationId * SLOT_FACTOR + slotIndex

    companion object {
        private const val SLOT_FACTOR = 100
        private const val MAX_SLOTS_PER_MEDICATION = SLOT_FACTOR
    }
}
