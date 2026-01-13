package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
    innerPadding: PaddingValues = PaddingValues(0.dp), // innerPadding 파라미터 추가
    viewModel: RecordViewModel
) {
    val showCalendar by viewModel.showCalendar.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xffF5F5F5))
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
                        fontSize = 20.sp
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
                        fontSize = 20.sp
                    )
                },
                selectedContentColor = Main40,
                unselectedContentColor = Color.Gray
            )
        }

        // 컨텐츠 영역에 weight(1f)를 주어 남은 공간만 정확히 차지하게 함
        Box(modifier = Modifier.weight(1f)) {
            if (showCalendar) {
                CalendarView(viewModel = viewModel)
            } else {
                AllPhotosView(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CalendarView(viewModel: RecordViewModel) {
    val photos by viewModel.photosForSelectedDate.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val exercises by viewModel.exercisesForSelectedDate.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), // Modifier.padding 대신 contentPadding 사용
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            color = Color.Black
                        )
                        val photo = photos.firstOrNull()
                        if (photo != null) {
                            Button(onClick = { viewModel.deletePhoto(photo) }) {
                                Text("사진 삭제")
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
                            val imageSize = (LocalConfiguration.current.screenWidthDp.dp / 2)

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photo.uri.toUri())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(imageSize)
                            )
                        }
                    }
                    ExerciseListView(exercises = exercises)
                }
            }
        }
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
            contentPadding = PaddingValues(16.dp), // contentPadding으로 변경
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allPhotos) { photo ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
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
            Text(
                text = "운동 기록",
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
                        ExerciseDetailView(exercise)
                    }
                }
                if (index < exercises.size - 1) {
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        } else {
            Text(
                text = "이 날은 운동 기록이 없어요.",
                fontSize = 16.sp,
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
    val setReps = exercise.setReps.split(",").mapNotNull { it.trim().toIntOrNull() }
    val setWeights = exercise.setWeights.split(",").mapNotNull { it.trim().toDoubleOrNull() }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        setReps.forEachIndexed { setIndex, reps ->
            val weight = setWeights.getOrNull(setIndex)
            val setText = "세트 ${setIndex + 1}: ${reps}회"
            val weightText = weight?.let { " / ${it}kg" } ?: ""

            Text(
                text = "$setText$weightText",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
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
