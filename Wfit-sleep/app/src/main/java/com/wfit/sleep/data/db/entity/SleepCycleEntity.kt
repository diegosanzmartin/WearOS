package com.wfit.sleep.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wfit.sleep.presentation.model.SleepPhase
import java.time.LocalDateTime

@Entity(tableName = "sleep_cycles")
data class SleepCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phase: SleepPhase,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMinutes: Int = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
) 