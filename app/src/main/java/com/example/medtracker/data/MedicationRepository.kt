package com.example.medtracker.data

import android.content.Context
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
        }.sortedWith(compareBy<Medication> { it.hour }.thenBy { it.minute }.thenBy { it.name })
    }

    fun saveAll(medications: List<Medication>) {
        val array = JSONArray()
        medications.forEach { medication -> array.put(medication.toJson()) }
        prefs.edit().putString(KEY_MEDICATIONS, array.toString()).apply()
    }

    fun add(name: String, dosage: String, hour: Int, minute: Int, notes: String) {
        val current = getAll()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        saveAll(current + Medication(nextId, name.trim(), dosage.trim(), hour, minute, notes.trim()))
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
        put("hour", hour)
        put("minute", minute)
        put("notes", notes)
        put("lastTakenAt", lastTakenAt ?: JSONObject.NULL)
    }

    private fun JSONObject.toMedication(): Medication = Medication(
        id = getInt("id"),
        name = getString("name"),
        dosage = getString("dosage"),
        hour = getInt("hour"),
        minute = getInt("minute"),
        notes = optString("notes"),
        lastTakenAt = if (isNull("lastTakenAt")) null else getLong("lastTakenAt")
    )

    companion object {
        private const val PREFS_NAME = "med_tracker_store"
        private const val KEY_MEDICATIONS = "medications"
    }
}
