package com.example.fittrack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fittrack.data.Exercise
import com.example.fittrack.ui.theme.Main40
import java.util.UUID

@Composable
fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit,
    initialExercise: Exercise? = null, // ✅ 수정 모드 지원
    onDelete: ((Exercise) -> Unit)? = null // ✅ 삭제 기능 지원
) {
    val shape = RoundedCornerShape(26.dp)

    val nameState = remember { mutableStateOf(initialExercise?.name ?: "") }
    val descState = remember { mutableStateOf(initialExercise?.description ?: "") }

    val categoryState = remember { mutableStateOf(initialExercise?.category ?: "strength") }
    val difficultyState = remember { mutableStateOf(initialExercise?.difficulty ?: "beginner") }

    val setsState = remember { mutableIntStateOf(initialExercise?.sets ?: 1) }
    val repsState = remember { mutableIntStateOf(initialExercise?.repsPerSet ?: 10) }
    val durationState = remember { mutableIntStateOf(initialExercise?.duration ?: 5) }
    // ✅ 기본 칼로리를 50에서 5로 하향 조정하고, 소수점 유지하도록 수정
    val caloriesState = remember { mutableStateOf(initialExercise?.calories?.toString()?.removeSuffix(".0") ?: "5") }

    // ✅ 기록 방식: reps/time
    val recordMode = remember {
        mutableStateOf(if (initialExercise?.duration != null) "time" else "reps")
    }

    val caloriesDouble = caloriesState.value.toDoubleOrNull()
    val canSubmit = nameState.value.isNotBlank() && caloriesDouble != null && caloriesDouble > 0.0

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = shape,
            color = Color.White
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                // 헤더
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Main40)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = if (initialExercise == null) "운동 직접 추가" else "운동 정보 수정",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                val buttonFocusRequester = remember { FocusRequester() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xffF8F8F8),
                        focusedBorderColor = Main40,
                        unfocusedBorderColor = Color(0xFFE7E7E7)
                    )
                    // 운동 이름
                    Text("운동 이름 *", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    OutlinedTextField(
                        value = nameState.value,
                        onValueChange = { nameState.value = it },
                        placeholder = { Text("예: 팔벌려뛰기") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = colors
                    )

                    // 카테고리
                    Text("카테고리 *", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryChip2("💪", "근력",
                            selected = categoryState.value == "strength",
                            onClick = { categoryState.value = "strength" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryChip2("🏃", "유산소",
                            selected = categoryState.value == "cardio",
                            onClick = { categoryState.value = "cardio" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryChip2("🧘", "유연성",
                            selected = categoryState.value == "flexibility",
                            onClick = { categoryState.value = "flexibility" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 난이도
                    Text("난이도", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DifficultyChip2("초급",
                            selected = difficultyState.value == "beginner",
                            onClick = { difficultyState.value = "beginner" },
                            modifier = Modifier.weight(1f)
                        )
                        DifficultyChip2("중급",
                            selected = difficultyState.value == "intermediate",
                            onClick = { difficultyState.value = "intermediate" },
                            modifier = Modifier.weight(1f)
                        )
                        DifficultyChip2("고급",
                            selected = difficultyState.value == "advanced",
                            onClick = { difficultyState.value = "advanced" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ✅ 기록 방식 (카테고리와 무관)
                    Text("기록 방식", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleChip(
                            text = "횟수",
                            selected = recordMode.value == "reps",
                            onClick = { recordMode.value = "reps" },
                            modifier = Modifier.weight(1f)
                        )
                        ToggleChip(
                            text = "시간",
                            selected = recordMode.value == "time",
                            onClick = { recordMode.value = "time" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ✅ 세트는 항상 활성화 + 두번째 필드는 recordMode에 따라 라벨/값 변경
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StepperNumberField(
                            label = "세트",
                            value = setsState.intValue,
                            min = 1,
                            max = 50,
                            onValueChange = { setsState.intValue = it },
                            modifier = Modifier.weight(1f)
                        )

                        if (recordMode.value == "reps") {
                            StepperNumberField(
                                label = "횟수",
                                value = repsState.intValue,
                                min = 1,
                                max = 200,
                                onValueChange = { repsState.intValue = it },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            StepperNumberField(
                                label = "시간(분)",
                                value = durationState.intValue,
                                min = 1,
                                max = 300,
                                onValueChange = { durationState.intValue = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 칼로리는 Double이라 기존 TextField 유지(원하면 stepper도 만들 수 있음)
                        SmallNumberField(
                            label = "칼로리",
                            value = caloriesState.value,
                            enabled = true,
                            onValueChange = { caloriesState.value = it },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 설명
                    Text("설명", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = descState.value,
                        onValueChange = { descState.value = it },
                        placeholder = { Text("예: 체력 증진") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        colors = colors,
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = {
                                val ex = Exercise(
                                    id = initialExercise?.id ?: ("custom_" + UUID.randomUUID().toString()),
                                    name = nameState.value.trim(),
                                    category = categoryState.value,

                                    // ✅ 세트는 항상 저장
                                    sets = setsState.intValue,

                                    // ✅ 기록 방식에 따라 repsPerSet vs duration 저장
                                    repsPerSet = if (recordMode.value == "reps") repsState.intValue else null,
                                    duration = if (recordMode.value == "time") durationState.intValue else null,

                                    calories = caloriesDouble ?: 0.0,
                                    difficulty = difficultyState.value,
                                    description = descState.value.trim()
                                )
                                onConfirm(ex)
                            },
                            enabled = canSubmit,
                            modifier = Modifier
                                .weight(if (initialExercise != null) 2f else 1f)
                                .height(56.dp)
                                .focusRequester(buttonFocusRequester) // 여기에 포커스를 받게 설정
                                .focusGroup(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canSubmit) Main40 else Color(0xFFE5E7EB),
                                contentColor = if (canSubmit) Color.White else Color(0xFF9CA3AF)
                            )
                        ) {
                            Text(
                                if (initialExercise == null) "운동 목록에 추가" else "변경 사항 저장",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperNumberField(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    val bgColor = if (isFocused.value) Color.White else Color(0xffF8F8F8)
    val borderColor = if (isFocused.value) Main40 else Color(0xFFE7E7E7)

    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp) // ✅ 높이 줄임 (기존 64dp)
                .clip(shape)
                .background(bgColor)
                .border(2.dp, borderColor, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }
                        if (filtered.isEmpty()) {
                            onValueChange(min)
                            return@BasicTextField
                        }
                        val n = filtered.toIntOrNull() ?: return@BasicTextField
                        onValueChange(n.coerceIn(min, max))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused.value = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 16.sp, // ✅ 글자 크기 줄임 (기존 24sp)
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )
                )
            }

            StepperButtons(
                onUp = {
                    onValueChange((value + 1).coerceAtMost(max))
                    focusRequester.requestFocus()
                },
                onDown = {
                    onValueChange((value - 1).coerceAtLeast(min))
                    focusRequester.requestFocus()
                }
            )
        }
    }
}

@Composable
private fun StepperButtons(
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 32.dp, height = 42.dp) // ✅ 버튼 영역 크기 줄임 (기존 36x48)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(20.dp)) { // ✅ 아이콘 버튼 크기 줄임 (기존 24)
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "증가", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDown, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "감소", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoryChip2(
    emoji: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Main40 else Color(0xFFF1F5F9)
    val fg = if (selected) Color.White else Color(0xFF111827)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .height(48.dp)
            .clickable { onClick() }
        ,
        contentAlignment = Alignment.Center
    ) {
        Text("$emoji $text", color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DifficultyChip2(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Main40 else Color(0xFFF1F5F9)
    val fg = if (selected) Color.White else Color(0xFF111827)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallNumberField(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xffF8F8F8),
        focusedBorderColor = Main40,
        unfocusedBorderColor = Color(0xFFE7E7E7)
    )

    Column(
        modifier = modifier
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp) // ✅ 높이 줄임 (기존 64dp)
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(18.dp),
            colors = colors,
            textStyle = TextStyle(
                fontSize = 16.sp, // ✅ 글자 크기 줄임 (기존 18sp)
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Main40 else Color(0xFFF1F5F9)
    val fg = if (selected) Color.White else Color(0xFF111827)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
