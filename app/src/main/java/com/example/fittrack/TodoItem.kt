package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fittrack.data.TodayExerciseEntity
import kotlin.collections.forEach

fun Modifier.dashedBorder(
    strokeWidth: Dp,
    color: Color,
    shape: Shape,
    on: Dp = 8.dp,
    off: Dp = 6.dp
): Modifier = this.drawWithContent {
    drawContent()

    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(on.toPx(), off.toPx()),
            0f
        )
    )

    drawOutline(outline = outline, color = color, style = stroke)
}


@Composable
fun TodayListCard(
    items: List<TodayExerciseEntity>,
    onToggle: (TodayExerciseEntity, Boolean) -> Unit,
    onDelete: (TodayExerciseEntity) -> Unit,
    onEditStrength: (TodayExerciseEntity, Int, Int) -> Unit,     // sets, reps
    onEditDuration: (TodayExerciseEntity, Int, Int) -> Unit,      // sets, minutes
    onTimerClick: (TodayExerciseEntity) -> Unit = {},
    onEditActualTime: (TodayExerciseEntity, Int) -> Unit = { _, _ -> }, // ✅ 실제 시간 수정용 콜백
    onEditSetInfo: (TodayExerciseEntity) -> Unit
) {
    Text("오늘의 운동 목록", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Spacer(Modifier.height(8.dp))

    val shape = RoundedCornerShape(18.dp)
    val isEmpty = items.isEmpty()

    if (isEmpty) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(
                    strokeWidth = 2.dp,
                    color = Color(0xFFCBD5E1),
                    shape = shape
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏋️‍♂️", fontSize = 50.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "아래 운동 리스트에서 선택해보세요!",
                        color = Color(0xFF777777),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "+ 버튼을 눌러 오늘의 운동에 추가할 수 있어요",
                        color = Color(0xFF777777),
                        fontSize = 10.sp
                    )
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            TodayRow(
                item = item,
                onToggle = { checked -> onToggle(item, checked) },
                onDelete = { onDelete(item) },
                onEditStrength = { sets, reps -> onEditStrength(item, sets, reps) },
                onEditDuration = { sets, minutes -> onEditDuration(item, sets, minutes) },
                onTimerClick = { onTimerClick(item) },
                onEditActualTime = { totalSec -> onEditActualTime(item, totalSec) },
                onEditSetInfo = { onEditSetInfo(item) }
            )
        }
    }
}

@Composable
private fun TodayRow(
    item: TodayExerciseEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEditStrength: (sets: Int, reps: Int) -> Unit,
    onEditDuration: (sets: Int, minutes: Int) -> Unit,
    onTimerClick: () -> Unit,
    onEditActualTime: (Int) -> Unit,
    onEditSetInfo: () -> Unit
) {
    val selected = item.isCompleted
    val editOpen = remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(22.dp)
    val borderColor = if (selected) Color(0xFF2F6BFF) else Color(0xFFE5E7EB)
    val shadowElevation = if (selected) 10.dp else 8.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = shadowElevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor, shape = cardShape)
                .padding(start = 18.dp, top = 18.dp, end = 8.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleCheck(checked = selected, onClick = { onToggle(!selected) })
                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.width(1.5.dp))
                        CategoryPill(
                            emoji = when (item.category) {
                                "strength" -> "💪"
                                "cardio" -> "🏃"
                                "flexibility" -> "🧘"
                                else -> "🏋️"
                            },
                            text = when (item.category) {
                                "strength" -> "근력"
                                "cardio" -> "유산소"
                                "flexibility" -> "유연성"
                                else -> "운동"
                            }
                        )

                        val isTimeBased = item.category == "cardio" || item.category == "flexibility" || item.duration != null

                        TimerPill(
                            actualSec = item.actualDurationSec,
                            onClick = { if (selected) editOpen.value = true else onTimerClick() },
                            isTimeBased = isTimeBased
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isTimeBased = item.category == "cardio" || item.category == "flexibility" || item.duration != null

                        if (isTimeBased) {
                            if (item.actualDurationSec > 0) {
                                val mins = item.actualDurationSec / 60
                                val secs = item.actualDurationSec % 60
                                val timeDisplay = when {
                                    mins > 0 && secs > 0 -> "${mins}분 ${secs}초"
                                    mins > 0 -> "${mins}분"
                                    else -> "${secs}초"
                                }

                                Text(
                                    text = "수행: $timeDisplay",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.clickable { if (selected) editOpen.value = true }
                                )
                            } else {
                                val totalGoalMinutes = item.sets * (item.duration ?: 0)
                                Text(
                                    text = "목표: ${totalGoalMinutes}분",
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            if (item.actualReps > 0) {
                                Text(
                                    text = "수행: ${item.sets}세트 (총 ${item.actualReps}회)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.clickable { onEditSetInfo() }
                                )
                            } else {
                                Text(
                                    text = "${item.sets}세트 ${item.repsPerSet ?: 0}회",
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "${item.calories} kcal",
                            color = Color(0xFF2563EB),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!selected) {
                    val iconBtnSize = 38.dp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        IconButton(
                            onClick = { editOpen.value = true },
                            modifier = Modifier.size(iconBtnSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "수정",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(iconBtnSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "삭제",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (editOpen.value) {
        EditExerciseDialog(
            item = item,
            isCompleted = selected,
            onDismiss = { editOpen.value = false },
            onConfirmStrength = { sets, reps ->
                onEditStrength(sets, reps)
                editOpen.value = false
            },
            onConfirmDuration = { sets, minutes ->
                onEditDuration(sets, minutes)
                editOpen.value = false
            },
            onConfirmActualTime = { totalSec ->
                onEditActualTime(totalSec)
                editOpen.value = false
            }
        )
    }
}

@Composable
private fun TimerPill(    actualSec: Int,
                          onClick: () -> Unit,
                          isTimeBased: Boolean = true
) {
    val hasTimeRecord = actualSec > 0
    val bgColor = if (hasTimeRecord) Color(0xFFEEF2FF) else Color(0xFFF3F4F6)
    val contentColor = if (hasTimeRecord) Color(0xFF4F46E5) else Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = "타이머",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )

            if (hasTimeRecord) {
                Spacer(Modifier.width(4.dp))
                val mins = actualSec / 60
                val secs = actualSec % 60

                val timeText = when {
                    mins > 0 && secs > 0 -> "${mins}분 ${secs}초"
                    mins > 0 -> "${mins}분"
                    else -> "${secs}초"
                }

                Text(
                    text = timeText,
                    fontSize = 14.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



@Composable
private fun CircleCheck(
    checked: Boolean,
    onClick: () -> Unit
) {
    val size = 34.dp
    val bg = if (checked) Color(0xFF2F6BFF) else Color.Transparent
    val border = if (checked) Color(0xFF2F6BFF) else Color(0xFFD1D5DB)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, border, CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "완료",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CategoryPill(emoji: String, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Text(text, fontSize = 14.sp, color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
        }
    }
}
