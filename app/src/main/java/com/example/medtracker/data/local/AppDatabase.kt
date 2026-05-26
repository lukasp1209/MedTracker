package com.example.medtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.medtracker.model.IntakeTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Provides Room type converters for values that cannot be stored directly in SQLite.
 */
class Converters {
    /**
     * Converts a list of intake times into a JSON string for database storage.
     */
    @TypeConverter
    fun fromIntakeTimeList(value: List<IntakeTime>): String {
        val array = JSONArray()
        value.forEach { time ->
            array.put(JSONObject().apply {
                put("hour", time.hour)
                put("minute", time.minute)
            })
        }
        return array.toString()
    }

    /**
     * Restores a list of intake times from the JSON string stored in the database.
     */
    @TypeConverter
    fun toIntakeTimeList(value: String): List<IntakeTime> {
        val array = JSONArray(value)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(IntakeTime(obj.getInt("hour"), obj.getInt("minute")))
            }
        }
    }

    /**
     * Converts selected weekdays into a comma-separated list of enum names.
     */
    @TypeConverter
    fun fromDayOfWeekSet(value: Set<java.time.DayOfWeek>): String {
        return value.joinToString(",") { it.name }
    }

    /**
     * Restores selected weekdays from their comma-separated database representation.
     */
    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<java.time.DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { java.time.DayOfWeek.valueOf(it) }.toSet()
    }
}

/**
 * Main Room database for medications and intake history entries.
 */
@Database(entities = [MedicationEntity::class, IntakeHistoryEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Gives access to all medication and history database operations.
     */
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the single database instance used by the whole application.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medtracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
