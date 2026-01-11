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

    val customCatalog: StateFlow<List<Exercise>> =
        repo.observeCustomCatalog()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()

    val todayList: StateFlow<List<TodayExerciseEntity>> =
        repo.observeToday(todayKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val catalogAll: StateFlow<List<Exercise>> =
        combine(catalog, customCatalog) { base, custom ->
            (base + custom).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredCatalog: StateFlow<List<Exercise>> =
        combine(catalogAll, selectedCategory) { list, cat ->
            if (cat.key == null) list else list.filter { it.category == cat.key }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<ProgressUi> =
        todayList
            .map { list ->
                val completed = list.filter { it.isCompleted }
                ProgressUi(
                    completedCount = completed.size,
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

    fun addCustomExerciseToCatalog(ex: Exercise) {
        viewModelScope.launch {
            repo.upsertCustomExercise(ex)
        }
    }

    fun updateCustomExercise(ex: Exercise) {
        viewModelScope.launch {
            repo.upsertCustomExercise(ex)
        }
    }

    fun deleteCustomExercise(ex: Exercise) {
        viewModelScope.launch {
            repo.deleteCustomExercise(ex.id)
        }
    }

    fun addExerciseToTodayWithSelection(ex: Exercise, sets: Int, repsPerSet: Int) {
        viewModelScope.launch {
            val calories = calcCalories(ex, sets = sets, repsPerSet = repsPerSet, durationMin = null)
            repo.addToToday(ex, todayKey, sets, repsPerSet, null, calories)
        }
    }

    fun addExerciseToTodayWithDuration(ex: Exercise, sets: Int, durationMin: Int) {
        viewModelScope.launch {
            val calories = calcCalories(ex, sets = sets, repsPerSet = null, durationMin = durationMin)
            repo.addToToday(ex, todayKey, sets, null, durationMin, calories)
        }
    }

    private fun calcCalories(ex: Exercise, sets: Int?, repsPerSet: Int?, durationMin: Int?): Int {
        val s = (sets ?: 1).toDouble()
        return if (repsPerSet != null || (ex.category == "strength" && durationMin == null)) {
            val baseReps = 10.0
            val r = (repsPerSet ?: 10).toDouble()
            (ex.calories * s * (r / baseReps)).roundToInt()
        } else {
            val baseMin = (ex.duration ?: 5).toDouble()
            val m = (durationMin ?: (ex.duration ?: 5)).toDouble()
            (ex.calories * s * (m / baseMin)).roundToInt()
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
            val base = catalogAll.value.firstOrNull { it.id == item.exerciseId }
            val kcal = if (base != null) calcCalories(base, sets, reps, null) else item.calories
            repo.updateTodayAmounts(item.rowId, sets, reps, null, kcal)
        }
    }

    fun updateTodayRowDuration(item: TodayExerciseEntity, sets: Int, minutes: Int) {
        viewModelScope.launch {
            val base = catalogAll.value.firstOrNull { it.id == item.exerciseId }
            val kcal = if (base != null) calcCalories(base, sets, null, minutes) else item.calories
            repo.updateTodayAmounts(item.rowId, sets, null, minutes, kcal)
        }
    }
}

class TodoViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = FitTrackDatabase.getInstance(appContext)
        val photoDb = PhotoDatabase.getDatabase(appContext)
        val photoRepo = PhotoRepository(photoDb.photoDao())
        
        val repo = TodoRepository(
            appContext,
            db.todayExerciseDao(),
            db.customExerciseDao(),
            photoRepo
        )
        
        @Suppress("UNCHECKED_CAST")
        return TodoViewModel(repo, photoDb.photoDao()) as T
    }
}
