package com.wfit.heart.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HeartRateStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = createGson()
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java,
                object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
                    override fun serialize(
                        src: LocalDateTime,
                        typeOfSrc: Type,
                        context: JsonSerializationContext
                    ): JsonElement {
                        return JsonPrimitive(dateTimeFormatter.format(src))
                    }

                    override fun deserialize(
                        json: JsonElement,
                        typeOfT: Type,
                        context: JsonDeserializationContext
                    ): LocalDateTime {
                        return LocalDateTime.parse(json.asString, dateTimeFormatter)
                    }
                })
            .create()
    }

    fun saveMeasurements(measurements: List<HeartRateMeasurement>) {
        val measurementsData = measurements.map { measurement ->
            MeasurementData(
                value = measurement.value,
                timestamp = measurement.timestamp
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
                    timestamp = data.timestamp
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private data class MeasurementData(
        val value: Int,
        val timestamp: LocalDateTime
    )

    companion object {
        private const val PREFS_NAME = "heart_rate_prefs"
        private const val KEY_MEASUREMENTS = "measurements"
    }
} 