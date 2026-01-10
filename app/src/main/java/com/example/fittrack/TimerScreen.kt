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
import androidx.compose.material.icons.filled.CheckCircle
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

    var totalWorkoutTime by remember { mutableStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEditRepsDialogForSet by remember { mutableStateOf<Int?>(null) }
    var showConfirmUncheckDialogForSet by remember { mutableStateOf<Int?>(null) }

    val totalReps = setReps.sum()

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
            initialReps = setReps.getOrNull(set - 1) ?: 10,
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
            onReset = {
                viewModel.resetWorkout()
                totalWorkoutTime = 0
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (isTimerRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                    if (isWorkoutStarted) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("총 횟수", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "$totalReps",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { viewModel.startWorkout() }) {
                                Text("운동 시작", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
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
                                        .padding(vertical = 4.dp)
                                ) {
                                    IconButton(onClick = {
                                        if (isCompleted) {
                                            showConfirmUncheckDialogForSet = set
                                        } else if (isWorkoutStarted) {
                                            viewModel.finishSet()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = if (isCompleted) "완료" else "미완료",
                                            tint = Color.White
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "$set 세트" + (setReps.getOrNull(set - 1)?.let { " - $it 회" } ?: ""),
                                        color = Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.clickable {
                                            showEditRepsDialogForSet = set
                                        }
                                    )
                                }
                            }
                        }
                        if (isWorkoutStarted) {
                            if (currentSet <= totalSets) {
                                Button(
                                    onClick = { viewModel.finishSet() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Text("세트 완료", color = Main40)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.resetWorkout()
                                    totalWorkoutTime = 0
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("운동 완료", color = Color.White)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { reps -= 5 }) { Text("-5") }
                    Button(onClick = { reps -= 1 }) { Text("-1") }
                    Button(onClick = { reps += 1 }) { Text("+1") }
                    Button(onClick = { reps += 5 }) { Text("+5") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reps) }) {
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
fun SettingsDialog(
    totalSets: Int,
    restTime: Int,
    onTotalSetsChange: (Int) -> Unit,
    onRestTimeChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    var sets by remember { mutableStateOf(totalSets.toString()) }
    var time by remember { mutableStateOf(restTime.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("운동 설정") },
        text = {
            Column {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("총 세트 수") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("휴식 시간 (초)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTotalSetsChange(sets.toIntOrNull() ?: totalSets)
                    onRestTimeChange(time.toIntOrNull() ?: restTime)
                    onDismiss()
                }
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
fun ConfirmUncheckDialog(
    setNumber: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "세트 체크 해제") },
        text = { Text(text = "${setNumber}세트로 돌아가시겠습니까? 현재 세트 이후의 모든 진행 상황이 초기화됩니다.") },
        confirmButton = {
            Button(
                onClick = onConfirm
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
