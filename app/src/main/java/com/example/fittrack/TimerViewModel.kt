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

    // 각 세트별 횟수를 저장하는 리스트
    private val _setReps = MutableStateFlow<List<Int>>(listOf())
    val setReps: StateFlow<List<Int>> = _setReps.asStateFlow()

    init {
        _setReps.value = List(_totalSets.value) { 0 }
    }

    // --- 타이머 함수 ---
    fun setRestTime(seconds: Long) {
        if (!isTimerRunning.value) {
            _totalTime.value = seconds
            _remainingTime.value = seconds
        }
    }

    private fun advanceToNextSet() {
        if (_currentSet.value < _totalSets.value) {
            _currentSet.value++
        }
        _reps.value = 0
    }

    private fun startRest() {
        if (_isTimerRunning.value) return

        _remainingTime.value = _totalTime.value
        if (_remainingTime.value <= 0) {
            advanceToNextSet()
            return
        }

        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value--
            }
            _isTimerRunning.value = false
            advanceToNextSet()
        }
    }

    fun stopRest() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingTime.value = _totalTime.value
        advanceToNextSet()
    }


    // --- 세트 & 횟수 함수 ---
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

    fun incrementReps() {
        _reps.value++
    }

    fun decrementReps() {
        if (_reps.value > 0) {
            _reps.value--
        }
    }

    fun incrementRepsByFive() {
        _reps.value += 5
    }

    fun decrementRepsByFive() {
        _reps.value = (_reps.value - 5).coerceAtLeast(0)
    }

    fun finishSet() {
        if (_currentSet.value <= _totalSets.value) {
            val updatedReps = _setReps.value.toMutableList()
            updatedReps[_currentSet.value - 1] = _reps.value
            _setReps.value = updatedReps
        }

        if (_currentSet.value < _totalSets.value) {
            startRest()
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingTime.value = _totalTime.value
        _currentSet.value = 1
        _reps.value = 0
        _setReps.value = List(_totalSets.value) { 0 }
    }
}
