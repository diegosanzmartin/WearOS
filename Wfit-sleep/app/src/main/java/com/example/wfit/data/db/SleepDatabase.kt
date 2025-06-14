package com.wfit.sleep.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wfit.sleep.data.db.dao.SleepCycleDao
import com.wfit.sleep.data.db.entity.SleepCycleEntity
import java.time.LocalDateTime

@Database(entities = [SleepCycleEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepCycleDao(): SleepCycleDao

    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null

        fun getDatabase(context: Context): SleepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }
} 