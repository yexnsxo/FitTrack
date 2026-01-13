package com.example.fittrack

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fittrack.data.Exercise
import com.example.fittrack.data.FitTrackDatabase
import com.example.fittrack.data.PhotoDatabase
import com.example.fittrack.data.PhotoRepository
import com.example.fittrack.data.TodayExerciseEntity
import com.example.fittrack.data.TodoRepository
import com.example.fittrack.ui.theme.Main40
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ViewModel for EditRecordScreen
class EditRecordViewModel(private val repo: TodoRepository, private val date: LocalDate) : ViewModel() {

    private val dateKey: String = date.toString()

    private val _exercises = MutableStateFlow<List<TodayExerciseEntity>>(emptyList())
    val exercises: StateFlow<List<TodayExerciseEntity>> = _exercises.asStateFlow()

    private val _catalog = MutableStateFlow<List<Exercise>>(emptyList())
    private val customCatalog: StateFlow<List<Exercise>> =
        repo.observeCustomCatalog()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val catalogAll: StateFlow<List<Exercise>> =
        combine(_catalog, customCatalog) { base, custom ->
            (base + custom).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private val _progress = MutableStateFlow(Progress(0, 0, 0, 0))
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()


    val filteredCatalog: StateFlow<List<Exercise>> = combine(
        catalogAll,
        selectedCategory
    ) { catalog, category ->
        when (category) {
            CategoryFilter.ALL -> catalog
            CategoryFilter.CUSTOM -> catalog.filter { it.id.startsWith("custom_") }
            else -> catalog.filter { it.category == category.key }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        viewModelScope.launch {
            _catalog.value = repo.loadCatalogFromAssets()
            repo.observeToday(dateKey).collect { exerciseList ->
                _exercises.value = exerciseList
                updateProgress(exerciseList)
            }
        }
    }

    private fun updateProgress(list: List<TodayExerciseEntity>) {
        val completed = list.filter { it.isCompleted }
        _progress.value = Progress(
            completedCount = completed.size,
            totalCount = list.size,
            caloriesSum = completed.sumOf { it.calories },
            totalDurationSec = completed.sumOf { it.actualDurationSec }
        )
    }

    fun selectCategory(category: CategoryFilter) {
        _selectedCategory.value = category
    }

    fun addExerciseToTodayWithSelection(exercise: Exercise, sets: Int, reps: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = exercise, sets = sets, repsPerSet = reps, durationMin = null)
            repo.addToToday(exercise, dateKey, sets, reps, null, calories)
        }
    }

    fun addExerciseToTodayWithDuration(exercise: Exercise, sets: Int, minutes: Int) {
        viewModelScope.launch {
            val calories = calcCalories(pending = exercise, sets = sets, repsPerSet = null, durationMin = minutes)
            repo.addToToday(exercise, dateKey, sets, null, minutes, calories)
        }
    }

    fun toggleCompleted(rowId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repo.setCompleted(rowId, isCompleted)
        }
    }

    fun deleteTodayRow(rowId: Long) {
        viewModelScope.launch {
            repo.deleteRow(rowId)
        }
    }

    fun updateSetInfo(rowId: Long, reps: List<String>, weights: List<String>) {
        viewModelScope.launch {
            val repsString = reps.joinToString(",")
            val weightsString = weights.joinToString(",")
            val actualReps = reps.mapNotNull { it.toIntOrNull() }.sum()
            repo.updateSetInfo(rowId, repsString, weightsString, actualReps)
        }
    }

    fun updateActualTime(rowId: Long, totalSec: Int) {
        viewModelScope.launch {
            val item = exercises.value.firstOrNull { it.rowId == rowId } ?: return@launch
            val baseExercise = catalogAll.value.firstOrNull { it.id == item.exerciseId }

            val updatedCalories = if (baseExercise != null) {
                if (item.repsPerSet != null) {
                    item.calories
                } else {
                    val actualMin = totalSec / 60.0
                    val baseMin = (baseExercise.duration ?: 5).toDouble()
                    (baseExercise.calories * (actualMin / baseMin)).roundToInt()
                }
            } else {
                item.calories
            }
            repo.completeRecord(rowId, totalSec, item.actualReps, updatedCalories, item.setReps, item.setWeights)
        }
    }

    fun addCustomExerciseToCatalog(exercise: Exercise) {
        viewModelScope.launch {
            repo.upsertCustomExercise(exercise)
        }
    }

    fun deleteCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            repo.deleteCustomExercise(exercise.id)
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
}

class EditRecordViewModelFactory(private val context: Context, private val date: LocalDate) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditRecordViewModel::class.java)) {
            val db = FitTrackDatabase.getInstance(context)
            val photoDb = PhotoDatabase.getDatabase(context)
            val photoRepo = PhotoRepository(photoDb.photoDao())
            val repo = TodoRepository(
                context,
                db.todayExerciseDao(),
                db.customExerciseDao(),
                photoRepo
            )
            @Suppress("UNCHECKED_CAST")
            return EditRecordViewModel(repo, date) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    date: LocalDate,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: EditRecordViewModel = viewModel(
        factory = EditRecordViewModelFactory(context, date)
    )

    val progress by viewModel.progress.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val filteredCatalog by viewModel.filteredCatalog.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    val pendingAddState = remember { mutableStateOf<Exercise?>(null) }
    val showDirectAddState = remember { mutableStateOf(false) }
    val pendingEditSet = remember { mutableStateOf<TodayExerciseEntity?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기록 편집: ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Main40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    ProgressOverview(
                        completedCount = progress.completedCount,
                        totalCount = progress.totalCount,
                        caloriesSum = progress.caloriesSum,
                        totalDurationSec = progress.totalDurationSec
                    )
                }

                item {
                    TodayListCard(
                        items = exercises,
                        onToggle = { item, checked -> viewModel.toggleCompleted(item.rowId, checked) },
                        onDelete = { item -> viewModel.deleteTodayRow(item.rowId) },
                        onEditStrength = { _, _, _ -> },
                        onEditDuration = { _, _, _ -> },
                        onTimerClick = { },
                        onEditActualTime = { item, totalSec -> viewModel.updateActualTime(item.rowId, totalSec) },
                        onEditSetInfo = { item -> pendingEditSet.value = item }
                    )
                }

                item { CategoryCard(selected = selected, onSelect = viewModel::selectCategory) }

                item {
                    ExerciseCatalogCard(
                        exercises = filteredCatalog,
                        onAdd = { ex -> pendingAddState.value = ex },
                        onDeleteCustom = { ex -> viewModel.deleteCustomExercise(ex) },
                        onOpenDirectAdd = { showDirectAddState.value = true }
                    )
                }
            }

            pendingAddState.value?.let { pending ->
                AddExerciseDialog(
                    exercise = pending,
                    onDismiss = { pendingAddState.value = null },
                    onConfirmStrength = { sets, reps ->
                        viewModel.addExerciseToTodayWithSelection(pending, sets, reps)
                        pendingAddState.value = null
                    },
                    onConfirmDuration = { sets, minutes ->
                        viewModel.addExerciseToTodayWithDuration(pending, sets, minutes)
                        pendingAddState.value = null
                    }
                )
            }

            if (showDirectAddState.value) {
                AddCustomExerciseDialog(
                    onDismiss = { showDirectAddState.value = false },
                    onConfirm = { newExercise ->
                        viewModel.addCustomExerciseToCatalog(newExercise)
                        showDirectAddState.value = false
                    }
                )
            }

            pendingEditSet.value?.let { item ->
                EditSetInfoDialog(
                    item = item,
                    onDismiss = { pendingEditSet.value = null },
                    onConfirm = { reps, weights ->
                        viewModel.updateSetInfo(item.rowId, reps, weights)
                        pendingEditSet.value = null
                    }
                )
            }
        }
    }
}
