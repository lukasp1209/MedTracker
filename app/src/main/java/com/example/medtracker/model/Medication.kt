package com.example.medtracker.model

import java.time.DayOfWeek

/**
 * Domain model used by the app UI and reminder logic.
 */
data class Medication(
    val id: Int,
    val name: String,
    val dosage: String,
    val intakeTimes: List<IntakeTime>,
    val notes: String = "",
    val lastTakenAt: Long? = null,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet() // Standard: Jeden Tag
)
