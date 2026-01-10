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

    // 운동 시작 상태
    private val _isWorkoutStarted = MutableStateFlow(false)
    val isWorkoutStarted: StateFlow<Boolean> = _isWorkoutStarted.asStateFlow()

    // 휴식 타이머 상태
    private val _totalTime = MutableStateFlow(60L) // 총 휴식 시간 (초)
    val totalTime: StateFlow<Long> = _totalTime.asStateFlow()

    private val _remainingTime = MutableStateFlow(60L) // 남은 휴식 시간
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    // 세트 & 횟수 상태
    private val _totalSets = MutableStateFlow(5)
    val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _reps = MutableStateFlow(0)
    val reps: StateFlow<Int> = _reps.asStateFlow()

    private val _setReps = MutableStateFlow<List<Int>>(listOf())
    val setReps: StateFlow<List<Int>> = _setReps.asStateFlow()

    init {
        _setReps.value = List(_totalSets.value) { 0 }
        _reps.value = _setReps.value.getOrElse(0) { 0 }
    }

    fun startWorkout() {
        _isWorkoutStarted.value = true
    }

    fun setRestTime(seconds: Long) {
        if (!isTimerRunning.value) {
            _totalTime.value = seconds
            _remainingTime.value = seconds
        }
    }

    private fun advanceToNextSet() {
        if (_currentSet.value < _totalSets.value) {
            _currentSet.value++
            _reps.value = _setReps.value.getOrElse(_currentSet.value - 1) { 0 }
        }
    }

    private fun startRest() {
        _remainingTime.value = _totalTime.value
        if (_remainingTime.value <= 0) return

        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value--
            }
            _isTimerRunning.value = false
        }
    }

    fun stopRest() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingTime.value = _totalTime.value
    }

    fun setTotalSets(count: Int) {
        if (count > 0) {
            _totalSets.value = count
            val currentReps = _setReps.value
            _setReps.value = List(count) { index -> currentReps.getOrNull(index) ?: 0 }
            if (_currentSet.value > count) {
                _currentSet.value = count
            }
        }
    }

    fun finishSet() {
        if (_currentSet.value <= _totalSets.value) {
            val updatedReps = _setReps.value.toMutableList()
            if (_currentSet.value - 1 < updatedReps.size) {
                updatedReps[_currentSet.value - 1] = _reps.value
                _setReps.value = updatedReps
            }
        }

        if (_currentSet.value < _totalSets.value) {
            advanceToNextSet()
            startRest()
        } else if (_currentSet.value == _totalSets.value) {
            _currentSet.value++ // Mark as complete
            stopRest()
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isWorkoutStarted.value = false
        _remainingTime.value = _totalTime.value
        _currentSet.value = 1
        _reps.value = 0
        _setReps.value = List(_totalSets.value) { 0 }
    }

    fun setRepsForSet(set: Int, reps: Int) {
        if (set > 0 && set <= _setReps.value.size) {
            val updatedReps = _setReps.value.toMutableList()
            updatedReps[set - 1] = reps
            _setReps.value = updatedReps
            if (set == _currentSet.value) {
                _reps.value = reps
            }
        }
    }

    fun resetToSet(set: Int) {
        if (set > 0 && set <= _totalSets.value) {
            timerJob?.cancel()
            _isTimerRunning.value = false
            _remainingTime.value = _totalTime.value

            _currentSet.value = set
            val updatedReps = _setReps.value.toMutableList()
            for (i in (set - 1) until _totalSets.value) {
                if (i < updatedReps.size) {
                    updatedReps[i] = 0
                }
            }
            _setReps.value = updatedReps
            _reps.value = _setReps.value.getOrElse(set - 1) { 0 }
        }
    }
}