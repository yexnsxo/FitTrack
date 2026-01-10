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

    private val _totalTime = MutableStateFlow(60L) // Default 60 seconds
    val totalTime: StateFlow<Long> = _totalTime.asStateFlow()

    private val _remainingTime = MutableStateFlow(60L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null

    fun setTime(seconds: Long) {
        if (!isRunning.value) {
            _totalTime.value = seconds
            _remainingTime.value = seconds
        }
    }

    fun startTimer() {
        if (isRunning.value || _remainingTime.value <= 0) return

        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value--
            }
            _isRunning.value = false
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        _remainingTime.value = _totalTime.value
    }
}
