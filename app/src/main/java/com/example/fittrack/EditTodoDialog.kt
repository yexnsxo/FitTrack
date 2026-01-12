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
    isCompleted: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmStrength: (sets: Int, repsPerSet: Int) -> Unit,
    onConfirmDuration: (sets: Int, minutes: Int) -> Unit,
    onConfirmActualTime: (totalSec: Int) -> Unit = { _ -> }
) {
    val isRepBased = remember(item) {
        item.repsPerSet != null || (item.category == "strength" && item.duration == null)
    }

    val setsState = remember { mutableIntStateOf(item.sets) }
    val repsState = remember { mutableIntStateOf(item.repsPerSet ?: 12) }
    val minutesGoalState = remember { mutableIntStateOf(item.duration ?: 30) }

    val actualMinutesState = remember { mutableIntStateOf(item.actualDurationSec / 60) }
    val actualSecondsState = remember { mutableIntStateOf(item.actualDurationSec % 60) }

    val sets = setsState.intValue
    val reps = repsState.intValue
    val minutesGoal = minutesGoalState.intValue
    val actualMin = actualMinutesState.intValue
    val actualSec = actualSecondsState.intValue

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(26.dp)
        Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = Color.White) {
            Column {
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
                        Text(item.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(if (isCompleted) "운동 소요 시간 수정" else item.description, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isCompleted) {
                        Text("실제 운동 시간", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                // ✅ 세 자릿수(300분 등)를 커버하기 위해 폰트 크기를 20.sp로 조정
                                NumberStepperFieldStepOnly(
                                    value = actualMin,
                                    onValueChange = { actualMinutesState.intValue = it },
                                    min = 0, max = 300, step = 1,
                                    fontSize = 20.sp 
                                )
                            }
                            Text("분", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Box(modifier = Modifier.weight(1f)) {
                                // ✅ 폰트 크기를 20.sp로 조정
                                NumberStepperFieldStepOnly(
                                    value = actualSec,
                                    onValueChange = { actualSecondsState.intValue = it },
                                    min = 0, max = 59, step = 1,
                                    fontSize = 20.sp
                                )
                            }
                            Text("초", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onConfirmActualTime(actualMin * 60 + actualSec) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Main40)
                        ) {
                            Text("소요 시간 저장하기", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Text("세트 수 *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                        NumberStepperFieldStepOnly(value = sets, onValueChange = { setsState.intValue = it }, min = 1, max = 50, step = 1)

                        if (isRepBased) {
                            Text("횟수 (회) *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                            NumberStepperFieldEditable(value = reps, onValueChange = { repsState.intValue = it }, min = 1, max = 200)
                        } else {
                            Text("시간 (분) *", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF111827))
                            NumberStepperFieldStepOnly(value = minutesGoal, onValueChange = { minutesGoalState.intValue = it }, min = 5, max = 300, step = 5)
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { if (isRepBased) onConfirmStrength(sets, reps) else onConfirmDuration(sets, minutesGoal) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Main40)
                        ) {
                            Text("운동 수정하기", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
