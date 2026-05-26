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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.reminder.MedicationAlarmReceiver
import com.example.medtracker.ui.theme.MedTrackerTheme
import kotlinx.coroutines.launch

/**
 * Full-screen alarm activity shown when a medication reminder needs immediate attention.
 */
class AlarmAlertActivity : ComponentActivity() {
    /**
     * Loads the alarm medication, renders the alert UI, and wires the action buttons.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)

        lifecycleScope.launch {
            val medication = MedicationRepository(this@AlarmAlertActivity).getAll().firstOrNull { it.id == medicationId }
            val intakeLabel = medication?.intakeTimes?.getOrNull(slotIndex)?.label().orEmpty()

            setContent {
                MedTrackerTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AlarmAlertScreen(
                            title = medication?.name ?: "Medikament",
                            dosage = medication?.dosage.orEmpty(),
                            intakeLabel = intakeLabel,
                            notes = medication?.notes.orEmpty(),
                            onTaken = {
                                sendBroadcast(
                                    MedicationAlarmReceiver.createIntent(this@AlarmAlertActivity, medicationId, slotIndex).apply {
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
        }
    }

    /**
     * Configures the activity so it can appear over the lock screen and wake the device.
     */
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
        private const val EXTRA_SLOT_INDEX = "extra_slot_index"

        /**
         * Builds the intent used by reminders to open this alarm screen.
         */
        fun createIntent(context: Context, medicationId: Int, slotIndex: Int): Intent {
            return Intent(context, AlarmAlertActivity::class.java)
                .putExtra(EXTRA_MEDICATION_ID, medicationId)
                .putExtra(EXTRA_SLOT_INDEX, slotIndex)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

/**
 * Displays the visible alarm content and exposes callbacks for the user's decision.
 */
@Composable
private fun AlarmAlertScreen(
    title: String,
    dosage: String,
    intakeLabel: String,
    notes: String,
    onTaken: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Erinnerung", style = MaterialTheme.typography.titleMedium)
                Text(text = title, style = MaterialTheme.typography.displaySmall)
                if (dosage.isNotBlank()) {
                    Text(text = dosage, style = MaterialTheme.typography.headlineSmall)
                }
                if (intakeLabel.isNotBlank()) {
                    Text(text = "Geplant für $intakeLabel", style = MaterialTheme.typography.bodyLarge)
                }
                if (notes.isNotBlank()) {
                    Text(text = notes, style = MaterialTheme.typography.bodyLarge)
                }
                Button(onClick = onTaken, modifier = Modifier.fillMaxWidth()) {
                    Text("Als genommen markieren")
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Später")
                }
            }
        }
    }
}
