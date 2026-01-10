package com.example.fittrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val photoDates by viewModel.photoDates.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { viewModel.addPhoto(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addPhoto(it)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageFile = createImageFile(context)
            val newImageUri = FileProvider.getUriForFile(context, "com.example.fittrack.provider", newImageFile)
            imageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            // Handle permission denial
        }
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            RecordCalendar(photoDates = photoDates)

            // 새로 추가된 운동 현황판
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

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Choose an option") },
                    text = { Text("Would you like to take a picture or choose from the gallery?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDialog = false
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        ) {
                            Text("Camera")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showDialog = false
                                galleryLauncher.launch("image/*")
                            }
                        ) {
                            Text("Gallery")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WorkoutStats(photoDates: List<LocalDate>) {
    // 이번 달 운동일 계산
    val cal = Calendar.getInstance()
    val currentYearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    // BUG FIX: Count distinct days, not all entries
    val monthlyWorkoutDays = photoDates.distinct().count { it.year == currentYearMonth.year && it.month == currentYearMonth.month }

    // 연속 운동일수 계산
    val sortedUniqueDates = photoDates.distinct().sortedDescending()
    var currentStreak = 0
    if (sortedUniqueDates.isNotEmpty()) {
        val today = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val latestWorkout = sortedUniqueDates.first()

        // 오늘 또는 어제 운동을 했을 경우에만 연속 운동으로 인정
        if (latestWorkout == today || latestWorkout == today.minusDays(1)) {
            var streakDate = latestWorkout
            for (date in sortedUniqueDates) {
                if (date == streakDate) {
                    currentStreak++
                    streakDate = streakDate.minusDays(1)
                } else {
                    // 연속이 끊기는 순간 종료
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
            .aspectRatio(1f), // This is important for square cells.
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
    val currentMonth = remember {
        val cal = Calendar.getInstance()
        YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
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
