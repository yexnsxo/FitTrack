package com.example.fittrack

import android.R.attr.onClick
import android.R.attr.top
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    navController: NavController
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
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (showCalendar) 0 else 1]),
                    color = Main40
                )
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
                CalendarView(viewModel = viewModel, navController = navController)
            } else {
                AllPhotosView(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CalendarView(viewModel: RecordViewModel, navController: NavController) {
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

        item {Spacer(Modifier.height(2.dp))}

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val photo = photos.firstOrNull()
                            if (photo != null) {
                                OutlinedButton(
                                    onClick = { viewModel.deletePhoto(photo) },
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)), // 회색 테두리
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

                            if (exercises.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        navController.navigate("editRecord/$dateStr")
                                    },
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "편집",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("기록 편집", color = Color.Gray, fontSize = 13.sp)
                                }
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
                    ExerciseListView(exercises = exercises)
                }
            }
        }
        item {Spacer(Modifier.height(2.dp))}
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
fun ExerciseListView(exercises: List<TodayExerciseEntity>) {
    val expandedState = remember { mutableStateMapOf<Long, Boolean>() }

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
    ) {
        if (exercises.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "운동 기록 💪",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )

                        val totalReps = exercise.actualReps
                        val durationMinutes = exercise.actualDurationSec / 60
                        val durationSeconds = exercise.actualDurationSec % 60

                        val summaryText = buildString {
                            if (totalReps > 0) {
                                append("총 ${totalReps}회")
                            }
                            if (durationMinutes > 0 || durationSeconds > 0) {
                                if (isNotEmpty()) append(", ")
                                if(durationMinutes > 0) append("${durationMinutes}분 ")
                                append("${durationSeconds}초")
                            }
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
}

@Composable
fun ExerciseDetailView(exercise: TodayExerciseEntity) {
    val setReps = exercise.setReps
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }

    val setWeights = exercise.setWeights
        .split(",")
        .mapNotNull { it.trim().toDoubleOrNull() }

    // 세트 정보가 없으면(예: 스트레칭) 펼쳐도 보여줄 게 없게 처리
    if (setReps.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        setReps.forEachIndexed { idx, reps ->
            val weight = setWeights.getOrNull(idx)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: "세트 1:"
                Text(
                    text = "세트 ${idx + 1}:",
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280)
                )

                // 오른쪽: "24회 / 5kg" 또는 "20회"
                val valueText = buildString {
                    append("${reps}회")
                    if (weight != null && weight > 0.0) {
                        // 5.0 -> 5 처럼 보이게 하고 싶으면 아래 형식 사용
                        val w = if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
                        append(" / ${w}kg")
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
