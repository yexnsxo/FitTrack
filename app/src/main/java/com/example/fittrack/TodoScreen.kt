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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.fittrack.data.Exercise
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
                        val target = item.repsPerSet ?: item.duration ?: 0
                        val type = if (item.repsPerSet != null) "reps" else "time"
                        val sets = item.sets
                        navController.navigate("timer?rowId=${item.rowId}&name=${item.name}&target=$target&type=$type&sets=$sets")
                    },
                    onEditActualTime = { item, totalSec ->
                        vm.updateActualTime(item.rowId, totalSec) // ✅ 콜백 연결 재확인
                    }
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
            AlertDialog(
                onDismissRequest = { showInitialDialog = false },
                title = { Text("오늘 운동 남기기") },
                text = { Text("사진을 남기시겠습니까?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showInitialDialog = false
                            showPhotoDialog = true
                        }
                    ) {
                        Text("사진과 함께 기록")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            recordViewModel.addPhoto(null)
                            showInitialDialog = false
                            navController.navigate("record")
                        }
                    ) {
                        Text("사진 없이 기록")
                    }
                }
            )
        }

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("사진 선택") },
                text = { Text("사진을 촬영하거나 갤러리에서 선택하세요.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("카메라")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("갤러리")
                    }
                }
            )
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
