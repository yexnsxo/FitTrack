package com.example.fittrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.fittrack.data.Exercise
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.ui.theme.Main40
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodoScreen(
    vm: TodoViewModel,
    timerViewModel: TimerViewModel,
    recordViewModel: RecordViewModel,
    navController: NavController
) {
    val progress by vm.progress.collectAsState()
    val selected by vm.selectedCategory.collectAsState()
    val filteredCatalog by vm.filteredCatalog.collectAsState()
    val todayList by vm.todayList.collectAsState()
    val isTodayPhotoSaved by vm.isTodayPhotoSaved.collectAsState()
    val context = LocalContext.current
    var showInitialDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let { recordViewModel.addPhoto(it) }
            }
            navController.navigate("record")
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                recordViewModel.addPhoto(it)
            }
            navController.navigate("record")
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val newImageFile = createImageFile(context)
                val newImageUri = FileProvider.getUriForFile(
                    context,
                    "com.example.fittrack.provider",
                    newImageFile
                )
                imageUri = newImageUri
                cameraLauncher.launch(newImageUri)
            } else {
                // Handle permission denial
            }
        }

    val pendingAddState = remember { mutableStateOf<Exercise?>(null) }
    val showDirectAddState = remember { mutableStateOf(false) }
    val pendingEditSet = remember { mutableStateOf<TodayExerciseEntity?>(null) }


    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ProgressOverview(
                    completedCount = progress.completedCount,
                    totalCount = progress.totalCount,
                    caloriesSum = progress.caloriesSum,
                    totalDurationSec = progress.totalDurationSec
                )
            }

            item {
                TodayListCard(
                    items = todayList,
                    onToggle = { item, checked ->
                        vm.toggleCompleted(item.rowId, checked)
                        if (!checked) {
                            timerViewModel.resetIfMatches(item.rowId)
                        }
                    },
                    onDelete = { item -> vm.deleteTodayRow(item.rowId) },
                    onEditStrength = { item, sets, reps ->
                        vm.updateTodayRowStrength(item, sets, reps)
                    },
                    onEditDuration = { item, sets, minutes ->
                        vm.updateTodayRowDuration(item, sets, minutes)
                    },
                    onTimerClick = { item ->
                        val target = item.duration ?: 0
                        val type = if (item.duration != null) "time" else "reps"
                        val sets = item.sets
                        val setReps = item.setReps
                        val setWeights = item.setWeights
                        navController.navigate("timer?rowId=${item.rowId}&name=${item.name}&target=$target&type=$type&sets=$sets&setReps=$setReps&setWeights=$setWeights")
                    },
                    onEditActualTime = { item, totalSec ->
                        vm.updateActualTime(item.rowId, totalSec) // ✅ 콜백 연결 재확인
                    },
                    onEditSetInfo = { item -> pendingEditSet.value = item }
                )
            }

            if (progress.completedCount != 0 && progress.completedCount == progress.totalCount) {
                item {
                    AllExercisesDoneCard(
                        onSaveClick = { showInitialDialog = true },
                        isButtonVisible = !isTodayPhotoSaved
                    )
                }
            }

            item { CategoryCard(selected = selected, onSelect = vm::selectCategory) }

            item {
                ExerciseCatalogCard(
                    exercises = filteredCatalog,
                    onAdd = { ex -> pendingAddState.value = ex },
                    onDeleteCustom = { ex -> vm.deleteCustomExercise(ex) },
                    onOpenDirectAdd = { showDirectAddState.value = true }
                )
            }
        }

        if (showInitialDialog) {
            Dialog(onDismissRequest = { showInitialDialog = false }) {
                val shape = RoundedCornerShape(26.dp)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = Color.White
                ) {
                    Column {
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
                                    text = "오늘 운동 남기기",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "사진을 남기시겠습니까?",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { showInitialDialog = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    showInitialDialog = false
                                    showPhotoDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Main40)
                            ) {
                                Text("사진과 함께 기록", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            Button(
                                onClick = {
                                    recordViewModel.addPhoto(null)
                                    showInitialDialog = false
                                    navController.navigate("record")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = Color(0xFF475569)
                                )
                            ) {
                                Text("사진 없이 기록", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showPhotoDialog) {
            Dialog(onDismissRequest = { showPhotoDialog = false }) {
                val shape = RoundedCornerShape(26.dp)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = Color.White
                ) {
                    Column {
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
                                    text = "사진 선택",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "사진을 촬영하거나 갤러리에서 선택하세요.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { showPhotoDialog = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    showPhotoDialog = false
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Main40)
                            ) {
                                Text("카메라", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            Button(
                                onClick = {
                                    showPhotoDialog = false
                                    galleryLauncher.launch("image/*")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = Color(0xFF475569)
                                )
                            ) {
                                Text("갤러리", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        pendingAddState.value?.let { pending ->
            AddExerciseDialog(
                exercise = pending,
                onDismiss = { pendingAddState.value = null },
                onConfirmStrength = { sets, reps ->
                    vm.addExerciseToTodayWithSelection(pending, sets, reps)
                    pendingAddState.value = null
                },
                onConfirmDuration = { sets, minutes ->
                    vm.addExerciseToTodayWithDuration(pending, sets, minutes)
                    pendingAddState.value = null
                }
            )
        }

        if (showDirectAddState.value) {
            AddCustomExerciseDialog(
                onDismiss = { showDirectAddState.value = false },
                onConfirm = { newExercise ->
                    vm.addCustomExerciseToCatalog(newExercise)
                    showDirectAddState.value = false
                }
            )
        }

        pendingEditSet.value?.let { item ->
            EditSetInfoDialog(
                item = item,
                onDismiss = { pendingEditSet.value = null },
                onConfirm = { sets, reps, weights ->
                    vm.updateSetInfo(item.rowId, sets, reps, weights)
                    pendingEditSet.value = null
                }
            )
        }
    }
}

@Composable
fun ProgressOverview(completedCount: Int, totalCount: Int, caloriesSum: Int, totalDurationSec: Int) {
    Spacer(Modifier.height(10.dp))

    val shape = RoundedCornerShape(18.dp)
    val bg = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1F6FF2),
            Color(0xFF2E86FF)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, shape)
                .padding(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    Text(
                        text = "오늘의 운동",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    val h = totalDurationSec / 3600
                    val m = (totalDurationSec % 3600) / 60
                    val s = totalDurationSec % 60

                    val timeText = when {
                        h > 0 -> if (s > 0) "${h}시간 ${m}분 ${s}초" else "${h}시간 ${m}분"
                        m > 0 -> if (s > 0) "${m}분 ${s}초" else "${m}분"
                        else -> "${s}초"
                    }

                    Text(
                        "총 운동 시간",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "완료됨",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${completedCount}/${totalCount}",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 총 소모 칼로리 추가
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "총 칼로리",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${caloriesSum}kcal",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        PercentProgressRing(
                            completedCount = completedCount,
                            totalCount = totalCount,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PercentProgressRing(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val p = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

            drawArc(
                color = Color.White.copy(alpha = 0.30f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                style = stroke
            )
        }

        Text(
            text = "${(p * 100).toInt()}%",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryCard(
    selected: CategoryFilter,
    onSelect: (CategoryFilter) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("카테고리", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)

            val scroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryFilter.entries.forEach { item ->
                    CategoryChip(
                        emoji = item.emoji,
                        text = item.label,
                        selected = (item == selected),
                        onClick = { onSelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    emoji: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Main40 else Color(0xFFF1F3F5)
    val fg = if (selected) Color.White else Color(0xFF1A1A1A)

    Card(
        modifier = Modifier
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 18.sp, color = fg)
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

@Composable
fun AllExercisesDoneCard(
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isButtonVisible: Boolean
) {
    val shape = RoundedCornerShape(28.dp)

    val bg = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1F6FF2),
            Color(0xFF2E86FF)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "모든 운동이 끝났습니다! 🎉",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "오늘도 멋지게 완료하셨네요!",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                )
            }
            if (isButtonVisible) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F6FF2)
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "오늘의 운동 기록 남기기",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

fun createImageFile(context: Context): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

@Composable
fun EditSetInfoDialog(
    item: TodayExerciseEntity,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: List<String>, weights: List<String>) -> Unit
) {
    val currentReps = remember(item) {
        item.setReps.split(",").map { it.trim() }
    }
    val currentWeights = remember(item) {
        item.setWeights.split(",").map { it.trim() }
    }

    val setsState = remember(item) { mutableStateOf(item.sets) }
    val repsState = remember(item) {
        mutableStateOf(List(item.sets) { i ->
            currentReps.getOrElse(i) { "" }
        })
    }
    val weightsState = remember(item) {
        mutableStateOf(List(item.sets) { i ->
            currentWeights.getOrElse(i) { "" }
        })
    }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = shape,
            color = Color.White
        ) {
            Column {
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
                            text = item.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "세트 정보 수정",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("세트 수", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        CompactStepperField(
                            value = setsState.value.toString(),
                            onValueChange = {
                                val newSize = it.toIntOrNull()?.coerceIn(1, 100) ?: 1
                                val oldSize = setsState.value

                                setsState.value = newSize
                                if (newSize > oldSize) {
                                    repsState.value = repsState.value + List(newSize - oldSize) { "" }
                                    weightsState.value = weightsState.value + List(newSize - oldSize) { "" }
                                } else if (newSize < oldSize) {
                                    repsState.value = repsState.value.take(newSize)
                                    weightsState.value = weightsState.value.take(newSize)
                                }
                            },
                            label = "세트"
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(setsState.value) { i ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "세트 ${i + 1}",
                                    modifier = Modifier.width(45.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                
                                CompactStepperField(
                                    value = repsState.value.getOrElse(i) { "" },
                                    onValueChange = { newValue ->
                                        val newList = repsState.value.toMutableList()
                                        if (i < newList.size) {
                                            newList[i] = newValue
                                            repsState.value = newList
                                        }
                                    },
                                    label = "회",
                                    modifier = Modifier.weight(1f)
                                )

                                CompactStepperField(
                                    value = weightsState.value.getOrElse(i) { "" },
                                    onValueChange = { newValue ->
                                        val newList = weightsState.value.toMutableList()
                                        if (i < newList.size) {
                                            newList[i] = newValue
                                            weightsState.value = newList
                                        }
                                    },
                                    label = "kg",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { onConfirm(setsState.value, repsState.value, weightsState.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main40)
                    ) {
                        Text("정보 수정 완료", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactStepperField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(Color(0xffF8F8F8))
            .border(1.dp, Color(0xFFE7E7E7), shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF111827)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Text(label, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(end = 2.dp))
        
        StepperButtons(
            onUp = {
                val current = value.toIntOrNull() ?: 0
                onValueChange((current + 1).toString())
            },
            onDown = {
                val current = value.toIntOrNull() ?: 0
                if (current > 0) onValueChange((current - 1).toString())
            }
        )
    }
}

@Composable
private fun StepperButtons(
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 28.dp, height = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "증가", tint = Color(0xFF374151), modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDown, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "감소", tint = Color(0xFF374151), modifier = Modifier.size(16.dp))
        }
    }
}
