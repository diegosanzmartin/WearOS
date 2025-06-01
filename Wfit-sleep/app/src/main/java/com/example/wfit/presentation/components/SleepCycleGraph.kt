package com.example.wfit.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.graphics.Typeface
import com.example.wfit.presentation.model.DailySleepData
import com.example.wfit.presentation.model.SleepPhase
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun SleepCycleGraph(
    sleepData: DailySleepData,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height - 20f // Reservar espacio para las etiquetas

        // Configurar el Paint para las etiquetas de tiempo
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            alpha = 180
            textSize = 35f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Colores para cada fase del sueño con efecto retro
        val phaseColors = mapOf(
            SleepPhase.AWAKE to Color(0xFFFF6B6B).copy(alpha = 0.4f),      // Rojo neón
            SleepPhase.LIGHT_SLEEP to Color(0xFF4ECDC4).copy(alpha = 0.4f), // Turquesa retro
            SleepPhase.DEEP_SLEEP to Color(0xFF45B7D1).copy(alpha = 0.4f),  // Azul retro
            SleepPhase.REM to Color(0xFFBA68C8).copy(alpha = 0.4f)          // Morado retro
        )

        // Colores para los bordes con efecto neón
        val strokeColors = mapOf(
            SleepPhase.AWAKE to Color(0xFFFF6B6B).copy(alpha = 0.8f),
            SleepPhase.LIGHT_SLEEP to Color(0xFF4ECDC4).copy(alpha = 0.8f),
            SleepPhase.DEEP_SLEEP to Color(0xFF45B7D1).copy(alpha = 0.8f),
            SleepPhase.REM to Color(0xFFBA68C8).copy(alpha = 0.8f)
        )

        // Dibujar grid retro (líneas de fondo)
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * i / gridLines
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        if (sleepData.cycles.isNotEmpty()) {
            // Calcular el tiempo total en minutos
            val totalMinutes = ChronoUnit.MINUTES.between(
                sleepData.cycles.first().startTime,
                sleepData.cycles.last().endTime
            ).toFloat()

            // Log para verificar los ciclos
            android.util.Log.d("SleepCycleGraph", "Dibujando ciclos:")
            sleepData.cycles.forEach { cycle ->
                android.util.Log.d("SleepCycleGraph", "Ciclo: ${cycle.phase} de ${cycle.startTime} a ${cycle.endTime} (${cycle.durationMinutes} minutos)")
            }

            var currentX = 0f
            sleepData.cycles.forEach { cycle ->
                val cycleWidth = (width * cycle.durationMinutes / totalMinutes)
                val y = when (cycle.phase) {
                    SleepPhase.AWAKE -> height * 0.2f
                    SleepPhase.LIGHT_SLEEP -> height * 0.4f
                    SleepPhase.DEEP_SLEEP -> height * 0.8f
                    SleepPhase.REM -> height * 0.6f
                }

                // Log para verificar el cálculo de dimensiones
                android.util.Log.d("SleepCycleGraph", "Dibujando ciclo ${cycle.phase}: x=$currentX, width=$cycleWidth, y=$y")

                // Crear un path para el área de cada fase
                val phasePath = Path().apply {
                    moveTo(currentX, height)
                    lineTo(currentX, y)
                    lineTo(currentX + cycleWidth, y)
                    lineTo(currentX + cycleWidth, height)
                    close()
                }

                // Dibujar el área rellena con efecto retro
                drawPath(
                    path = phasePath,
                    color = phaseColors[cycle.phase] ?: Color.Gray
                )

                // Dibujar el borde con efecto neón
                drawPath(
                    path = phasePath,
                    color = strokeColors[cycle.phase] ?: Color.White,
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round
                    )
                )

                // Dibujar línea horizontal más gruesa para cada fase
                drawLine(
                    color = strokeColors[cycle.phase] ?: Color.White,
                    start = Offset(currentX, y),
                    end = Offset(currentX + cycleWidth, y),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                currentX += cycleWidth
            }

            // Dibujar línea base con efecto neón
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Formatear y dibujar las etiquetas de tiempo
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val startTime = sleepData.cycles.first().startTime.format(timeFormatter)
            val endTime = sleepData.cycles.last().endTime.format(timeFormatter)

            drawIntoCanvas { canvas ->
                // Dibujar hora de inicio
                canvas.nativeCanvas.drawText(
                    startTime,
                    0f,
                    height + 35f, // Posición Y debajo del gráfico
                    textPaint
                )

                // Dibujar hora de fin
                val endTimeWidth = textPaint.measureText(endTime)
                canvas.nativeCanvas.drawText(
                    endTime,
                    width - endTimeWidth,
                    height + 35f, // Posición Y debajo del gráfico
                    textPaint
                )
            }
        } else {
            // Dibujar línea base con efecto neón
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Dibujar mensaje de no datos
            drawIntoCanvas { canvas ->
                val noDataText = "No hay datos"
                val textWidth = textPaint.measureText(noDataText)
                canvas.nativeCanvas.drawText(
                    noDataText,
                    (width - textWidth) / 2,
                    height / 2,
                    textPaint
                )
            }
        }
    }
} 