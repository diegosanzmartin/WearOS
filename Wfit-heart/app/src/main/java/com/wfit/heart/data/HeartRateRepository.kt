package com.wfit.heart.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Entity(tableName = "heart_rates")
data class HeartRateEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val heartRate: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHeartRates(): Flow<List<HeartRateEntry>>

    @Insert
    suspend fun insert(entry: HeartRateEntry)

    @Query("DELETE FROM heart_rates WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

@Database(entities = [HeartRateEntry::class], version = 1)
abstract class HeartRateDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
}

class HeartRateRepository {
    private var database: HeartRateDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                HeartRateDatabase::class.java,
                "heart_rate_database"
            ).build()
        }
    }

    suspend fun saveHeartRate(heartRate: Int) {
        database?.heartRateDao()?.let { dao ->
            dao.insert(HeartRateEntry(heartRate = heartRate))
            
            // Delete entries older than 24 hours
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            dao.deleteOlderThan(oneDayAgo)
        }
    }

    fun getRecentHeartRates(): Flow<List<HeartRateEntry>>? {
        return database?.heartRateDao()?.getRecentHeartRates()
    }
} 