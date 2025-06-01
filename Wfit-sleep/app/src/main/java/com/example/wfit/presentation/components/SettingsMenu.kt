package com.example.wfit.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SettingsMenu(
    isTrackingEnabled: Boolean,
    onTrackingToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSleepCycleSettings by remember { mutableStateOf(false) }

    if (showSleepCycleSettings) {
        SleepCycleScreen(
            onConfirm = { startTime, endTime ->
                // Handle the confirmed time range, e.g., save to ViewModel or SharedPreferences
                println("Sleep cycle confirmed: $startTime to $endTime") // Placeholder action
                showSleepCycleSettings = false
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Non-tracking time switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Non-tracking time",
                fontSize = 16.sp,
                color = Color.White
            )
            Switch(
                checked = isTrackingEnabled,
                onCheckedChange = { onTrackingToggle() }
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .padding(vertical = 8.dp)
        )

        // Sleep Cycle setting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showSleepCycleSettings = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Cycle",
                fontSize = 16.sp,
                color = Color.White
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .padding(vertical = 8.dp)
        )

        // Version info
        Text(
            text = "Version 1.0.0",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
} 