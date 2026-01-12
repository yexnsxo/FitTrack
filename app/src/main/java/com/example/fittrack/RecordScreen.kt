package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun RecordScreen(modifier: Modifier = Modifier, viewModel: RecordViewModel) {
    val showCalendar by viewModel.showCalendar.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF1F3F5)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = if (showCalendar) 0 else 1,
                containerColor = Color.White
            ) {
                Tab(
                    selected = showCalendar,
                    onClick = { viewModel.setShowCalendar(true) },
                    text = { Text("달력") },
                    selectedContentColor = Main40,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = !showCalendar,
                    onClick = { viewModel.setShowCalendar(false) },
                    text = { Text("전체 사진") },
                    selectedContentColor = Main40,
                    unselectedContentColor = Color.Gray
                )
            }

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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                val amountText = if (exercise.duration != null) {
                    "${exercise.duration}분"
                } else {
                    "${exercise.repsPerSet}회"
                }
                val setsText = "${exercise.sets}세트"

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
                    Text(
                        text = "$setsText, $amountText",
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }
                if (index < exercises.size - 1) {
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
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
