package com.example.medtracker.model

data class IntakeTime(
    val hour: Int,
    val minute: Int
) {
    fun label(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
