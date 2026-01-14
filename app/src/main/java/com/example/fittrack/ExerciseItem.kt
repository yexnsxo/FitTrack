package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fittrack.data.Exercise
import com.example.fittrack.ui.theme.Main40

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onAdd: () -> Unit,
    onDelete: (() -> Unit)? = null, // ✅ 삭제 콜백 추가 (수정은 제거)
    modifier: Modifier = Modifier.padding(vertical = 2.dp)
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardBg = Color(0xFFF2F4F7)
    val kcalBlue = Color(0xFF1A6DED)

    val (diffLabel, diffColor) = when (exercise.difficulty) {
        "beginner" -> "초급" to Color(0xFF16A34A)
        "intermediate" -> "중급" to Color(0xFFF59E0B)
        "advanced" -> "고급" to Color(0xFFEF4444)
        else -> exercise.difficulty to Color(0xFF111827)
    }

    val leftEmoji = when (exercise.category) {
        "strength" -> "💪"
        "cardio" -> "🏃"
        "flexibility" -> "🧘"
        else -> "💪"
    }

    val amountText = when {
        exercise.repsPerSet != null -> "세트: ${exercise.sets ?: 1} / 횟수: ${exercise.repsPerSet}"
        exercise.duration != null -> "세트: ${exercise.sets ?: 1} / 시간: ${exercise.duration}분"
        else -> "세트: ${exercise.sets ?: 1}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(leftEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111827)
                                )
                            ) {
                                append(exercise.name)
                            }

                            append("  ")
                            withStyle(
                                SpanStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = diffColor,
                                    baselineShift = BaselineShift(0.1f)
                                )
                            ) {
                                append(diffLabel)
                            }
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // ✅ 삭제 버튼만 표시 (수정 버튼은 제거됨)
                    if (onDelete != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "delete",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = exercise.description,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = amountText,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = "${exercise.calories.toInt()} kcal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kcalBlue
                    )
                }
            }

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFD1D5DB), CircleShape)
                    .background(Color(0xFFF9FAFB))
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "add",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ExerciseCatalogCard(
    exercises: List<Exercise>,
    onAdd: (Exercise) -> Unit,
    onDeleteCustom: (Exercise) -> Unit, // ✅ 삭제 콜백 유지
    onOpenDirectAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("운동 선택", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)

                Button(
                    onClick = onOpenDirectAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "추가")
                    Spacer(Modifier.width(4.dp))
                    Text("직접 추가")
                }
            }

            exercises.forEach { ex ->
                val isCustom = ex.id.startsWith("custom_")
                ExerciseItem(
                    exercise = ex,
                    onAdd = { onAdd(ex) },
                    onDelete = if (isCustom) { { onDeleteCustom(ex) } } else null // ✅ 커스텀만 삭제 가능
                )
            }

            if (exercises.isEmpty()) {
                Text("해당 카테고리에 운동이 없어요.", color = Color(0xFF777777), fontSize = 14.sp)
            }
        }
    }
}
