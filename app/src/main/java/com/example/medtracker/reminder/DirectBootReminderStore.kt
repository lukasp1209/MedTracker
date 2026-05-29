package com.example.medtracker.reminder

import android.content.Context
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

/**
 * Stores the minimum reminder data in Device Protected Storage for direct boot.
 */
object DirectBootReminderStore {
    private const val PREFS_NAME = "direct_boot_reminders"
    private const val KEY_MEDICATIONS = "medications"

    /**
     * Saves medication reminder data where direct-boot-aware receivers can read it before unlock.
     */
    fun save(context: Context, medications: List<Medication>) {
        val array = JSONArray()
        medications.forEach { medication ->
            array.put(JSONObject().apply {
                put("id", medication.id)
                put("name", medication.name)
                put("dosage", medication.dosage)
                put("notes", medication.notes)
                put("lastTakenAt", medication.lastTakenAt ?: JSONObject.NULL)
                put("intakeTimes", JSONArray().apply {
                    medication.intakeTimes.forEach { time ->
                        put(JSONObject().apply {
                            put("hour", time.hour)
                            put("minute", time.minute)
                        })
                    }
                })
                put("daysOfWeek", JSONArray().apply {
                    medication.daysOfWeek.forEach { day -> put(day.name) }
                })
            })
        }

        directBootContext(context)
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEDICATIONS, array.toString())
            .apply()
    }

    /**
     * Loads the mirrored medication reminder data available during direct boot.
     */
    fun load(context: Context): List<Medication> {
        val raw = directBootContext(context)
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MEDICATIONS, null)
            ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toMedication())
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Removes one medication from the direct boot mirror after it was deleted.
     */
    fun remove(context: Context, medicationId: Int) {
        save(context, load(context).filterNot { it.id == medicationId })
    }

    private fun directBootContext(context: Context): Context {
        return context.applicationContext.createDeviceProtectedStorageContext()
    }

    private fun JSONObject.toMedication(): Medication {
        val times = getJSONArray("intakeTimes")
        val days = optJSONArray("daysOfWeek")
        return Medication(
            id = getInt("id"),
            name = getString("name"),
            dosage = getString("dosage"),
            notes = optString("notes"),
            lastTakenAt = if (isNull("lastTakenAt")) null else getLong("lastTakenAt"),
            intakeTimes = buildList {
                for (index in 0 until times.length()) {
                    val time = times.getJSONObject(index)
                    add(IntakeTime(time.getInt("hour"), time.getInt("minute")))
                }
            },
            daysOfWeek = if (days == null) {
                DayOfWeek.values().toSet()
            } else {
                buildSet {
                    for (index in 0 until days.length()) {
                        add(DayOfWeek.valueOf(days.getString(index)))
                    }
                }
            }
        )
    }
}
