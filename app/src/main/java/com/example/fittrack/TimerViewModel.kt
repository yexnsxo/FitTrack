package com.example.fittrack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerViewModel : ViewModel() {

    private var timerService: TimerService? = null
    private var isBound = false

    private val _isWorkoutStarted = MutableStateFlow(false)
    val isWorkoutStarted: StateFlow<Boolean> = _isWorkoutStarted.asStateFlow()

    private val _totalTime = MutableStateFlow(60)
    val totalTime: StateFlow<Int> = _totalTime.asStateFlow()

    val remainingTime: StateFlow<Int> get() = timerService?.remainingTime ?: MutableStateFlow(0)
    val isTimerRunning: StateFlow<Boolean> get() = timerService?.isResting ?: MutableStateFlow(false)
    val totalWorkoutTime: StateFlow<Int> get() = timerService?.totalWorkoutTime ?: MutableStateFlow(0)


    private val _totalSets = MutableStateFlow(5)
    val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _setReps = MutableStateFlow<List<Int>>(listOf())
    val setReps: StateFlow<List<Int>> = _setReps.asStateFlow()

    init {
        _setReps.value = List(_totalSets.value) { 10 }
    }

    fun startWorkout() {
        _isWorkoutStarted.value = true
        timerService?.startWorkout()
    }

    fun setRestTime(seconds: Int) {
        if (timerService?.isResting?.value == false) {
            _totalTime.value = seconds
        }
    }

    private fun advanceToNextSet() {
        if (_currentSet.value <= _totalSets.value) {
            _currentSet.value++
        }
    }

    private fun startRest() {
        timerService?.startRest(_totalTime.value)
    }

    fun stopRest() {
        timerService?.stopRest()
    }

    fun finishSet() {
        if (_currentSet.value <= _totalSets.value) {
            advanceToNextSet()
            startRest()
        }
    }

    fun resetWorkout() {
        stopRest()
        _isWorkoutStarted.value = false
        _currentSet.value = 1
        _setReps.value = List(_totalSets.value) { 10 }
        timerService?.stopWorkout()
    }

    fun setTotalSets(count: Int) {
        if (count > 0) {
            _totalSets.value = count
            val currentReps = _setReps.value
            _setReps.value = List(count) { index -> currentReps.getOrNull(index) ?: 10 }
            if (_currentSet.value > count) {
                _currentSet.value = count
            }
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
            stopRest()
            _currentSet.value = set
            _isWorkoutStarted.value = true
            val updatedReps = _setReps.value.toMutableList()
            for (i in (set - 1) until _totalSets.value) {
                if (i < updatedReps.size) {
                    updatedReps[i] = 10
                }
            }
            _setReps.value = updatedReps
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            binder.setViewModel(this@TimerViewModel)
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    fun bindService(context: Context) {
        Intent(context, TimerService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
