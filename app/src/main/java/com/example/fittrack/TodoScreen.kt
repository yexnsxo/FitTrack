package com.example.fittrack

import android.R.attr.layoutDirection
import android.text.TextUtils.isEmpty
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.fittrack.ui.theme.Main40
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import kotlinx.serialization.json.Json
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fittrack.data.Exercise


private suspend fun loadExercisesFromAssets(context: android.content.Context): List<Exercise> =
    withContext(Dispatchers.IO) {
        val jsonText = context.assets.open("exercise_database.json")
            .bufferedReader()
            .use { it.readText() }

        val json = Json { ignoreUnknownKeys = true }
        json.decodeFromString<List<Exercise>>(jsonText)
    }

@Composable
fun TodoScreen(modifier: Modifier = Modifier, vm: TodoViewModel = viewModel(factory = TodoViewModelFactory(LocalContext.current.applicationContext))) {
    val progress by vm.progress.collectAsState()
    val selected by vm.selectedCategory.collectAsState()
    val filteredCatalog by vm.filteredCatalog.collectAsState()
    val todayList by vm.todayList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ProgressOverview(completedCount = progress.completedCount, totalCount = progress.totalCount, caloriesSum = progress.caloriesSum)
        }
        item {
            TodayListCard(
                items = todayList,
                onToggle = vm::toggleCompleted,
                onDelete = vm::deleteTodayRow
            )
        }
        item {
            CategoryCard(
                selected = selected,
                onSelect = vm::selectCategory
            )
        }

        item {
            ExerciseCatalogCard(
                exercises = filteredCatalog,
                onAdd = vm::addExerciseToToday
            )
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProgressOverview(completedCount: Int, totalCount: Int, caloriesSum: Int) {
    Spacer(Modifier.height(10.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(Main40)
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "오늘의 운동",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
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
                        Text(text = "${completedCount}/${totalCount}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                        Text("완료됨", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("$caloriesSum", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text("소모 칼로리", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }

                    PercentProgressRing(
                        completedCount = completedCount,
                        totalCount = totalCount,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
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
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
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