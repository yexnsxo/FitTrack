package com.example.fittrack

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fittrack.data.Exercise
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.ui.theme.Main40
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: RecordViewModel,
    todoViewModel: TodoViewModel
) {
    val showCalendar by viewModel.showCalendar.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        TabRow(
            selectedTabIndex = if (showCalendar) 0 else 1,
            containerColor = Color.White,
            indicator = { tabPositions ->
                val index = if (showCalendar) 0 else 1
                if (index < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = Main40
                    )
                }
            }
        ) {
            Tab(
                selected = showCalendar,
                onClick = { viewModel.setShowCalendar(true) },
                text = {
                    Text(
                        text = "달력",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                },
                selectedContentColor = Main40,
                unselectedContentColor = Color.Gray
            )
            Tab(
                selected = !showCalendar,
                onClick = { viewModel.setShowCalendar(false) },
                text = {
                    Text(
                        text = "전체 사진",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                },
                selectedContentColor = Main40,
                unselectedContentColor = Color.Gray
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (showCalendar) {
                CalendarView(viewModel = viewModel, todoViewModel = todoViewModel)
            } else {
                AllPhotosView(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CalendarView(viewModel: RecordViewModel, todoViewModel: TodoViewModel) {
    val photos by viewModel.photosForSelectedDate.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val exercises by viewModel.exercisesForSelectedDate.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                RecordCalendar(
                    markedDates = markedDates,
                    onDateSelected = { date -> viewModel.onDateSelected(date) }
                )
            }
        }

        item { Spacer(Modifier.height(2.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        val photo = photos.firstOrNull()
                        if (photo != null) {
                            OutlinedButton(
                                onClick = { viewModel.deletePhoto(photo) },
                                modifier = Modifier.height(34.dp),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Gray
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("사진 삭제", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }

                    val photo = photos.firstOrNull()
                    if (photo != null && photo.uri.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val imageSize = (LocalConfiguration.current.screenWidthDp.dp * 0.7f)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photo.uri.toUri())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(imageSize)
                                )
                            }
                        }
                    }
                    ExerciseListView(
                        exercises = exercises,
                        todoViewModel = todoViewModel,
                        recordViewModel = viewModel,
                        selectedDate = selectedDate
                    )
                }
            }
        }
        item { Spacer(Modifier.height(2.dp)) }
    }
}

@Composable
fun AllPhotosView(viewModel: RecordViewModel) {
    val allPhotos by viewModel.allPhotos.collectAsState()

    if (allPhotos.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "저장된 사진이 없습니다.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allPhotos) { photo ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    val imageSize = (LocalConfiguration.current.screenWidthDp.dp / 3)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(imageSize)
                    )
                }
            }
        }
    }
}


@Composable
fun Day(day: CalendarDay, isMarked: Boolean, onDateSelected: (LocalDate) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onDateSelected(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.dayOfMonth.toString())
        if (isMarked) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = Main40, shape = CircleShape)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun RecordCalendar(
    markedDates: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    HorizontalCalendar(
        state = state,
        dayContent = { day ->
            val isMarked = markedDates.contains(day.date)
            Day(day, isMarked, onDateSelected)
        },
        monthHeader = { month ->
            MonthHeader(daysOfWeek = month.weekDays.first(), month = month.yearMonth.toString())
        }
    )
}

@Composable
fun ExerciseListView(
    exercises: List<TodayExerciseEntity>,
    todoViewModel: TodoViewModel,
    recordViewModel: RecordViewModel,
    selectedDate: LocalDate
) {
    val expandedState = remember { mutableStateMapOf<Long, Boolean>() }
    var editingSetInfo by remember { mutableStateOf<TodayExerciseEntity?>(null) }
    var showAddExerciseModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "운동 기록 💪",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { showAddExerciseModal = true },
                modifier = Modifier
                    .size(32.dp)
                    .background(Main40, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "기록 추가",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (exercises.isNotEmpty()) {
            exercises.forEachIndexed { index, exercise ->
                val isExpanded = expandedState[exercise.rowId] ?: false

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedState[exercise.rowId] = !isExpanded
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = exercise.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { editingSetInfo = exercise },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "상세 기록 수정",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }

                        val totalReps = exercise.actualReps
                        val durationMinutes = exercise.actualDurationSec / 60
                        val durationSeconds = exercise.actualDurationSec % 60

                        val summaryText = buildString {
                            val isTimeBased = exercise.repsPerSet == null
                            append("${exercise.sets}세트 (")
                            if (isTimeBased) {
                                if (durationMinutes > 0 || durationSeconds > 0) {
                                    if (durationMinutes > 0) append("${durationMinutes}분 ")
                                    if (durationSeconds > 0) append("${durationSeconds}초")
                                } else {
                                    append("0초")
                                }
                            } else {
                                if (totalReps > 0) {
                                    append("총 ${totalReps}회")
                                } else {
                                    append("0회")
                                }
                            }
                            append(")")
                        }

                        Text(
                            text = summaryText,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        ExerciseDetailView(exercise)
                    }
                }
                if (index < exercises.size - 1) {
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        } else {
            Text(
                text = "이 날은 운동 기록이 없어요.. 🥺",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    editingSetInfo?.let { item ->
        EditSetInfoDialog(
            item = item,
            onDismiss = { editingSetInfo = null },
            onConfirm = { reps, weights ->
                todoViewModel.updateSetInfo(item.rowId, reps, weights)
                editingSetInfo = null
            }
        )
    }

    if (showAddExerciseModal) {
        RecordAddExerciseModal(
            dateKey = selectedDate.toString(),
            todoViewModel = todoViewModel,
            recordViewModel = recordViewModel,
            onDismiss = { showAddExerciseModal = false }
        )
    }
}

@Composable
fun RecordAddExerciseModal(
    dateKey: String,
    todoViewModel: TodoViewModel,
    recordViewModel: RecordViewModel,
    onDismiss: () -> Unit
) {
    val filteredCatalog by todoViewModel.filteredCatalog.collectAsState()
    val selectedCategory by todoViewModel.selectedCategory.collectAsState()
    val photos by recordViewModel.photosForSelectedDate.collectAsState()

    var pendingAddExercise by remember { mutableStateOf<Exercise?>(null) }
    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let { recordViewModel.addPhoto(it, dateKey) }
            }
            onDismiss()
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                recordViewModel.addPhoto(it, dateKey)
            }
            onDismiss()
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val newImageFile = createImageFile(context)
                val newImageUri =
                    FileProvider.getUriForFile(context, "com.example.fittrack.provider", newImageFile)
                imageUri = newImageUri
                cameraLauncher.launch(newImageUri)
            }
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("운동 기록 추가", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(Modifier.height(16.dp))
                CategoryCard(selected = selectedCategory, onSelect = todoViewModel::selectCategory)
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredCatalog.forEach { ex ->
                        ExerciseItem(exercise = ex, onAdd = { pendingAddExercise = ex })
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (photos.isEmpty()) {
                            showPhotoChoiceDialog = true
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Main40)
                ) {
                    Text("운동 추가 완료", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    pendingAddExercise?.let { ex ->
        AddExerciseDialog(
            exercise = ex,
            onDismiss = { pendingAddExercise = null },
            onConfirmStrength = { sets, reps ->
                todoViewModel.addExerciseToDateWithSelection(dateKey, ex, sets, reps)
                pendingAddExercise = null
            },
            onConfirmDuration = { sets, mins ->
                todoViewModel.addExerciseToDateWithDuration(dateKey, ex, sets, mins)
                pendingAddExercise = null
            }
        )
    }

    if (showPhotoChoiceDialog) {
        Dialog(onDismissRequest = { showPhotoChoiceDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("오늘 운동 남기기", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("기록을 위해 사진을 남기시겠습니까?", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showPhotoChoiceDialog = false; showPhotoSourceDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main40)
                    ) {
                        Text("사진과 함께 기록", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            recordViewModel.addPhoto(null, dateKey)
                            showPhotoChoiceDialog = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        )
                    ) {
                        Text("사진 없이 기록", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPhotoSourceDialog) {
        Dialog(onDismissRequest = { showPhotoSourceDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("사진 선택", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false; permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main40)
                    ) {
                        Text("카메라", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false; galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        )
                    ) {
                        Text("갤러리", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailView(exercise: TodayExerciseEntity) {
    val isTimeBased = exercise.repsPerSet == null

    val setReps = if (exercise.setReps.isNotEmpty()) {
        exercise.setReps.split(",").mapNotNull { it.trim().toIntOrNull() }
    } else if (isTimeBased && exercise.sets > 0) {
        List(exercise.sets) { exercise.duration ?: 0 }
    } else {
        emptyList()
    }

    val setWeights = exercise.setWeights
        .split(",")
        .map { it.trim() }

    if (setReps.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        setReps.forEachIndexed { idx, reps ->
            val weight = setWeights.getOrNull(idx) ?: ""
            val unit2 = if (isTimeBased) "km" else "kg"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "세트 ${idx + 1}:",
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280)
                )

                val valueText = buildString {
                    if (isTimeBased) {
                        append("${reps}분")
                        if (weight.isNotEmpty() && (weight.toDoubleOrNull() ?: 0.0) > 0.0) {
                            append(" / ${weight}${unit2}")
                        }
                    } else {
                        append("${reps}회")
                        if (weight.isNotEmpty() && (weight.toDoubleOrNull() ?: 0.0) > 0.0) {
                            val w =
                                if (weight.toDoubleOrNull()!! % 1.0 == 0.0) weight.toDoubleOrNull()!!
                                    .toInt().toString() else weight
                            append(" / ${w}${unit2}")
                        }
                    }
                }

                Text(
                    text = valueText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}


@Composable
fun MonthHeader(daysOfWeek: List<CalendarDay>, month: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            text = month,
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            val formatter = remember { DateTimeFormatter.ofPattern("E") }
            for (day in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = day.date.format(formatter),
                )
            }
        }
    }
}
