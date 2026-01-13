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
        return if (repsPerSet != null || (pending.category == "strength" && durationMin == null)) {
            val baseReps = 10.0
            val r = (repsPerSet ?: 10).toDouble()
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
                actualSec = actualSec,
                actualReps = totalReps,
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
                if (item.repsPerSet != null) {
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
            repo.completeRecord(rowId, totalSec, item.actualReps, updatedCalories, item.setReps, item.setWeights)
        }
    }

    fun toggleCompleted(rowId: Long, checked: Boolean) {
        viewModelScope.launch {
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            if (checked) {
                val targetReps = if (item.repsPerSet != null) {
                    item.sets * (item.repsPerSet ?: 0)
                } else {
                    0
                }

                val estimatedSec = if (item.duration != null) {
                    item.sets * (item.duration ?: 0) * 60
                } else {
                    0
                }

                val setRepsString = if (item.repsPerSet != null) {
                    List(item.sets) { item.repsPerSet.toString() }.joinToString(",")
                } else {
                    ""
                }
                val setWeightsString = if (item.repsPerSet != null) {
                    List(item.sets) { "" }.joinToString(",")
                } else {
                    ""
                }

                repo.completeRecord(rowId, estimatedSec, targetReps, item.calories, setRepsString, setWeightsString)
            } else {
                val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }
                val initialCalories = if (baseExercise != null) {
                    calcCalories(baseExercise, item.sets, item.repsPerSet, item.duration)
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

    fun updateSetInfo(rowId: Long, reps: List<String>, weights: List<String>) {
        viewModelScope.launch {
            val repsString = reps.joinToString(",")
            val weightsString = weights.joinToString(",")
            val totalReps = reps.sumOf { it.toIntOrNull() ?: 0 }
            
            val item = todayList.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }

            val updatedCalories = if (baseExercise != null && item.isCompleted) {
                if (item.repsPerSet != null) {
                    // 근력 운동: 수정된 총 횟수(totalReps)를 기준으로 칼로리 재계산
                    (baseExercise.calories * (totalReps / 10.0)).roundToInt()
                } else {
                    // 시간 기반 운동: setInfo 수정이 칼로리에 직접 영향을 주지 않는다면 기존값 유지
                    item.calories
                }
            } else {
                item.calories
            }

            repo.updateSetInfoWithCalories(rowId, repsString, weightsString, totalReps, updatedCalories)
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
