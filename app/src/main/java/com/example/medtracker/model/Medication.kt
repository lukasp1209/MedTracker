package com.example.medtracker.model

data class IntakeTime(
    val hour: Int,
    val minute: Int
) {
    fun label(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

data class Medication(
    val id: Int,
    val name: String,
    val dosage: String,
    val intakeTimes: List<IntakeTime>,
    val notes: String = "",
    val lastTakenAt: Long? = null
)
