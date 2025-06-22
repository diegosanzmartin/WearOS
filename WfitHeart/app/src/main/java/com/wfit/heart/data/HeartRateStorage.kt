package com.wfit.heart.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HeartRateStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    fun saveMeasurements(measurements: List<HeartRateMeasurement>) {
        val measurementsData = measurements.map { measurement ->
            MeasurementData(
                value = measurement.value,
                time = measurement.time.format(timeFormatter)
            )
        }
        val json = gson.toJson(measurementsData)
        prefs.edit().putString(KEY_MEASUREMENTS, json).apply()
    }

    fun loadMeasurements(): List<HeartRateMeasurement> {
        val json = prefs.getString(KEY_MEASUREMENTS, null) ?: return emptyList()
        val type = object : TypeToken<List<MeasurementData>>() {}.type
        return try {
            val measurementsData: List<MeasurementData> = gson.fromJson(json, type)
            measurementsData.map { data ->
                HeartRateMeasurement(
                    value = data.value,
                    time = LocalTime.parse(data.time, timeFormatter)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private data class MeasurementData(
        val value: Int,
        val time: String
    )

    companion object {
        private const val PREFS_NAME = "heart_rate_prefs"
        private const val KEY_MEASUREMENTS = "measurements"
    }
} 