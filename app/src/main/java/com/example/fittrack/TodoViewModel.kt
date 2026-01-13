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
import java.time.LocalDate
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
    val caloriesSum: Int = 0,
    val totalDurationSec: Int = 0
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
                    caloriesSum = completed.sumOf { it.calories },
                    totalDurationSec = completed.sumOf { it.actualDurationSec }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())

    val isTodayPhotoSaved: StateFlow<Boolean> =
        photoDao.getAllPhotos()
            .map { photos ->
                photos.any { it.date == todayKey }
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

    fun deleteCustomExercise(ex: Exercise) {
        viewModelScope.launch {
            repo.deleteCustomExercise(ex.id)
        }
    }

    // ✅ 특정 날짜의 루틴을 오늘로 복사
    fun copyRoutineToToday(dateKey: String) {
        viewModelScope.launch {
            val pastExercises = repo.getTodayOnce(dateKey)
            pastExercises.forEach { past ->
                val item = TodayExerciseEntity(
                    dateKey = todayKey, // 오늘 날짜로 설정
                    exerciseId = past.exerciseId,
                    name = past.name,
                    category = past.category,
                    sets = past.sets,
                    repsPerSet = past.repsPerSet,
                    duration = past.duration,
                    calories = past.calories,
                    difficulty = past.difficulty,
                    description = past.description,
                    isCompleted = false // 오늘 할 일이므로 미완료 상태로 추가
                )
                repo.addTodayExercise(item)
            }
        }
    }

    fun addExerciseToDateWithSelection(dateKey: String, ex: Exercise, sets: Int, repsPerSet: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, repsPerSet = repsPerSet, durationMin = null)
            val repsString = List(sets) { repsPerSet.toString() }.joinToString(",")
            val totalReps = sets * repsPerSet
            
            val item = TodayExerciseEntity(
                dateKey = dateKey,
                exerciseId = ex.id,
                name = ex.name,
                category = ex.category,
                sets = sets,
                repsPerSet = repsPerSet,
                actualReps = totalReps,
                setReps = repsString,
                calories = calories,
                difficulty = ex.difficulty,
                description = ex.description,
                isCompleted = true
            )
            repo.addTodayExercise(item)
        }
    }

    fun addExerciseToDateWithDuration(dateKey: String, ex: Exercise, sets: Int, durationMin: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, repsPerSet = null, durationMin = durationMin)
            val repsString = List(sets) { durationMin.toString() }.joinToString(",")
            val totalMin = sets * durationMin
            
            val item = TodayExerciseEntity(
                dateKey = dateKey,
                exerciseId = ex.id,
                name = ex.name,
                category = ex.category,
                sets = sets,
                duration = durationMin,
                actualReps = totalMin,
                setReps = repsString,
                actualDurationSec = totalMin * 60,
                calories = calories,
                difficulty = ex.difficulty,
                description = ex.description,
                isCompleted = true
            )
            repo.addTodayExercise(item)
        }
    }

    fun addExerciseToTodayWithSelection(ex: Exercise, sets: Int, repsPerSet: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, repsPerSet = repsPerSet, durationMin = null)
            repo.addToToday(ex, todayKey, sets, repsPerSet, null, calories)
        }
    }

    fun addExerciseToTodayWithDuration(ex: Exercise, sets: Int, durationMin: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, repsPerSet = null, durationMin = durationMin)
            repo.addToToday(ex, todayKey, sets, null, durationMin, calories)
        }
    }

    private fun calcCalories(pending: Exercise, sets: Int?, repsPerSet: Int?, durationMin: Int?): Int {
        val s = (sets ?: 1).toDouble()
        return if (repsPerSet != null) {
            val baseReps = 10.0
            val r = repsPerSet.toDouble()
            (pending.calories * s * (r / baseReps)).roundToInt()
        } else {
            val baseMin = (pending.duration ?: 5).toDouble()
            val m = (durationMin ?: (pending.duration ?: 5)).toDouble()
            (pending.calories * s * (m / baseMin)).roundToInt()
        }.coerceAtLeast(0)
    }

    fun completeWorkoutFromTimer(
        rowId: Long,
        actualSec: Int,
        totalReps: Int,
        setReps: List<String>,
        setWeights: List<String>
    ) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }

            val updatedCalories = if (baseExercise != null) {
                if (item.repsPerSet != null) {
                    (baseExercise.calories * (totalReps / 10.0)).roundToInt()
                } else {
                    val baseMin = (baseExercise.duration ?: 5).toDouble()
                    (baseExercise.calories * (totalReps.toDouble() / baseMin)).roundToInt()
                }
            } else {
                item.calories
            }

            repo.completeRecord(
                rowId = rowId,
                sets = setReps.size,
                actualSec = actualSec,
                actualReps = totalReps,
                calories = updatedCalories,
                setReps = setReps.joinToString(","),
                setWeights = setWeights.joinToString(",")
            )
        }
    }

    fun updateActualTime(rowId: Long, totalSec: Int) {
        viewModelScope.launch {
            repo.completeRecord(rowId, 1, totalSec, 0, 0, "", "") 
        }
    }
    
    fun observeExercisesForDate(dateKey: String) = repo.observeToday(dateKey)

    fun toggleCompleted(rowId: Long, checked: Boolean) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            if (checked) {
                val targetReps = if (item.repsPerSet != null) {
                    item.sets * item.repsPerSet
                } else {
                    item.sets * (item.duration ?: 0)
                }

                val estimatedSec = if (item.duration != null) {
                    item.sets * (item.duration ?: 0) * 60
                } else {
                    0
                }

                val setRepsString = if (item.repsPerSet != null) {
                    List(item.sets) { item.repsPerSet.toString() }.joinToString(",")
                } else if (item.duration != null) {
                    List(item.sets) { item.duration.toString() }.joinToString(",")
                } else {
                    ""
                }
                
                val setWeightsString = ""

                repo.completeRecord(rowId, item.sets, estimatedSec, targetReps, item.calories, setRepsString, setWeightsString)
            } else {
                val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }
                val initialCalories = if (baseExercise != null) {
                    calcCalories(baseExercise, item.sets, item.repsPerSet, item.duration)
                } else {
                    item.calories
                }
                repo.resetRecord(rowId, initialCalories)

                val currentList = repo.observeToday(todayKey).first()
                if (currentList.none { it.isCompleted }) {
                    val photos = photoDao.getPhotoForDate(todayKey).first()
                    photos.forEach { photo ->
                        photoDao.delete(photo)
                    }
                }
            }
        }
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

    fun updateSetInfo(rowId: Long, sets: Int, reps: String, weights: String, totalReps: Int) {
        viewModelScope.launch {
            repo.updateSetInfo(rowId, sets, reps, weights, totalReps)
        }
    }

    fun updateSetInfo(rowId: Long, reps: List<String>, weights: List<String>) {
        viewModelScope.launch {
            val repsString = reps.joinToString(",")
            val weightsString = weights.joinToString(",")
            val totalReps = reps.sumOf { it.toIntOrNull() ?: 0 }
            
            val item = repo.getTodayOnce(todayKey).firstOrNull { it.rowId == rowId }
            val baseExercise = catalogAll.value.firstOrNull { it.id == item?.exerciseId }
            
            val isTimeBased = item?.repsPerSet == null

            val updatedCalories = if (baseExercise != null && item?.isCompleted == true) {
                if (!isTimeBased) {
                    (baseExercise.calories * (totalReps / 10.0)).roundToInt()
                } else {
                    val baseMin = (baseExercise.duration ?: 5).toDouble()
                    (baseExercise.calories * (totalReps.toDouble() / baseMin)).roundToInt()
                }
            } else {
                item?.calories ?: 0
            }

            val actualSec = if (isTimeBased) totalReps * 60 else item?.actualDurationSec ?: 0

            repo.updateSetInfoFull(rowId, reps.size, repsString, weightsString, totalReps, updatedCalories, actualSec)
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
