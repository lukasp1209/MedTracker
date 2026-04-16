package com.example.medtracker.data

import android.content.Context
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import org.json.JSONArray
import org.json.JSONObject

class MedicationRepository(context: Context) {
    private val storageContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext().also {
            it.moveSharedPreferencesFrom(context, PREFS_NAME)
        }
    } else {
        context
    }
    private val prefs = storageContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Medication> {
        val raw = prefs.getString(KEY_MEDICATIONS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toMedication())
            }
        }.sortedWith(
            compareBy<Medication> { medication ->
                medication.intakeTimes.minOfOrNull { it.hour * 60 + it.minute } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )
    }

    fun saveAll(medications: List<Medication>) {
        val array = JSONArray()
        medications.forEach { medication -> array.put(medication.toJson()) }
        prefs.edit().putString(KEY_MEDICATIONS, array.toString()).apply()
    }

    fun add(name: String, dosage: String, intakeTimes: List<IntakeTime>, notes: String) {
        val current = getAll()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        saveAll(
            current + Medication(
                id = nextId,
                name = name.trim(),
                dosage = dosage.trim(),
                intakeTimes = intakeTimes.sortedWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute }),
                notes = notes.trim()
            )
        )
    }

    fun delete(id: Int) {
        saveAll(getAll().filterNot { it.id == id })
    }

    fun markTaken(id: Int, timestamp: Long = System.currentTimeMillis()) {
        saveAll(
            getAll().map { medication ->
                if (medication.id == id) medication.copy(lastTakenAt = timestamp) else medication
            }
        )
    }

    private fun Medication.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("dosage", dosage)
        put("intakeTimes", JSONArray().apply {
            intakeTimes.forEach { time ->
                put(
                    JSONObject().apply {
                        put("hour", time.hour)
                        put("minute", time.minute)
                    }
                )
            }
        })
        put("notes", notes)
        put("lastTakenAt", lastTakenAt ?: JSONObject.NULL)
    }

    private fun JSONObject.toMedication(): Medication {
        val intakeTimes = when {
            has("intakeTimes") -> {
                val array = getJSONArray("intakeTimes")
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(IntakeTime(item.getInt("hour"), item.getInt("minute")))
                    }
                }
            }
            has("hour") && has("minute") -> listOf(IntakeTime(getInt("hour"), getInt("minute")))
            else -> emptyList()
        }

        return Medication(
            id = getInt("id"),
            name = getString("name"),
            dosage = getString("dosage"),
            intakeTimes = intakeTimes.sortedWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute }),
            notes = optString("notes"),
            lastTakenAt = if (isNull("lastTakenAt")) null else getLong("lastTakenAt")
        )
    }

    companion object {
        private const val PREFS_NAME = "med_tracker_store"
        private const val KEY_MEDICATIONS = "medications"
    }
}
