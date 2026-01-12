package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fittrack.ui.theme.Main40
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(viewModel: TimerViewModel = viewModel()) {

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEditRepsDialogForSet by remember { mutableStateOf<Int?>(null) }
    var showConfirmUncheckDialogForSet by remember { mutableStateOf<Int?>(null) }

    Scaffold {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RepsModeScreen(
                    viewModel = viewModel,
                    onEditRepsClick = { set -> showEditRepsDialogForSet = set },
                    onUncheckSetClick = { set -> showConfirmUncheckDialogForSet = set },
                    onSettingsClick = { showSettingsDialog = true }
                )
            }
        }
    }

    if (showSettingsDialog) {
        val totalSets by viewModel.totalSets.collectAsState()
        val restTime by viewModel.totalTime.collectAsState() // totalTime is restTime in old VM
        SettingsDialog(
            totalSets = totalSets,
            restTime = restTime,
            onTotalSetsChange = { viewModel.setTotalSets(it) },
            onRestTimeChange = { viewModel.setRestTime(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    showEditRepsDialogForSet?.let { set ->
        val setReps by viewModel.setReps.collectAsState()
        val totalSets by viewModel.totalSets.collectAsState()
        EditRepsDialog(
            setNumber = set,
            initialReps = setReps.getOrNull(set - 1) ?: 0,
            onConfirm = { newReps ->
                viewModel.setRepsForSet(set, newReps)
            },
            onConfirmAll = { newReps ->
                for (i in set..totalSets) {
                    viewModel.setRepsForSet(i, newReps)
                }
            },
            onDismiss = { showEditRepsDialogForSet = null }
        )
    }

    showConfirmUncheckDialogForSet?.let { set ->
        ConfirmUncheckDialog(
            setNumber = set,
            onConfirm = {
                viewModel.resetToSet(set)
                showConfirmUncheckDialogForSet = null
            },
            onDismiss = { showConfirmUncheckDialogForSet = null }
        )
    }
}

@Composable
fun RepsModeScreen(
    viewModel: TimerViewModel,
    onEditRepsClick: (Int) -> Unit,
    onUncheckSetClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isWorkoutStarted) {
            StatsCards(viewModel)
        } else {
            Button(
                onClick = { viewModel.startWorkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Main40)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("운동 시작", style = MaterialTheme.typography.titleLarge)
            }
        }

        SetChecklist(viewModel, onEditRepsClick, onUncheckSetClick, onSettingsClick)

        val isResting by viewModel.isTimerRunning.collectAsState()
        if (isResting) {
            RestTimerBar(viewModel)
        }

        if (isWorkoutStarted) {
            val currentSet by viewModel.currentSet.collectAsState()
            val totalSets by viewModel.totalSets.collectAsState()
            val isFinished = currentSet > totalSets

            Button(
                onClick = { viewModel.finishSet() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !isFinished,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28a745)) // Green color
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("세트 완료", style = MaterialTheme.typography.titleLarge)
            }

            if (isFinished) {
                Button(
                    onClick = { viewModel.resetWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Main40)
                ) {
                    Text("운동 완료", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.resetWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("운동 완료", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun StatsCards(viewModel: TimerViewModel) {
    val totalSets by viewModel.totalSets.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val setReps by viewModel.setReps.collectAsState()
    var totalWorkoutTime by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { // Simplified timer for display
        while (true) {
            delay(1000)
            totalWorkoutTime++
        }
    }

    val completedSets = (currentSet - 1).coerceAtLeast(0)
    val totalReps = setReps.take(completedSets).sum()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(title = "소요 시간", value = formatTime(totalWorkoutTime), modifier = Modifier.weight(1f))
        StatCard(title = "완료 세트", value = "$completedSets/$totalSets", modifier = Modifier.weight(1f), isPrimary = true)
        StatCard(title = "총 횟수", value = totalReps.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, isPrimary: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) Main40 else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SetChecklist(
    viewModel: TimerViewModel,
    onEditRepsClick: (Int) -> Unit,
    onUncheckSetClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val totalSets by viewModel.totalSets.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val setReps by viewModel.setReps.collectAsState()
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "설정")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..totalSets).forEach { setNumber ->
                    val isCompleted = setNumber < currentSet
                    val isCurrent = setNumber == currentSet
                    SetItem(
                        index = setNumber - 1,
                        reps = setReps.getOrNull(setNumber - 1) ?: 0,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent && isWorkoutStarted,
                        isActive = isWorkoutStarted,
                        onCompleteClick = { if (isCurrent) viewModel.finishSet() else onUncheckSetClick(setNumber) },
                        onItemClick = { onEditRepsClick(setNumber) }
                    )
                }
            }
        }
    }
}

@Composable
fun SetItem(
    index: Int,
    reps: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isActive: Boolean,
    onCompleteClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFFE6F4EA) // light green
        isCurrent -> Main40.copy(alpha = 0.1f)
        else -> Color(0xFFF3F3F3)
    }
    val borderColor = when {
        isCompleted -> Color(0xFFB8D9C5)
        isCurrent -> Main40
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF28a745)
                        isCurrent -> Main40
                        else -> Color(0xFFE0E0E0)
                    }
                )
                .clickable(enabled = isActive && (isCurrent || isCompleted), onClick = onCompleteClick),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "완료", tint = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text((index + 1).toString(), color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onItemClick)
        ) {
            Text(
                text = "세트 ${index + 1}",
                fontWeight = FontWeight.Bold,
                color = when {
                    isCompleted -> Color(0xFF1E7B3B)
                    isCurrent -> Main40
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text("$reps 회", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RestTimerBar(viewModel: TimerViewModel) {
    val timeLeft by viewModel.remainingTime.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("휴식 중", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(formatTime(timeLeft), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = { viewModel.stopRest() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Main40,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "건너뛰기")
                Spacer(Modifier.width(4.dp))
                Text("건너뛰기")
            }
        }
    }
}

@Composable
fun SettingsDialog(
    totalSets: Int,
    restTime: Int,
    onTotalSetsChange: (Int) -> Unit,
    onRestTimeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localSets by remember { mutableStateOf(totalSets.toString()) }
    var localTime by remember { mutableStateOf(restTime.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = localSets,
                    onValueChange = { localSets = it },
                    label = { Text("총 세트 수") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = localTime,
                    onValueChange = { localTime = it },
                    label = { Text("휴식 시간 (초)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                localSets.toIntOrNull()?.let(onTotalSetsChange)
                localTime.toIntOrNull()?.let(onRestTimeChange)
                onDismiss()
            }, modifier = Modifier.fillMaxWidth()) { Text("완료") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun EditRepsDialog(
    setNumber: Int,
    initialReps: Int,
    onConfirm: (Int) -> Unit,
    onConfirmAll: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember { mutableStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("횟수 수정") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("세트 ${setNumber}의 반복 횟수를 조정합니다.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = { reps = (reps - 1).coerceAtLeast(0) },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-1")
                    }
                    Text(reps.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Main40)
                    IconButton(
                        onClick = { reps++ },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+1")
                    }
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        onConfirm(reps)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("확인") }

                TextButton(
                    onClick = {
                        onConfirmAll(reps)
                        onDismiss()
                    }
                ) {
                    Text("남은 모든 세트에 적용")
                }
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
        title = { Text("체크 해제") },
        text = { Text("세트 ${setNumber}의 완료를 취소하시겠습니까? 이후 세트의 완료도 함께 취소됩니다.") },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("체크 해제") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
