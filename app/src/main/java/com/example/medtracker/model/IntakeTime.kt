package com.example.medtracker.model

/**
 * Represents one daily time at which a medication should be taken.
 */
data class IntakeTime(
    val hour: Int,
    val minute: Int
) {
    /**
     * Formats the time as a two-digit 24-hour label, for example 08:05.
     */
    fun label(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
