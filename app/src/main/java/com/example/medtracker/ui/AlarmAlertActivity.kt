package com.example.medtracker.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.reminder.MedicationAlarmReceiver

class AlarmAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        val medication = MedicationRepository(this).getAll().firstOrNull { it.id == medicationId }

        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
            ) {
                AlarmAlertScreen(
                    title = medication?.name ?: "Medikament",
                    dosage = medication?.dosage ?: "",
                    notes = medication?.notes ?: "",
                    onTaken = {
                        sendBroadcast(
                            MedicationAlarmReceiver.createIntent(this, medicationId).apply {
                                action = "com.example.medtracker.action.MARK_TAKEN"
                            }
                        )
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    companion object {
        private const val EXTRA_MEDICATION_ID = "extra_medication_id"

        fun createIntent(context: Context, medicationId: Int): Intent {
            return Intent(context, AlarmAlertActivity::class.java)
                .putExtra(EXTRA_MEDICATION_ID, medicationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

@Composable
private fun AlarmAlertScreen(
    title: String,
    dosage: String,
    notes: String,
    onTaken: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Erinnerung", style = MaterialTheme.typography.headlineMedium)
        Text(text = title, style = MaterialTheme.typography.displaySmall)
        if (dosage.isNotBlank()) {
            Text(text = dosage, style = MaterialTheme.typography.titleLarge)
        }
        if (notes.isNotBlank()) {
            Text(text = notes, style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onTaken, modifier = Modifier.fillMaxWidth()) {
            Text("Als genommen markieren")
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Spaeter")
        }
    }
}
