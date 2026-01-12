package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.ui.theme.Main40
import kotlin.math.roundToInt

@Composable
fun EditExerciseDialog(
    item: TodayExerciseEntity,
    onDismiss: () -> Unit,
    onConfirmStrength: (sets: Int, repsPerSet: Int) -> Unit,
    onConfirmDuration: (sets: Int, minutes: Int) -> Unit
) {
    // 횟수 기반 여부를 더 명확하게 판단
    val isRepBased = remember(item) {
        item.repsPerSet != null || (item.category == "strength" && item.duration == null)
    }

    val setsState = remember { mutableIntStateOf(item.sets) }
    val repsState = remember { mutableIntStateOf(item.repsPerSet ?: 12) }
    val minutesState = remember { mutableIntStateOf(item.duration ?: 30) }

    val sets = setsState.intValue
    val reps = repsState.intValue
    val minutes = minutesState.intValue

    val kcalBlue = Color(0xFF1A6DED)

    val kcalPreview = remember(sets, reps, minutes) {
        if (isRepBased) {
            val baseReps = 10.0
            val oldSets = item.sets.coerceAtLeast(1)
            val oldReps = (item.repsPerSet ?: 10).coerceAtLeast(1)
            val baseKcal = item.calories.toDouble() / (oldSets * (oldReps / baseReps))
            (baseKcal * sets * (reps / baseReps)).roundToInt().coerceAtLeast(0)
        } else {
            val oldSets = item.sets.coerceAtLeast(1)
            val oldMin = (item.duration ?: 5).coerceAtLeast(1)
            val kcalPerUnit = item.calories.toDouble() / (oldSets * oldMin)
            (kcalPerUnit * sets * minutes).roundToInt().coerceAtLeast(0)
        }
    }

    val diffLabel = when (item.difficulty) {
        "beginner" -> "초급"
        "intermediate" -> "중급"
        "advanced" -> "고급"
        else -> item.difficulty
    }

    val diffColor = when (item.difficulty) {
        "beginner" -> Color(0xFF16A34A)
        "intermediate" -> Color(0xFFF59E0B)
        "advanced" -> Color(0xFFEF4444)
        else -> Color(0xFF111827)
    }

    val catEmoji = when (item.category) {
        "strength" -> "💪"
        "cardio" -> "🏃"
        "flexibility" -> "🧘"
        else -> "🏋️"
    }
    val catLabel = when (item.category) {
        "strength" -> "근력"
        "cardio" -> "유산소"
        "flexibility" -> "유연성"
        else -> "운동"
    }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(26.dp)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = Color.White
        ) {
            Column {
                // ✅ 헤더: Row와 weight(1f)를 사용하여 글자가 길어지면 줄바꿈되도록 수정
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(Main40)
                        .padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 32.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftPill(text = "$catEmoji  $catLabel", bg = Color.White.copy(alpha = 0.55f), fg = Color(0xFF374151))
                    SoftPill(text = diffLabel, bg = Color.White.copy(alpha = 0.55f), fg = diffColor, bold = true)
                    Text("$kcalPreview kcal", color = kcalBlue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("세트 수 *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                    NumberStepperFieldStepOnly(
                        value = sets,
                        onValueChange = { setsState.intValue = it },
                        min = 1,
                        max = 50,
                        step = 1
                    )

                    if (isRepBased) {
                        Text("횟수 (회) *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                        NumberStepperFieldEditable(
                            value = reps,
                            onValueChange = { repsState.intValue = it },
                            min = 1,
                            max = 200
                        )
                    } else {
                        Text("시간 (분) *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                        NumberStepperFieldStepOnly(
                            value = minutes,
                            onValueChange = { minutesState.intValue = it },
                            min = 5,
                            max = 300,
                            step = 5
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (isRepBased) onConfirmStrength(sets, reps) else onConfirmDuration(sets, minutes)
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                    ) {
                        Text("운동 수정하기", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
