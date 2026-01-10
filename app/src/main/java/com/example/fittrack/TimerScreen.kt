package com.example.fittrack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val totalTime by viewModel.totalTime.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    
    var tempInputTime by remember { mutableStateOf(totalTime.toString()) }

    LaunchedEffect(totalTime, isRunning) {
        if (!isRunning) {
            tempInputTime = totalTime.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            CircularTimer(totalTime = totalTime, remainingTime = remainingTime)
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = tempInputTime,
            onValueChange = { newTimeValue ->
                tempInputTime = newTimeValue
                newTimeValue.toLongOrNull()?.let { time ->
                    if (time != totalTime) { 
                        viewModel.setTime(time)
                    }
                }
            },
            label = { Text("휴식 시간 (초)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.6f),
            singleLine = true,
            enabled = !isRunning 
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.startTimer() }, enabled = !isRunning && totalTime > 0) {
                Text("시작")
            }
            Button(onClick = { viewModel.pauseTimer() }, enabled = isRunning) {
                Text("정지")
            }
            Button(onClick = { viewModel.resetTimer() }) {
                Text("초기화")
            }
        }
    }
}

@Composable
fun CircularTimer(totalTime: Long, remainingTime: Long) {
    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime else 0f

    // BUG FIX: Read the theme color outside the Canvas's onDraw scope
    val progressColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(250.dp)) {
            drawArc(
                color = Color.LightGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 15f, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            drawArc(
                color = progressColor, // Use the variable here
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 15f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${remainingTime / 60}:${(remainingTime % 60).toString().padStart(2, '0')}",
            fontSize = 70.sp,
            color = MaterialTheme.colorScheme.primary // This is fine
        )
    }
}
