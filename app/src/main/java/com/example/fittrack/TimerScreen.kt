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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isTimerRunning) {
            RestTimerView(viewModel = viewModel)
        } else {
            SetCounterView(viewModel = viewModel)
        }
    }
}

@Composable
fun RestTimerView(viewModel: TimerViewModel) {
    val totalTime by viewModel.totalTime.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("휴식 시간", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            CircularTimer(totalTime = totalTime, remainingTime = remainingTime)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { viewModel.stopRest() }) {
            Text("휴식 건너뛰기")
        }
    }
}

@Composable
fun SetCounterView(viewModel: TimerViewModel) {
    val totalSets by viewModel.totalSets.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val setReps by viewModel.setReps.collectAsState()
    val totalTime by viewModel.totalTime.collectAsState()
    var tempInputTime by remember { mutableStateOf(totalTime.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 총 세트 수 설정
            Text("총 세트 수", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { viewModel.setTotalSets(totalSets - 1) }) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "세트 감소")
                }
                Text(text = "$totalSets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.setTotalSets(totalSets + 1) }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "세트 증가")
                }
            }
            
            // 휴식 시간 설정
            OutlinedTextField(
                value = tempInputTime,
                onValueChange = { 
                    tempInputTime = it
                    it.toLongOrNull()?.let { time -> viewModel.setRestTime(time) }
                },
                label = { Text("휴식 시간 (초)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(0.8f),
                singleLine = true
            )

            // 세트별 횟수 표시
            Text("세트별 횟수", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(setReps) { index, repCount ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${index + 1}세트", style = MaterialTheme.typography.bodySmall)
                        Text("$repCount", style = MaterialTheme.typography.bodyLarge, fontWeight = if (index + 1 == currentSet) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // 현재 세트 및 횟수
            Text("$currentSet / $totalSets 세트", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            
            Text("횟수", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.decrementRepsByFive() }) {
                    Text("-5")
                }
                IconButton(onClick = { viewModel.decrementReps() }) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "횟수 1 감소")
                }
                Text(text = "$reps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { viewModel.incrementReps() }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "횟수 1 증가")
                }
                Button(onClick = { viewModel.incrementRepsByFive() }) {
                    Text("+5")
                }
            }

            // 세트 완료 버튼
            Button(onClick = { viewModel.finishSet() }, modifier = Modifier.fillMaxWidth()) {
                Text("세트 완료")
            }
            
            // 전체 운동 초기화 버튼
            Button(onClick = { viewModel.resetWorkout() }, modifier = Modifier.fillMaxWidth()) {
                Text("운동 초기화")
            }
        }
    }
}

@Composable
fun CircularTimer(totalTime: Long, remainingTime: Long) {
    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime else 0f
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
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 15f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${remainingTime / 60}:${(remainingTime % 60).toString().padStart(2, '0')}",
            fontSize = 70.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
