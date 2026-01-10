package com.example.fittrack

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.Exercise
import com.example.fittrack.data.FitTrackDatabase
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.data.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CategoryFilter(val key: String?, val label: String, val emoji: String) {
    ALL(null, "전체", "🏋️"),
    STRENGTH("strength", "근력", "💪"),
    CARDIO("cardio", "유산소", "🏃"),
    FLEXIBILITY("flexibility", "유연성", "🧘")
}

data class ProgressUi(
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val caloriesSum: Int = 0
)

class TodoViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    private val todayKey: String = repo.todayKey()

    private val _catalog = MutableStateFlow<List<Exercise>>(emptyList())
    val catalog: StateFlow<List<Exercise>> = _catalog.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()

    // 오늘 운동 목록 (Room)
    val todayList: StateFlow<List<TodayExerciseEntity>> =
        repo.observeToday(todayKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 필터링된 Catalog(전체, 근력, 유산소, 유연성)
    val filteredCatalog: StateFlow<List<Exercise>> =
        combine(catalog, selectedCategory) { list, cat ->
            if (cat == CategoryFilter.ALL) list
            else list.filter { it.category == cat.key }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ProgressOverview 컴포넌트에 필요한 데이터
    val progress: StateFlow<ProgressUi> =
        todayList
            .map { list ->
                val completed = list.filter { it.isCompleted }
                ProgressUi(
                    completedCount = list.count { it.isCompleted },
                    totalCount = list.size,
                    caloriesSum = completed.sumOf { it.calories }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())

    init {
        viewModelScope.launch {
            // 다음날 상태 초기화
            repo.cleanupNotToday(todayKey)

            _catalog.value = repo.loadCatalogFromAssets()
        }
    }

    fun selectCategory(cat: CategoryFilter) {
        _selectedCategory.value = cat
    }

    fun addExerciseToToday(ex: Exercise) {
        viewModelScope.launch {
            repo.addToToday(ex, todayKey)
        }
    }

    fun toggleCompleted(rowId: Long, checked: Boolean) {
        viewModelScope.launch {
            repo.setCompleted(rowId, checked)
        }
    }

    fun deleteTodayRow(rowId: Long) {
        viewModelScope.launch {
            repo.deleteRow(rowId)
        }
    }
}

class TodoViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = FitTrackDatabase.getInstance(appContext)
        val repo = TodoRepository(appContext, db.todayExerciseDao())
        @Suppress("UNCHECKED_CAST")
        return TodoViewModel(repo) as T
    }
}