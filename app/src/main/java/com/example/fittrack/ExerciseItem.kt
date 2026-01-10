package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fittrack.data.Exercise
import com.example.fittrack.ui.theme.Main40

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier.padding(vertical = 2.dp)
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardBg = Color(0xFFF2F4F7) // 사진처럼 연한 회색
    val kcalBlue = Color(0xFF1A6DED)

    val (diffLabel, diffColor) = when (exercise.difficulty) {
        "beginner" -> "초급" to Color(0xFF16A34A)       // green
        "intermediate" -> "중급" to Color(0xFFF59E0B)  // amber
        "advanced" -> "고급" to Color(0xFFEF4444)       // red
        else -> exercise.difficulty to Color(0xFF111827)
    }

    val leftEmoji = when (exercise.category) {
        "strength" -> "💪"
        "cardio" -> "🏃"
        "flexibility" -> "🧘"
        else -> "💪"
    }

    val amountText = when {
        exercise.sets != null -> "세트: ${exercise.sets}"
        exercise.duration != null -> "시간: ${exercise.duration}분"
        else -> ""
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
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 아이콘 영역
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(leftEmoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            // 가운데 영역
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // (1) 이름 + 난이도
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = diffLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = diffColor
                    )
                }

                // (2) 설명
                Text(
                    text = exercise.description,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                // (3) 세트/시간 + kcal
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (amountText.isNotBlank()) {
                        Text(
                            text = amountText,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(Modifier.width(14.dp))
                    }

                    Text(
                        text = "${exercise.calories} kcal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kcalBlue
                    )
                }
            }

            // 오른쪽 + 버튼
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFD1D5DB), CircleShape)
                    .background(Color(0xFFF9FAFB))
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "add",
                    tint = Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
fun ExerciseCatalogCard(
    exercises: List<Exercise>,
    onAdd: (Exercise) -> Unit
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
                    onClick = { /* TODO: 직접 추가 다이얼로그 */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "추가")
                    Spacer(Modifier.width(4.dp))
                    Text("직접 추가")
                }
            }

            exercises.forEach { ex ->
                ExerciseItem(exercise = ex, onAdd = { onAdd(ex) })
            }

            if (exercises.isEmpty()) {
                Text("해당 카테고리에 운동이 없어요.", color = Color(0xFF777777), fontSize = 14.sp)
            }
        }
    }
}