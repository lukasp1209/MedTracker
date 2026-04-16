package com.example.medtracker.model

data class Medication(
    val id: Int,
    val name: String,
    val dosage: String,
    val hour: Int,
    val minute: Int,
    val notes: String = "",
    val lastTakenAt: Long? = null
)
