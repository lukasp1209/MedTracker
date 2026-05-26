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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles fired medication alarms and shows the high-priority reminder notification.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {
    /**
     * Loads the medication for the alarm, handles quick actions, and schedules the next occurrence.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)
        if (medicationId == -1 || slotIndex == -1) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MedicationRepository(context)
                val medication = repository.getAll().firstOrNull { it.id == medicationId } ?: return@launch
                val intakeTime = medication.intakeTimes.getOrNull(slotIndex) ?: return@launch
                createNotificationChannel(context)

                val notificationId = notificationId(medication.id, slotIndex)
                val fullScreenIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    AlarmAlertActivity.createIntent(context, medication.id, slotIndex),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val markTakenIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + ACTION_OFFSET_MARK_TAKEN,
                    createIntent(context, medication.id, slotIndex).apply { action = ACTION_MARK_TAKEN },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (intent.action == ACTION_MARK_TAKEN) {
                    repository.markTaken(medication.id)
                    ReminderScheduler(context).scheduleMedicationSlot(medication, slotIndex)
                    NotificationManagerCompat.from(context).cancel(notificationId)
                    return@launch
                }

                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    ReminderScheduler(context).scheduleMedicationSlot(medication, slotIndex)
                    return@launch
                }

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Zeit fuer ${medication.name}")
                    .setContentText(buildNotificationText(medication.dosage, intakeTime.label()))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setFullScreenIntent(fullScreenIntent, true)
                    .addAction(0, "Als genommen markieren", markTakenIntent)
                    .build()

                NotificationManagerCompat.from(context).notify(notificationId, notification)
                ReminderScheduler(context).scheduleMedicationSlot(medication, slotIndex)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Creates the Android notification channel used for medication reminders.
     */
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
        private const val EXTRA_SLOT_INDEX = "extra_slot_index"
        private const val ACTION_MARK_TAKEN = "com.example.medtracker.action.MARK_TAKEN"
        private const val ACTION_OFFSET_MARK_TAKEN = 10_000
        private const val CHANNEL_ID = "medication_reminders"

        /**
         * Builds an intent containing the medication and intake slot identifiers.
         */
        fun createIntent(context: Context, medicationId: Int, slotIndex: Int): Intent {
            return Intent(context, MedicationAlarmReceiver::class.java)
                .putExtra(EXTRA_MEDICATION_ID, medicationId)
                .putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }

        /**
         * Creates a stable notification id for one medication intake slot.
         */
        private fun notificationId(medicationId: Int, slotIndex: Int): Int = medicationId * 100 + slotIndex

        /**
         * Builds the notification body text from dosage and scheduled time.
         */
        private fun buildNotificationText(dosage: String, timeLabel: String): String {
            return if (dosage.isBlank()) {
                "Einnahme um $timeLabel"
            } else {
                "$dosage um $timeLabel einnehmen"
            }
        }
    }
}
