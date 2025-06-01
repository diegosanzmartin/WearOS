package com.example.wfit.presentation.model

import java.time.LocalDateTime

enum class SleepPhase {
    AWAKE,
    LIGHT_SLEEP,
    DEEP_SLEEP,
    REM
}

data class SleepCycle(
    val phase: SleepPhase,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMinutes: Int = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
)

data class DailySleepData(
    val date: LocalDateTime,
    val cycles: List<SleepCycle>,
    val totalSleepTime: Int = cycles.filter { it.phase != SleepPhase.AWAKE }.sumOf { it.durationMinutes },
    val sleepScore: Int = calculateSleepScore(cycles)
) {
    companion object {
        private fun calculateSleepScore(cycles: List<SleepCycle>): Int {
            // Implementación básica del cálculo del score
            val totalMinutes = cycles.filter { it.phase != SleepPhase.AWAKE }.sumOf { it.durationMinutes }
            val deepSleepMinutes = cycles.filter { it.phase == SleepPhase.DEEP_SLEEP }
                .sumOf { it.durationMinutes }
            val remSleepMinutes = cycles.filter { it.phase == SleepPhase.REM }
                .sumOf { it.durationMinutes }
            
            // Score basado en la proporción ideal de sueño profundo (20-25%) y REM (20-25%)
            val deepSleepScore = if (totalMinutes > 0) {
                (deepSleepMinutes.toFloat() / totalMinutes * 100).coerceIn(0f, 25f)
            } else 0f
            val remSleepScore = if (totalMinutes > 0) {
                (remSleepMinutes.toFloat() / totalMinutes * 100).coerceIn(0f, 25f)
            } else 0f
            
            return ((deepSleepScore + remSleepScore) * 2).toInt()
        }
    }
} 