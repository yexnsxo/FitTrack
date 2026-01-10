package com.example.fittrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fittrack.ui.theme.Main40
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()
    val totalTime by viewModel.totalTime.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val totalSets by viewModel.totalSets.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val setReps by viewModel.setReps.collectAsState()

    var totalWorkoutTime by remember { mutableStateOf(0L) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEditRepsDialogForSet by remember { mutableStateOf<Int?>(null) }
    var showConfirmUncheckDialogForSet by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isWorkoutStarted) {
        if (isWorkoutStarted) {
            while (true) {
                delay(1000)
                totalWorkoutTime++
            }
        }
    }

    if (showEditRepsDialogForSet != null) {
        val set = showEditRepsDialogForSet!!
        EditRepsDialog(
            setNumber = set,
            initialReps = setReps.getOrNull(set - 1) ?: 0,
            onConfirm = { newReps ->
                viewModel.setRepsForSet(set, newReps)
                showEditRepsDialogForSet = null
            },
            onDismiss = { showEditRepsDialogForSet = null }
        )
    }

    if (showConfirmUncheckDialogForSet != null) {
        val set = showConfirmUncheckDialogForSet!!
        ConfirmUncheckDialog(
            setNumber = set,
            onConfirm = {
                viewModel.resetToSet(set)
                showConfirmUncheckDialogForSet = null
            },
            onDismiss = { showConfirmUncheckDialogForSet = null }
        )
    }
    if (showSettingsDialog) {
        SettingsDialog(
            totalSets = totalSets,
            restTime = totalTime,
            onTotalSetsChange = { viewModel.setTotalSets(it) },
            onRestTimeChange = { viewModel.setRestTime(it) },
            onDismiss = { showSettingsDialog = false },
            onReset = { viewModel.resetWorkout() }
        )
    }

    Scaffold(
        bottomBar = {
            if (isTimerRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("휴식", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${remainingTime / 60}:${(remainingTime % 60).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { viewModel.stopRest() }) {
                            Text("건너뛰기")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (!isWorkoutStarted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { viewModel.startWorkout() }) {
                    Text("운동 시작", style = MaterialTheme.typography.headlineMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("총 운동 시간", style = MaterialTheme.typography.titleMedium)
                            val hours = totalWorkoutTime / 3600
                            val minutes = (totalWorkoutTime % 3600) / 60
                            val seconds = totalWorkoutTime % 60
                            Text(
                                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Main40),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("세트 체크리스트", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "운동 설정", tint = Color.White)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..totalSets).forEach { set ->
                                    val isCompleted = set < currentSet
                                    val isCurrent = set == currentSet
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when {
                                                    isCompleted -> showConfirmUncheckDialogForSet = set
                                                    isCurrent -> viewModel.finishSet()
                                                    else -> showEditRepsDialogForSet = set
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = if (isCompleted) "완료" else "미완료",
                                            tint = Color.White
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "$set 세트" + (setReps.getOrNull(set - 1)?.let { if (it > 0) " - $it 회" else "" } ?: ""),
                                            color = Color.White,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.finishSet() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("세트 완료", color = Main40)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRepsDialog(
    setNumber: Int,
    initialReps: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember { mutableStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "$setNumber 세트 횟수 수정") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = reps.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { reps = (reps - 5).coerceAtLeast(0) }) {
                        Text("-5")
                    }
                    IconButton(onClick = { reps = (reps - 1).coerceAtLeast(0) }) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "1 감소")
                    }
                    IconButton(onClick = { reps++ }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "1 증가")
                    }
                    Button(onClick = { reps += 5 }) {
                        Text("+5")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reps) }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ConfirmUncheckDialog(
    setNumber: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "세트 초기화") },
        text = { Text("$setNumber 세트로 운동을 되돌릴까요?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    totalSets: Int,
    restTime: Long,
    onTotalSetsChange: (Int) -> Unit,
    onRestTimeChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    var tempRestTime by remember(restTime) { mutableStateOf(restTime.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("운동 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("총 세트 수", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onTotalSetsChange(totalSets - 1) }) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "세트 감소")
                        }
                        Text(text = "$totalSets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onTotalSetsChange(totalSets + 1) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "세트 증가")
                        }
                    }
                }
                OutlinedTextField(
                    value = tempRestTime,
                    onValueChange = {
                        tempRestTime = it
                        it.toLongOrNull()?.let { time -> onRestTimeChange(time) }
                    },
                    label = { Text("휴식 시간 (초)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("운동 초기화")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}