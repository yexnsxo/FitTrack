package com.example.fittrack

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel
) {
    val photos by viewModel.photos.collectAsState()
    val photoDates by viewModel.photoDates.collectAsState()

    Scaffold {
        paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            RecordCalendar(photoDates = photoDates)

            WorkoutStats(photoDates = photoDates)

            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val spacing = 8.dp
            val imageSize = (screenWidth - 32.dp - (spacing * 3)) / 4

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(photos.chunked(4)) { rowPhotos ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        rowPhotos.forEach { photo ->
                            if (photo.uri.isNotEmpty()) {
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
        }
    }
}

@Composable
fun WorkoutStats(photoDates: List<LocalDate>) {
    val cal = Calendar.getInstance()
    val currentYearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    val monthlyWorkoutDays = photoDates.distinct().count { it.year == currentYearMonth.year && it.month == currentYearMonth.month }

    val sortedUniqueDates = photoDates.distinct().sortedDescending()
    var currentStreak = 0
    if (sortedUniqueDates.isNotEmpty()) {
        val today = LocalDate.now()
        val latestWorkout = sortedUniqueDates.first()

        if (latestWorkout == today || latestWorkout == today.minusDays(1)) {
            var streakDate = latestWorkout
            for (date in sortedUniqueDates) {
                if (date == streakDate) {
                    currentStreak++
                    streakDate = streakDate.minusDays(1)
                } else {
                    break
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "이번 달 운동일", style = MaterialTheme.typography.bodyMedium)
            Text(text = "$monthlyWorkoutDays 일", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "연속 운동", style = MaterialTheme.typography.bodyMedium)
            Text(text = "$currentStreak 일", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Day(day: CalendarDay, isMarked: Boolean) {
    Box(
        modifier = Modifier
            .aspectRatio(1f),
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
    photoDates: List<LocalDate>
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
            val isMarked = photoDates.contains(day.date)
            Day(day, isMarked)
        },
        monthHeader = { month ->
            MonthHeader(daysOfWeek = month.weekDays.first(), month = month.yearMonth.toString())
        }
    )
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