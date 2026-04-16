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
        val triggerAtMillis = nextTriggerTimeMillis(medication)
        val receiverIntent = MedicationAlarmReceiver.createIntent(context, medication.id)
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            medication.id,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val infoIntent = PendingIntent.getActivity(
            context,
            medication.id,
            Intent(context, com.example.medtracker.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClock = AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent)
        alarmManager.setAlarmClock(alarmClock, alarmIntent)
    }

    fun cancelMedication(id: Int) {
        val intent = MedicationAlarmReceiver.createIntent(context, id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
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

    private fun nextTriggerTimeMillis(medication: Medication): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(medication.hour).withMinute(medication.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
