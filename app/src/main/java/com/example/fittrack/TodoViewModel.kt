package com.example.fittrack

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.Exercise
import com.example.fittrack.data.FitTrackDatabase
import com.example.fittrack.data.PhotoDao
import com.example.fittrack.data.PhotoDatabase
import com.example.fittrack.data.PhotoRepository
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.data.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

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
    private val repo: TodoRepository,
    private val photoDao: PhotoDao
) : ViewModel() {

    private val todayKey: String = repo.todayKey()

    private val _catalog = MutableStateFlow<List<Exercise>>(emptyList())
    val catalog: StateFlow<List<Exercise>> = _catalog.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()

    val todayList: StateFlow<List<TodayExerciseEntity>> =
        repo.observeToday(todayKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredCatalog: StateFlow<List<Exercise>> =
        combine(catalog, selectedCategory) { list, cat ->
            if (cat == CategoryFilter.ALL) list else list.filter { it.category == cat.key }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    val isTodayPhotoSaved: StateFlow<Boolean> =
        photoDao.getAllPhotos()
            .map { photos ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = cal.timeInMillis

                photos.any { it.createdAt in startOfDay until endOfDay }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)


    init {
        viewModelScope.launch {
            repo.cleanupNotToday(todayKey)
            _catalog.value = repo.loadCatalogFromAssets()
        }
    }

    fun selectCategory(cat: CategoryFilter) {
        _selectedCategory.value = cat
    }

    // ✅ 모달에서 선택한 값으로 추가
    fun addExerciseToTodayWithSelection(
        ex: Exercise,
        sets: Int,
        repsPerSet: Int
    ) {
        viewModelScope.launch {
            val calories = calcCalories(ex, sets = sets, repsPerSet = repsPerSet, durationMin = null)
            repo.addToToday(
                ex = ex,
                dateKey = todayKey,
                sets = sets,
                repsPerSet = repsPerSet,
                duration = null,
                calories = calories
            )
        }
    }

    // ✅ 유산소/유연성용(원하면 사용)
    fun addExerciseToTodayWithDuration(ex: Exercise, durationMin: Int) {
        viewModelScope.launch {
            val calories = calcCalories(ex, sets = null, repsPerSet = null, durationMin = durationMin)
            repo.addToToday(
                ex = ex,
                dateKey = todayKey,
                sets = null,
                repsPerSet = null,
                duration = durationMin,
                calories = calories
            )
        }
    }

    // ✅ 칼로리 계산 (기준이 불명확해서 선형 스케일로 구현)
    // - strength: ex.calories 를 "10회 1세트 기준"으로 보고 sets, reps에 비례
    // - cardio/flex: ex.calories 를 "ex.duration 분 기준"으로 보고 duration에 비례
    private fun calcCalories(
        ex: Exercise,
        sets: Int?,
        repsPerSet: Int?,
        durationMin: Int?
    ): Int {
        return when (ex.category) {
            "strength" -> {
                val baseReps = 10.0
                val s = (sets ?: 1).toDouble()
                val r = (repsPerSet ?: 10).toDouble()
                (ex.calories * s * (r / baseReps)).roundToInt()
            }
            else -> {
                val baseMin = (ex.duration ?: 5).toDouble()
                val m = (durationMin ?: (ex.duration ?: 5)).toDouble()
                (ex.calories * (m / baseMin)).roundToInt()
            }
        }.coerceAtLeast(0)
    }

    fun toggleCompleted(rowId: Long, checked: Boolean) {
        viewModelScope.launch { repo.setCompleted(rowId, checked) }
    }

    fun deleteTodayRow(rowId: Long) {
        viewModelScope.launch { repo.deleteRow(rowId) }
    }

    fun updateTodayRowStrength(item: TodayExerciseEntity, sets: Int, reps: Int) {
        viewModelScope.launch {
            val base = catalog.value.firstOrNull { it.id == item.exerciseId }

            val kcal = if (base != null) {
                val baseReps = 10.0
                (base.calories * sets * (reps / baseReps)).roundToInt()
            } else {
                // base 못 찾으면 기존 칼로리 유지(fallback)
                item.calories
            }

            repo.updateTodayAmounts(
                rowId = item.rowId,
                sets = sets,
                repsPerSet = reps,
                duration = null,
                calories = kcal
            )
        }
    }

    fun updateTodayRowDuration(item: TodayExerciseEntity, minutes: Int) {
        viewModelScope.launch {
            val base = catalog.value.firstOrNull { it.id == item.exerciseId }

            val kcal = if (base != null) {
                val baseMin = (base.duration ?: 5).toDouble()
                (base.calories * (minutes / baseMin)).roundToInt()
            } else {
                item.calories
            }

            repo.updateTodayAmounts(
                rowId = item.rowId,
                sets = null,
                repsPerSet = null,
                duration = minutes,
                calories = kcal
            )
        }
    }
}

class TodoViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = FitTrackDatabase.getInstance(appContext)
        val photoDb = PhotoDatabase.getDatabase(appContext)
        val photoRepository = PhotoRepository(photoDb.photoDao())
        val repo = TodoRepository(appContext, db.todayExerciseDao(), photoRepository)
        @Suppress("UNCHECKED_CAST")
        return TodoViewModel(repo, photoDb.photoDao()) as T
    }
}
