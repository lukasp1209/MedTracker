package com.example.medtracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medtracker.data.MedicationRepository

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val repository = MedicationRepository(context)
            ReminderScheduler(context).rescheduleAll(repository.getAll())
        }
    }
}
