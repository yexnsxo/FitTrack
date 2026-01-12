package com.example.fittrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private val _targetRowId = MutableStateFlow<Long?>(null)
    val targetRowId: StateFlow<Long?> = _targetRowId.asStateFlow()

    private val _exerciseName = MutableStateFlow("")
    val exerciseName: StateFlow<String> = _exerciseName.asStateFlow()

    private val _workoutType = MutableStateFlow("reps")
    val workoutType: StateFlow<String> = _workoutType.asStateFlow()

    private val _isWorkoutStarted = MutableStateFlow(false)
    val isWorkoutStarted: StateFlow<Boolean> = _isWorkoutStarted.asStateFlow()

    private val _totalWorkoutTime = MutableStateFlow(0)
    val totalWorkoutTime: StateFlow<Int> = _totalWorkoutTime.asStateFlow()
    private var workoutTimerJob: Job? = null

    private val _totalRestTime = MutableStateFlow(60) 
    val totalRestTime: StateFlow<Int> = _totalRestTime.asStateFlow()

    private val _remainingRestTime = MutableStateFlow(60) 
    val remainingRestTime: StateFlow<Int> = _remainingRestTime.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting: StateFlow<Boolean> = _isResting.asStateFlow()

    private var restTimerJob: Job? = null

    private val _totalSets = MutableStateFlow(5)
    val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _setReps = MutableStateFlow<List<Int>>(listOf())
    val setReps: StateFlow<List<Int>> = _setReps.asStateFlow()

    fun initWorkout(rowId: Long, name: String, target: Int, type: String, targetSets: Int) {
        // ✅ 이미 같은 운동이 시작된 상태라면 초기화하지 않고 유지
        if (_targetRowId.value == rowId && _isWorkoutStarted.value) return

        _targetRowId.value = rowId
        _exerciseName.value = name
        _workoutType.value = type
        _totalSets.value = if (targetSets > 0) targetSets else 5
        _setReps.value = List(_totalSets.value) { target }
        
        resetWorkout()
    }

    fun startWorkout() {
        _isWorkoutStarted.value = true
        startWorkoutTimer()
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _totalWorkoutTime.value++
            }
        }
    }

    fun setRestTime(seconds: Int) {
        if (!_isResting.value) {
            _totalRestTime.value = seconds
            _remainingRestTime.value = seconds
        }
    }

    private fun startRest() {
        _remainingRestTime.value = _totalRestTime.value
        if (_remainingRestTime.value <= 0) return

        _isResting.value = true
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_remainingRestTime.value > 0) {
                delay(1000)
                _remainingRestTime.value--
            }
            _isResting.value = false
        }
    }

    fun stopRest() {
        restTimerJob?.cancel()
        _isResting.value = false
        _remainingRestTime.value = _totalRestTime.value
    }

    fun setTotalSets(count: Int) {
        if (count > 0) {
            _totalSets.value = count
            val currentReps = _setReps.value
            val target = if (currentReps.isNotEmpty()) currentReps[0] else 10
            _setReps.value = List(count) { index -> currentReps.getOrNull(index) ?: target }
            if (_currentSet.value > count) {
                _currentSet.value = count
            }
        }
    }

    fun finishSet() {
        if (_currentSet.value < _totalSets.value) {
            _currentSet.value++
            startRest()
        } else if (_currentSet.value == _totalSets.value) {
            _currentSet.value++ // 완료 상태
            stopRest()
            workoutTimerJob?.cancel()
        }
    }

    fun resetWorkout() {
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()
        _isResting.value = false
        _isWorkoutStarted.value = false
        _totalWorkoutTime.value = 0
        _remainingRestTime.value = _totalRestTime.value
        _currentSet.value = 1
    }

    // ✅ 완전히 초기화 (운동 종료 시 또는 Todo 체크 해제 시 호출)
    fun clearWorkout() {
        resetWorkout()
        _targetRowId.value = null
        _exerciseName.value = ""
        _setReps.value = emptyList()
    }

    fun resetIfMatches(rowId: Long) {
        if (_targetRowId.value == rowId) {
            clearWorkout()
        }
    }

    fun setRepsForSet(set: Int, reps: Int) {
        if (set > 0 && set <= _setReps.value.size) {
            val updatedReps = _setReps.value.toMutableList()
            updatedReps[set - 1] = reps
            _setReps.value = updatedReps
        }
    }

    fun resetToSet(set: Int) {
        if (set > 0 && set <= _totalSets.value) {
            restTimerJob?.cancel()
            _isResting.value = false
            _remainingRestTime.value = _totalRestTime.value
            _currentSet.value = set
        }
    }
    
    fun getTotalReps(): Int {
        val completedSets = (_currentSet.value - 1).coerceAtLeast(0)
        return _setReps.value.take(completedSets).sum()
    }
}
