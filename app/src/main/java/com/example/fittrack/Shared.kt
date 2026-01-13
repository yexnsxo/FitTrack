package com.example.fittrack

enum class CategoryFilter(val key: String?, val label: String, val emoji: String) {
    ALL(null, "전체", "🏋️"),
    CUSTOM("custom", "커스텀", "⭐️"),
    STRENGTH("strength", "근력", "💪"),
    CARDIO("cardio", "유산소", "🏃"),
    FLEXIBILITY("flexibility", "유연성", "🧘");
}

data class Progress(
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val caloriesSum: Int = 0,
    val totalDurationSec: Int = 0
)
