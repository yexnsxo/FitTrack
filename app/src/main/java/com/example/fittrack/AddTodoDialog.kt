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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fittrack.data.Exercise
import com.example.fittrack.ui.theme.Main40
import kotlin.math.roundToInt

@Composable
fun AddExerciseDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirmStrength: (sets: Int, repsPerSet: Int) -> Unit,
    onConfirmDuration: (sets: Int, minutes: Int) -> Unit
) {
    // ✅ 횟수 기반인지 시간 기반인지 판단 로직 개선
    // 1. repsPerSet이 있으면 횟수 기반
    // 2. duration이 있으면 시간 기반
    // 3. 둘 다 없으면 카테고리가 strength인 경우 횟수 기반으로 간주
    val isRepBased = remember(exercise) {
        when {
            exercise.repsPerSet != null -> true
            exercise.duration != null -> false
            exercise.category == "strength" -> true
            else -> false
        }
    }

    val setsState = remember { mutableIntStateOf(3) }
    val repsState = remember { mutableIntStateOf(exercise.repsPerSet ?: 12) }
    val minutesState = remember { mutableIntStateOf(exercise.duration ?: 30) }

    val sets = setsState.intValue
    val reps = repsState.intValue
    val minutes = minutesState.intValue

    val kcalBlue = Color(0xFF1A6DED)

    val kcal = remember(exercise, sets, reps, minutes) {
        if (isRepBased) {
            val baseReps = 10.0
            (exercise.calories * sets * (reps / baseReps)).roundToInt()
        } else {
            val baseMin = (exercise.duration ?: 5).toDouble()
            (exercise.calories * sets * (minutes / baseMin)).roundToInt()
        }.coerceAtLeast(0)
    }

    val diffLabel = when (exercise.difficulty) {
        "beginner" -> "초급"
        "intermediate" -> "중급"
        "advanced" -> "고급"
        else -> exercise.difficulty
    }

    val diffColor = when (exercise.difficulty) {
        "beginner" -> Color(0xFF16A34A)
        "intermediate" -> Color(0xFFF59E0B)
        "advanced" -> Color(0xFFEF4444)
        else -> Color(0xFF111827)
    }

    val catEmoji = when (exercise.category) {
        "strength" -> "💪"
        "cardio" -> "🏃"
        "flexibility" -> "🧘"
        else -> "🏋️"
    }

    val catLabel = when (exercise.category) {
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
                            text = exercise.name,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 32.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = exercise.description,
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

                // pill row
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftPill(
                        text = "$catEmoji  $catLabel",
                        bg = Color.White.copy(alpha = 0.55f),
                        fg = Color(0xFF374151)
                    )
                    SoftPill(
                        text = diffLabel,
                        bg = Color.White.copy(alpha = 0.55f),
                        fg = diffColor,
                        bold = true
                    )
                    Text(
                        text = "$kcal kcal",
                        color = kcalBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(18.dp))

                // 입력 영역
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 모든 운동에서 세트 수는 필수
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
                            max = 200,
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
                            if (isRepBased) onConfirmStrength(sets, reps)
                            else onConfirmDuration(sets, minutes)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main40, contentColor = Color.White)
                    ) {
                        Text("운동 추가하기", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun SoftPill(
    text: String,
    bg: Color,
    fg: Color,
    bold: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE5E7EB), shape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 16.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
fun NumberStepperFieldEditable(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.75f))
            .border(2.dp, Color(0xFFE5E7EB), shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ 세트 수와 동일한 "값 영역" (오른쪽 스텝퍼 제외한 공간)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value.toString(),
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }
                    if (filtered.isBlank()) return@BasicTextField
                    val n = filtered.toIntOrNull() ?: return@BasicTextField
                    onValueChange(n.coerceIn(min, max))
                },
                modifier = Modifier.fillMaxWidth(), // ✅ textAlign이 제대로 먹게
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center // ✅ 값 영역 기준 가운데
                )
            )
        }

        StepperButtons(
            onUp = { onValueChange((value + 1).coerceAtMost(max)) },
            onDown = { onValueChange((value - 1).coerceAtLeast(min)) }
        )
    }
}

@Composable
fun NumberStepperFieldStepOnly(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.75f))
            .border(2.dp, Color(0xFFE5E7EB), shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )
        }

        StepperButtons(
            onUp = { onValueChange((value + step).coerceAtMost(max)) },
            onDown = { onValueChange((value - step).coerceAtLeast(min)) }
        )
    }
}

@Composable
private fun StepperButtons(
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 42.dp, height = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "증가", tint = Color(0xFF374151))
        }
        IconButton(onClick = onDown, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "감소", tint = Color(0xFF374151))
        }
    }
}
