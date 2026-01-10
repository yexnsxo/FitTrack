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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fittrack.data.TodayExerciseEntity
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
    viewModel: RecordViewModel
) {
    val photos by viewModel.photosForSelectedDate.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val exercises by viewModel.exercisesForSelectedDate.collectAsState()

    Scaffold {
        paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            RecordCalendar(markedDates = markedDates, onDateSelected = { date -> viewModel.onDateSelected(date) })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
                val photo = photos.firstOrNull()
                if (photo != null) {
                    Button(onClick = { viewModel.deletePhoto(photo) }) {
                        Text("사진 삭제")
                    }
                }
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    ExerciseListView(exercises = exercises)
                }

                val photo = photos.firstOrNull()
                if (photo != null && photo.uri.isNotEmpty()) {
                    item {
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
                    .background(color = Color.Red, shape = CircleShape)
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
    Column {
        exercises.forEach { exercise ->
            Text(text = "${exercise.name} - ${exercise.sets} sets, ${exercise.repsPerSet} reps")
        }
    }
}

@Composable
fun MonthHeader(daysOfWeek: List<CalendarDay>, month: String) {
    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
