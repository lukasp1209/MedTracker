package com.example.medtracker.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medtracker.R
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.ui.AlarmAlertActivity

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        if (medicationId == -1) return

        val repository = MedicationRepository(context)
        val medication = repository.getAll().firstOrNull { it.id == medicationId } ?: return
        createNotificationChannel(context)

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            medication.id,
            AlarmAlertActivity.createIntent(context, medication.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markTakenIntent = PendingIntent.getBroadcast(
            context,
            medication.id + ACTION_OFFSET_MARK_TAKEN,
            createIntent(context, medication.id).apply { action = ACTION_MARK_TAKEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (intent.action == ACTION_MARK_TAKEN) {
            repository.markTaken(medication.id)
            ReminderScheduler(context).scheduleMedication(medication)
            NotificationManagerCompat.from(context).cancel(medication.id)
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ReminderScheduler(context).scheduleMedication(medication)
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Zeit für ${medication.name}")
            .setContentText("${medication.dosage} jetzt einnehmen")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "Als genommen markieren", markTakenIntent)
            .build()

        NotificationManagerCompat.from(context).notify(medication.id, notification)
        ReminderScheduler(context).scheduleMedication(medication)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medikamenten-Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Laute und sichtbare Erinnerungen für Medikamente"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(soundUri, attributes)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val EXTRA_MEDICATION_ID = "extra_medication_id"
        private const val ACTION_MARK_TAKEN = "com.example.medtracker.action.MARK_TAKEN"
        private const val ACTION_OFFSET_MARK_TAKEN = 10_000
        private const val CHANNEL_ID = "medication_reminders"

        fun createIntent(context: Context, medicationId: Int): Intent {
            return Intent(context, MedicationAlarmReceiver::class.java)
                .putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
    }
}
