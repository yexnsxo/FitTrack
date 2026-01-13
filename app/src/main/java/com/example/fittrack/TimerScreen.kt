package com.example.fittrack

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fittrack.ui.theme.Main40
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    todoViewModel: TodoViewModel,
    onFinish: () -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: TimerViewModel = viewModel(),
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEditRepsDialogForSet by remember { mutableStateOf<Int?>(null) }
    var showConfirmUncheckDialogForSet by remember { mutableStateOf<Int?>(null) }
    val workoutType by viewModel.workoutType.collectAsState()

    // RecordScreen과 동일하게 배경색을 채우고 내부 패딩 적용
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // TodoScreen 배경색과 통일
    ) {
        // LazyColumn이 남은 공간을 차지하도록 weight(1f) 적용
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = 16.dp, 
                bottom = 32.dp // 하단 여백 충분히 확보
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val rowId by viewModel.targetRowId.collectAsState()
                val totalTime by viewModel.totalWorkoutTime.collectAsState()
                val setReps by viewModel.setReps.collectAsState()
                val setWeights by viewModel.setWeights.collectAsState()

                val onCompleteWorkout: (Boolean) -> Unit = { shouldRecord ->
                    if (shouldRecord) {
                        rowId?.let { id ->
                            val completedSets = (viewModel.currentSet.value - 1).coerceAtLeast(0)
                            val repsList = setReps.take(completedSets).map { it.toString() }
                            val weightsList = setWeights.take(completedSets).map { it.toString() }

                            todoViewModel.completeWorkoutFromTimer(
                                rowId = id,
                                actualSec = totalTime,
                                setReps = repsList,
                                setWeights = weightsList
                            )
                        }
                        onFinish()
                    } else {
                        viewModel.clearWorkout()
                    }
                }
                
                if (workoutType == "time") {
                    TimeModeScreen(
                        viewModel = viewModel,
                        onEditRepsClick = { set -> showEditRepsDialogForSet = set },
                        onUncheckSetClick = { set -> showConfirmUncheckDialogForSet = set },
                        onSettingsClick = { showSettingsSheet = true },
                        onCompleteWorkout = onCompleteWorkout
                    )
                } else {
                    RepsModeScreen(
                        viewModel = viewModel,
                        onEditRepsClick = { set -> showEditRepsDialogForSet = set },
                        onUncheckSetClick = { set -> showConfirmUncheckDialogForSet = set },
                        onSettingsClick = { showSettingsSheet = true },
                        onCompleteWorkout = onCompleteWorkout
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        val totalSets by viewModel.totalSets.collectAsState()
        val restTime by viewModel.totalRestTime.collectAsState()
        val workoutType by viewModel.workoutType.collectAsState()
        val exerciseName by viewModel.exerciseName.collectAsState()

        SettingsSheet(
            totalSets = totalSets,
            restTime = restTime,
            workoutType = workoutType,
            isWorkoutStarted = exerciseName.isNotEmpty(),
            onTotalSetsChange = { viewModel.setTotalSets(it) },
            onRestTimeChange = { viewModel.setRestTime(it) },
            onWorkoutTypeChange = { viewModel.setWorkoutType(it) },
            onClearWorkout = { viewModel.clearWorkout() },
            onDismiss = { showSettingsSheet = false }
        )
    }

    showEditRepsDialogForSet?.let { set ->
        val setReps by viewModel.setReps.collectAsState()
        val setWeights by viewModel.setWeights.collectAsState()
        val totalSets by viewModel.totalSets.collectAsState()
        val workoutType by viewModel.workoutType.collectAsState()
        EditRepsDialog(
            setNumber = set,
            initialReps = setReps.getOrNull(set - 1) ?: 0,
            initialWeight = setWeights.getOrNull(set - 1) ?: 0,
            unit = if (workoutType == "time") "분" else "회",
            onConfirm = { newReps, newWeight ->
                viewModel.setRepsForSet(set, newReps)
                viewModel.setWeightForSet(set, newWeight)
            },
            onConfirmAll = { newReps, newWeight ->
                for (i in set..totalSets) {
                    viewModel.setRepsForSet(i, newReps)
                    viewModel.setWeightForSet(i, newWeight)
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
            },
            onDismiss = { showConfirmUncheckDialogForSet = null }
        )
    }
}

@Composable
fun TimeModeScreen(
    viewModel: TimerViewModel,
    onEditRepsClick: (Int) -> Unit,
    onUncheckSetClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onCompleteWorkout: (Boolean) -> Unit
) {
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startWorkout()
            }
        }
    )
    val exerciseName by viewModel.exerciseName.collectAsState()
    val remainingSetTime by viewModel.remainingSetTime.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val setReps by viewModel.setReps.collectAsState()
    val totalDuration = (setReps.getOrNull(currentSet - 1)?.times(60)) ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (exerciseName.isNotEmpty()) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Main40
            )
        }

        if (isWorkoutStarted) {
            StatsCards(viewModel)
            CircularTimer(time = remainingSetTime, totalTime = totalDuration)
        } else {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startWorkout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Main40),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("운동 시작", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }

        SetChecklist(viewModel, onEditRepsClick, onUncheckSetClick, onSettingsClick)

        val isResting by viewModel.isResting.collectAsState()
        if (isResting) {
            RestTimerBar(viewModel)
        }

        if (isWorkoutStarted) {
            val totalSets by viewModel.totalSets.collectAsState()
            val isFinished = currentSet > totalSets

            Button(
                onClick = { viewModel.finishSet() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !isFinished,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28a745)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("세트 완료", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Button(
                onClick = { onCompleteWorkout(exerciseName.isNotEmpty()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isFinished) Main40 else Color.Gray),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (exerciseName.isNotEmpty()) "운동 종료 및 기록" else "운동 종료",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CircularTimer(time: Int, totalTime: Int) {
    val progress = if (totalTime > 0) time.toFloat() / totalTime else 0f

    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 24.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            drawArc(
                color = Color.LightGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Main40,
                startAngle = -90f,
                sweepAngle = -progress * 360f,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
        }
        Text(text = formatTime(time), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
    }
}

@Composable
fun RepsModeScreen(
    viewModel: TimerViewModel,
    onEditRepsClick: (Int) -> Unit,
    onUncheckSetClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onCompleteWorkout: (Boolean) -> Unit
) {
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startWorkout()
            }
        }
    )
    val exerciseName by viewModel.exerciseName.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (exerciseName.isNotEmpty()) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Main40
            )
        }

        if (isWorkoutStarted) {
            StatsCards(viewModel)
        } else {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startWorkout()
                    }
                 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Main40),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("운동 시작", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }

        SetChecklist(viewModel, onEditRepsClick, onUncheckSetClick, onSettingsClick)

        val isResting by viewModel.isResting.collectAsState()
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28a745)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("세트 완료", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Button(
                onClick = { onCompleteWorkout(exerciseName.isNotEmpty()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isFinished) Main40 else Color.Gray),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(text = if (exerciseName.isNotEmpty()) "운동 종료 및 기록" else "운동 종료",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White)
            }
        }
    }
}

@Composable
fun StatsCards(viewModel: TimerViewModel) {
    val totalSets by viewModel.totalSets.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val setReps by viewModel.setReps.collectAsState()
    val totalWorkoutTime by viewModel.totalWorkoutTime.collectAsState()
    val workoutType by viewModel.workoutType.collectAsState()

    val completedSets = (currentSet - 1).coerceAtLeast(0)
    val totalReps = setReps.take(completedSets).sum()
    val unitLabel = if (workoutType == "time") "총 시간" else "총 횟수"
    val unitValue = if (workoutType == "time") "$totalReps 분" else "$totalReps 회"

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(title = "소요 시간", value = formatTime(totalWorkoutTime), modifier = Modifier.weight(1f))
        StatCard(title = "완료 세트", value = "$completedSets/$totalSets", modifier = Modifier.weight(1f), isPrimary = true)
        StatCard(title = unitLabel, value = unitValue, modifier = Modifier.weight(1f))
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
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) Main40 else Color(0xFF111827)
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
    val setWeights by viewModel.setWeights.collectAsState()
    val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()
    val workoutType by viewModel.workoutType.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "운동 계획",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = Color(0xFF111827)
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", tint = Color(0xFF111827))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..totalSets).forEach { setNumber ->
                    val isCompleted = setNumber < currentSet
                    val isCurrent = setNumber == currentSet
                    SetItem(
                        index = setNumber - 1,
                        reps = setReps.getOrNull(setNumber - 1) ?: 0,
                        weight = setWeights.getOrNull(setNumber - 1) ?: 0,
                        unit = if (workoutType == "time") "분" else "회",
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
    weight: Int,
    unit: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isActive: Boolean,
    onCompleteClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFFE6F4EA)
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
            .clickable(onClick = onItemClick)
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
                Text((index + 1).toString(), color = if (isCurrent) Color.White else Color(0xFF111827), fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "세트 ${index + 1}",
            fontWeight = FontWeight.Bold,
            color = when {
                isCompleted -> Color(0xFF1E7B3B)
                isCurrent -> Main40
                else -> Color(0xFF111827)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (weight > 0 && unit == "회") {
                Text(
                    text = "${weight}kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }
            Text(
                text = "$reps $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun RestTimerBar(viewModel: TimerViewModel) {
    val timeLeft by viewModel.remainingRestTime.collectAsState()

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
                Text("휴식 중", style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827))
                Text(formatTime(timeLeft), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            Button(
                onClick = { viewModel.stopRest() },
                colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "건너뛰기")
                Spacer(Modifier.width(4.dp))
                Text("건너뛰기", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    totalSets: Int,
    restTime: Int,
    workoutType: String,
    isWorkoutStarted: Boolean,
    onTotalSetsChange: (Int) -> Unit,
    onRestTimeChange: (Int) -> Unit,
    onWorkoutTypeChange: (String) -> Unit,
    onClearWorkout: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var localSets by remember { mutableIntStateOf(totalSets) }
    var localRestTime by remember { mutableIntStateOf(restTime) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Main40)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = "운동 설정",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 운동 방식
                Text("운동 방식", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111827))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleChip(
                        text = "횟수",
                        selected = workoutType == "reps",
                        onClick = { onWorkoutTypeChange("reps") },
                        enabled = !isWorkoutStarted,
                        modifier = Modifier.weight(1f)
                    )
                    ToggleChip(
                        text = "시간",
                        selected = workoutType == "time",
                        onClick = { onWorkoutTypeChange("time") },
                        enabled = !isWorkoutStarted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepperNumberField(
                        label = "총 세트 수",
                        value = localSets,
                        min = 1,
                        max = 99,
                        onValueChange = { localSets = it },
                        modifier = Modifier.weight(1f)
                    )

                    StepperNumberField(
                        label = "휴식 시간(초)",
                        value = localRestTime,
                        min = 0,
                        max = 300,
                        step = 5,
                        onValueChange = { localRestTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (isWorkoutStarted) {
                    OutlinedButton(
                        onClick = {
                            onClearWorkout()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Text("운동 등록 해제", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onTotalSetsChange(localSets)
                        onRestTimeChange(localRestTime)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                ) {
                    Text("설정 완료", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepperNumberField(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    val bgColor = if (isFocused.value) Color.White else Color(0xffF8F8F8)
    val borderColor = if (isFocused.value) Main40 else Color(0xFFE7E7E7)

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(shape)
                .background(bgColor)
                .border(2.dp, borderColor, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }
                        if (filtered.isEmpty()) {
                            onValueChange(min)
                            return@BasicTextField
                        }
                        val n = filtered.toIntOrNull() ?: return@BasicTextField
                        onValueChange(n.coerceIn(min, max))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused.value = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )
                )
            }

            StepperButtons(
                onUp = {
                    onValueChange((value + step).coerceAtMost(max))
                    focusRequester.requestFocus()
                },
                onDown = {
                    onValueChange((value - step).coerceAtLeast(min))
                    focusRequester.requestFocus()
                }
            )
        }
    }
}

@Composable
private fun StepperButtons(
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 32.dp, height = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "증가", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDown, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "감소", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Main40 else Color(0xFFF1F5F9)
    val fg = if (selected) Color.White else Color(0xFF111827)
    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg.copy(alpha = alpha))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg.copy(alpha = alpha), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun EditRepsDialog(
    setNumber: Int,
    initialReps: Int,
    initialWeight: Int,
    unit: String,
    onConfirm: (Int, Int) -> Unit,
    onConfirmAll: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember { mutableIntStateOf(initialReps) }
    var weight by remember { mutableIntStateOf(initialWeight) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = Color.White
        ) {
            Column {
                // 헤더
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(Main40)
                        .padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (unit == "분") "시간 수정" else "횟수 및 무게 수정",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "세트 ${setNumber} 기록 수정",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 반복 횟수 / 시간
                    Text(
                        if (unit == "분") "시간 (분) *" else "횟수 (회) *",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                    StepperNumberField(
                        label = "",
                        value = reps,
                        min = 0,
                        max = 300,
                        onValueChange = { reps = it }
                    )

                    // 무게 (횟수 모드일 때만 표시)
                    if (unit == "회") {
                        Text(
                            "무게 (kg) *",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827)
                        )
                        StepperNumberField(
                            label = "",
                            value = weight,
                            min = 0,
                            max = 500,
                            onValueChange = { weight = it }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 버튼 영역
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onConfirm(reps, weight)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                        ) {
                            Text("현재 세트만 적용", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                onConfirmAll(reps, weight)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Main40),
                            border = BorderStroke(2.dp, Main40)
                        ) {
                            Text("남은 모든 세트에 적용", fontSize = 18.sp, color = Color(0xFF111827))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ConfirmUncheckDialog(
    setNumber: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("체크 해제", color = Color(0xFF111827)) },
        text = { Text("세트 ${setNumber}의 완료를 취소하시겠습니까? 이후 세트의 완료도 함께 취소됩니다.", color = Color(0xFF111827)) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("체크 해제", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color(0xFF111827)) }
        }
    )
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}
