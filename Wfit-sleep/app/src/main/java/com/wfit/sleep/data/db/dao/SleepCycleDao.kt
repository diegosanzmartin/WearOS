package com.wfit.sleep.data.db.dao

import androidx.room.*
import com.wfit.sleep.data.db.entity.SleepCycleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SleepCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleepCycle: SleepCycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sleepCycles: List<SleepCycleEntity>)

    @Query("SELECT * FROM sleep_cycles WHERE startTime >= :startDate AND startTime < :endDate ORDER BY startTime ASC")
    fun getSleepCyclesBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<SleepCycleEntity>>

    @Query("SELECT * FROM sleep_cycles WHERE startTime >= :date ORDER BY startTime ASC LIMIT 1")
    suspend fun getFirstSleepCycleAfter(date: LocalDateTime): SleepCycleEntity?

    @Query("SELECT * FROM sleep_cycles WHERE endTime <= :date ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastSleepCycleBefore(date: LocalDateTime): SleepCycleEntity?

    @Query("DELETE FROM sleep_cycles WHERE startTime < :date")
    suspend fun deleteSleepCyclesBefore(date: LocalDateTime)

    @Query("DELETE FROM sleep_cycles WHERE startTime >= :startDate AND startTime < :endDate")
    suspend fun deleteSleepCyclesBetween(startDate: LocalDateTime, endDate: LocalDateTime)
} 