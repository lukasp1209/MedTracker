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

class Converters {
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

    @TypeConverter
    fun fromDayOfWeekSet(value: Set<java.time.DayOfWeek>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<java.time.DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { java.time.DayOfWeek.valueOf(it) }.toSet()
    }
}

@Database(entities = [MedicationEntity::class, IntakeHistoryEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
