package com.wfit.sleep.presentation.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

@Composable
fun SleepCycleScreen(
    initialStartTime: LocalTime = LocalTime.of(23, 0),
    initialEndTime: LocalTime = LocalTime.of(8, 0),
    onConfirm: (LocalTime, LocalTime) -> Unit
) {
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }

    val totalMinutesInDay = 24 * 60
    val startAngleOffset = -90f // Start at the top (12 o'clock)

    fun angleToTime(angle: Float): LocalTime {
        val normalizedAngle = (angle - startAngleOffset + 360) % 360
        val minutes = (normalizedAngle / 360f * totalMinutesInDay).roundToInt()
        return LocalTime.ofSecondOfDay(minutes * 60L)
    }

    fun timeToAngle(time: LocalTime): Float {
        val minutes = time.hour * 60 + time.minute
        return (minutes.toFloat() / totalMinutesInDay * 360f + startAngleOffset) % 360
    }

    var startHandleAngle by remember { mutableStateOf(timeToAngle(startTime)) }
    var endHandleAngle by remember { mutableStateOf(timeToAngle(endTime)) }

    val selectedDurationHours = calculateDuration(startTime, endTime)

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularSlider(
                startAngle = startHandleAngle,
                endAngle = endHandleAngle,
                onStartAngleChange = { newAngle ->
                    startHandleAngle = newAngle
                    startTime = angleToTime(newAngle)
                },
                onEndAngleChange = { newAngle ->
                    endHandleAngle = newAngle
                    endTime = angleToTime(newAngle)
                },
                modifier = Modifier.align(Alignment.Center).fillMaxSize(0.8f)
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${selectedDurationHours}h 0m",
                    style = MaterialTheme.typography.title1.copy(fontSize = 24.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { onConfirm(startTime, endTime) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(ButtonDefaults.LargeButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Confirm",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CircularSlider(
    startAngle: Float,
    endAngle: Float,
    onStartAngleChange: (Float) -> Unit,
    onEndAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 20f,
    handleRadius: Float = 20f
) {
    val darkerArcColor = Color(0xFF303F9F) // Dark blue
    val lighterArcColor = Color(0xFFADD8E6) // Light blue

    var draggingStart by remember { mutableStateOf(false) }
    var draggingEnd by remember { mutableStateOf(false) }

    Canvas(modifier = modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val angle = atan2(offset.y - centerY, offset.x - centerX) * (180f / PI.toFloat())
                    val startHandlePos = Offset(
                        centerX + (size.width / 2 - strokeWidth / 2) * cos(Math.toRadians(startAngle.toDouble())).toFloat(),
                        centerY + (size.height / 2 - strokeWidth / 2) * sin(Math.toRadians(startAngle.toDouble())).toFloat()
                    )
                    val endHandlePos = Offset(
                        centerX + (size.width / 2 - strokeWidth / 2) * cos(Math.toRadians(endAngle.toDouble())).toFloat(),
                        centerY + (size.height / 2 - strokeWidth / 2) * sin(Math.toRadians(endAngle.toDouble())).toFloat()
                    )

                    val distToStart = (offset - startHandlePos).getDistance()
                    val distToEnd = (offset - endHandlePos).getDistance()

                    if (distToStart < handleRadius * 2) { // Increased touch target
                        draggingStart = true
                        draggingEnd = false
                    } else if (distToEnd < handleRadius * 2) { // Increased touch target
                        draggingEnd = true
                        draggingStart = false
                    } else {
                        draggingStart = false
                        draggingEnd = false
                    }
                },
                onDrag = { change, _ ->
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val angle = (atan2(change.position.y - centerY, change.position.x - centerX) * (180f / PI.toFloat()) + 360) % 360

                    if (draggingStart) {
                        onStartAngleChange(angle)
                    } else if (draggingEnd) {
                        onEndAngleChange(angle)
                    }
                    change.consume()
                },
                onDragEnd = {
                    draggingStart = false
                    draggingEnd = false
                }
            )
        }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - strokeWidth / 2

        // Draw the full 24h circle background (optional, if needed)
        drawCircle(
            color = Color.DarkGray.copy(alpha = 0.5f),
            radius = radius + strokeWidth / 2, // Outer edge of the track
            center = center,
            style = Stroke(width = strokeWidth / 2) // Thin track
        )


        // Draw the selected arc
        val sweepAngle = (endAngle - startAngle + 360) % 360
        drawArc(
            color = lighterArcColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Draw the unselected part of the arc (darker blue)
         val unselectedSweepAngle = 360 - sweepAngle
         if (unselectedSweepAngle > 0.1f) { // Avoid drawing if full circle
            drawArc(
                color = darkerArcColor,
                startAngle = endAngle, // Start where the lighter arc ends
                sweepAngle = unselectedSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }


        // Draw start handle
        val startHandleCenter = Offset(
            center.x + radius * cos(Math.toRadians(startAngle.toDouble())).toFloat(),
            center.y + radius * sin(Math.toRadians(startAngle.toDouble())).toFloat()
        )
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = startHandleCenter
        )
        drawCircle( // Border for the handle
            color = Color.Black,
            radius = handleRadius,
            center = startHandleCenter,
            style = Stroke(width = 2.dp.toPx())
        )


        // Draw end handle
        val endHandleCenter = Offset(
            center.x + radius * cos(Math.toRadians(endAngle.toDouble())).toFloat(),
            center.y + radius * sin(Math.toRadians(endAngle.toDouble())).toFloat()
        )
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = endHandleCenter
        )
        drawCircle( // Border for the handle
            color = Color.Black,
            radius = handleRadius,
            center = endHandleCenter,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw hour markers
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = android.graphics.Color.LTGRAY
            textAlign = Paint.Align.CENTER
        }
        
        for (hour in 0 until 24) {
            val angleRad = Math.toRadians((hour.toFloat() / 24f * 360f - 90f).toDouble())
            val markerRadius = radius + strokeWidth * 0.8f
            val x = center.x + markerRadius * cos(angleRad).toFloat()
            val y = center.y + markerRadius * sin(angleRad).toFloat()
            val textY = y + textPaint.textSize / 3

            // Draw a small tick
            val tickStartRadius = radius + strokeWidth /2
            val tickEndRadius = radius + strokeWidth /2 + if (hour % 3 == 0) 8.dp.toPx() else 4.dp.toPx()
            val tickStartX = center.x + tickStartRadius * cos(angleRad).toFloat()
            val tickStartY = center.y + tickStartRadius * sin(angleRad).toFloat()
            val tickEndX = center.x + tickEndRadius * cos(angleRad).toFloat()
            val tickEndY = center.y + tickEndRadius * sin(angleRad).toFloat()

            drawLine(
                color = Color.LightGray,
                start = Offset(tickStartX, tickStartY),
                end = Offset(tickEndX, tickEndY),
                strokeWidth = if (hour % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
            )

            // Draw hour text for 0, 6, 12, 18
            if (hour % 6 == 0) {
                val textRadius = radius + strokeWidth * 1.5f
                val textX = center.x + textRadius * cos(angleRad).toFloat()
                val textTextY = center.y + textRadius * sin(angleRad).toFloat() + textPaint.textSize / 3
                drawContext.canvas.nativeCanvas.drawText(
                    hour.toString(),
                    textX,
                    textTextY,
                    textPaint
                )
            }
        }
    }
}

private fun calculateDuration(start: LocalTime, end: LocalTime): Int {
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute
    return if (endMinutes >= startMinutes) {
        (endMinutes - startMinutes) / 60
    } else {
        (totalMinutesInDay - startMinutes + endMinutes) / 60
    }
}

private const val totalMinutesInDay = 24 * 60

// Preview for Android Studio
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreviewSleepCycleScreen() {
    MaterialTheme {
        SleepCycleScreen(
            initialStartTime = LocalTime.of(22, 0),
            initialEndTime = LocalTime.of(7, 0),
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun DefaultPreviewSleepCycleScreenLarge() {
    MaterialTheme {
        SleepCycleScreen(
            initialStartTime = LocalTime.of(23,30),
            initialEndTime = LocalTime.of(8,30),
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(device = Devices.WEAR_OS_SQUARE, showSystemUi = true)
@Composable
fun DefaultPreviewSleepCycleScreenSquare() {
    MaterialTheme {
        SleepCycleScreen(
                initialStartTime = LocalTime.NOON,
                initialEndTime = LocalTime.MIDNIGHT,
                onConfirm = { _, _ -> }
        )
    }
} 