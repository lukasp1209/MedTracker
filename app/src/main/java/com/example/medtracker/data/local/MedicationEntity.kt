package com.example.medtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medtracker.model.IntakeTime

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val notes: String,
    val lastTakenAt: Long?,
    val intakeTimes: List<IntakeTime>,
    val daysOfWeek: Set<java.time.DayOfWeek>
)

@Entity(tableName = "intake_history")
data class IntakeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val takenAt: Long = System.currentTimeMillis(),
    val status: String = "TAKEN" // TAKEN, SNOOZED, SKIPPED
)
