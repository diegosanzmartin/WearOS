/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.wfit.heart.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.wfit.heart.R
import com.wfit.heart.data.HeartRateEntry
import com.wfit.heart.data.HeartRateRepository
import com.wfit.heart.service.HeartRateMonitorService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var heartRateText: TextView
    private lateinit var activityModeText: TextView
    private lateinit var dateText: TextView
    private lateinit var heartRateChart: LineChart
    private val repository = HeartRateRepository()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startHeartRateMonitoring()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupChart()
        repository.initialize(this)
        
        if (checkPermission()) {
            startHeartRateMonitoring()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

        observeHeartRateData()
    }

    private fun initializeViews() {
        heartRateText = findViewById(R.id.heartRateText)
        activityModeText = findViewById(R.id.activityModeText)
        dateText = findViewById(R.id.dateText)
        heartRateChart = findViewById(R.id.heartRateChart)
        
        updateDateText()
    }

    private fun setupChart() {
        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 200f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun startHeartRateMonitoring() {
        val serviceIntent = Intent(this, HeartRateMonitorService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun observeHeartRateData() {
        lifecycleScope.launch {
            repository.getRecentHeartRates()?.collectLatest { entries ->
                updateHeartRateDisplay(entries)
                updateChart(entries)
            }
        }
    }

    private fun updateHeartRateDisplay(entries: List<HeartRateEntry>) {
        entries.firstOrNull()?.let { latest ->
            heartRateText.text = "${latest.heartRate} bpm"
            activityModeText.text = when {
                latest.heartRate > 143 -> "High Intensity"
                latest.heartRate < 60 -> "Resting"
                else -> "Normal"
            }
        }
    }

    private fun updateChart(entries: List<HeartRateEntry>) {
        val dataEntries = entries.mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.heartRate.toFloat())
        }

        val dataSet = LineDataSet(dataEntries, "Heart Rate").apply {
            color = getColor(R.color.graph_line)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val maxThresholdDataSet = LineDataSet(
            List(entries.size) { Entry(it.toFloat(), 143f) },
            "Max Threshold"
        ).apply {
            color = getColor(R.color.max_threshold)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 1f
            enableDashedLine(10f, 5f, 0f)
        }

        val minThresholdDataSet = LineDataSet(
            List(entries.size) { Entry(it.toFloat(), 60f) },
            "Min Threshold"
        ).apply {
            color = getColor(R.color.min_threshold)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 1f
            enableDashedLine(10f, 5f, 0f)
        }

        heartRateChart.data = LineData(dataSet, maxThresholdDataSet, minThresholdDataSet)
        heartRateChart.invalidate()
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateText.text = dateFormat.format(Date())
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
}