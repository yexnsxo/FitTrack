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
            when {
                cat == CategoryFilter.ALL -> list
                cat == CategoryFilter.CUSTOM -> list.filter { it.id.startsWith("custom_") }
                else -> list.filter { it.category == cat.key }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<Progress> =
        todayList
            .map { list ->
                val completed = list.filter { it.isCompleted }
                Progress(
                    completedCount = completed.size,
                    totalCount = list.size,
                    caloriesSum = completed.sumOf { it.calories },
                    totalDurationSec = completed.sumOf { it.actualDurationSec }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Progress())

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

    fun deleteCustomExercise(ex: Exercise) {
        viewModelScope.launch {
            repo.deleteCustomExercise(ex.id)
        }
    }

    fun addExerciseToTodayWithSelection(ex: Exercise, sets: Int, setReps: String) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, setReps = setReps, durationMin = null)
            repo.addToToday(ex, todayKey, sets, setReps, null, calories)
        }
    }

    fun addExerciseToTodayWithDuration(ex: Exercise, sets: Int, durationMin: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = ex, sets = sets, setReps = null, durationMin = durationMin)
            repo.addToToday(ex, todayKey, sets, "", durationMin, calories)
        }
    }

    private fun calcCalories(pending: Exercise, sets: Int?, setReps: String?, durationMin: Int?): Int {
        val s = (sets ?: 1).toDouble()
        return if (!setReps.isNullOrEmpty()) {
            val repsList = setReps.split(',').mapNotNull { it.trim().toIntOrNull() }
            val totalReps = repsList.sum().toDouble()
            val baseReps = 10.0 * repsList.size
            (pending.calories * s * (totalReps / baseReps)).roundToInt()
        } else {
            val baseMin = (pending.duration ?: 5).toDouble()
            val m = (durationMin ?: (pending.duration ?: 5)).toDouble()
            (pending.calories * s * (m / baseMin)).roundToInt()
        }.coerceAtLeast(0)
    }

    fun completeWorkoutFromTimer(
        rowId: Long,
        actualSec: Int,
        setReps: List<String>,
        setWeights: List<String>
    ) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }
            val totalReps = setReps.sumOf { it.toIntOrNull() ?: 0 }

            val updatedCalories = if (baseExercise != null) {
                if (item.duration.isNullOrEmpty()) {
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
                actualSec = actualSec,
                calories = updatedCalories,
                setReps = setReps.joinToString(","),
                setWeights = setWeights.joinToString(",")
            )
        }
    }

    // ✅ 실제 시간 수동 수정 시 칼로리 재계산 로직 추가
    fun updateActualTime(rowId: Long, totalSec: Int) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }

            val updatedCalories = if (baseExercise != null) {
                if (item.duration.isNullOrEmpty()) {
                    // 근력 운동: 시간 수정 시에도 기존 수행 횟수 기준으로 칼로리 유지 (또는 필요 시 수정 가능)
                    item.calories
                } else {
                    // 시간 기반 운동: 수정된 실제 분(min)에 맞춰 칼로리 재계산
                    val actualMin = totalSec / 60.0
                    val baseMin = (baseExercise.duration ?: 5).toDouble()
                    (baseExercise.calories * (actualMin / baseMin)).roundToInt()
                }
            } else {
                item.calories
            }

            // DB 업데이트 (기존 completeRecord 재사용하여 실제 시간과 갱신된 칼로리 저장)
            repo.completeRecord(rowId, totalSec, updatedCalories, item.setReps, item.setWeights)
        }
    }

    fun toggleCompleted(rowId: Long, checked: Boolean) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            if (checked) {
                val estimatedSec = if (item.duration.isNotEmpty()) {
                    item.sets * (item.duration.split(",").firstOrNull()?.trim()?.toIntOrNull() ?: 0) * 60
                } else {
                    0
                }

                val setWeightsString = List(item.sets) { "" }.joinToString(",")

                repo.completeRecord(rowId, estimatedSec, item.calories, item.setReps, setWeightsString)
            } else {
                val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }
                val initialCalories = if (baseExercise != null) {
                    calcCalories(baseExercise, item.sets, item.setReps, item.duration.split(",").firstOrNull()?.trim()?.toIntOrNull())
                } else {
                    item.calories
                }
                repo.resetRecord(rowId, initialCalories)
            }
        }
    }

    fun deleteTodayRow(rowId: Long) {
        viewModelScope.launch { repo.deleteRow(rowId) }
    }

    fun updateTodayRowStrength(item: TodayExerciseEntity, sets: Int, reps: String) {
        viewModelScope.launch {
            val base = catalogAll.value.firstOrNull { it.id == item.exerciseId }
            val kcal = if (base != null) calcCalories(base, sets, reps, null) else item.calories
            //repo.updateTodayAmounts(item.rowId, sets, null, kcal)
            // TODO: update setReps as well
        }
    }

    fun updateTodayRowDuration(item: TodayExerciseEntity, sets: Int, minutes: Int) {
        viewModelScope.launch {
            val base = catalogAll.value.firstOrNull { it.id == item.exerciseId }
            val kcal = if (base != null) calcCalories(base, sets, null, minutes) else item.calories
            val durationString = List(sets) { minutes }.joinToString(",")
            repo.updateTodayAmounts(item.rowId, sets, durationString, kcal)
        }
    }

    fun updateSetInfo(rowId: Long, sets: Int, reps: List<String>, weights: List<String>) {
        viewModelScope.launch {
            val repsString = reps.joinToString(",")
            val weightsString = weights.joinToString(",")
            val totalReps = reps.sumOf { it.toIntOrNull() ?: 0 }

            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }

            val updatedCalories = if (baseExercise != null && item.isCompleted) {
                if (item.duration.isNullOrEmpty()) {
                    (baseExercise.calories * (totalReps / 10.0)).roundToInt()
                } else {
                    item.calories
                }
            } else {
                item.calories
            }

            repo.updateSetInfoAndCount(rowId, sets, repsString, weightsString, updatedCalories)
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
