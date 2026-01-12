package com.example.fittrack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private var timerService: TimerService? = null
    private var isBound = false
    private var serviceStateCollectorJob: Job? = null

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

    private val _totalRestTime = MutableStateFlow(60)
    val totalRestTime: StateFlow<Int> = _totalRestTime.asStateFlow()

    private val _remainingRestTime = MutableStateFlow(0)
    val remainingRestTime: StateFlow<Int> = _remainingRestTime.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting: StateFlow<Boolean> = _isResting.asStateFlow()

    private val _totalSets = MutableStateFlow(5)
    val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _setReps = MutableStateFlow<List<Int>>(emptyList())
    val setReps: StateFlow<List<Int>> = _setReps.asStateFlow()

    // Public functions to control the timer, delegating to the service
    fun initWorkout(rowId: Long, name: String, target: Int, type: String, targetSets: Int) =
        timerService?.initWorkout(rowId, name, target, type, targetSets)

    fun startWorkout() = timerService?.startWorkout()
    fun stopRest() = timerService?.stopRest()
    fun finishSet() = timerService?.finishSet()
    fun resetWorkout() = timerService?.resetWorkout()
    fun clearWorkout() = timerService?.clearWorkout()
    fun resetIfMatches(rowId: Long) = timerService?.resetIfMatches(rowId)

    fun setRestTime(seconds: Int) = timerService?.setRestTime(seconds)
    fun setTotalSets(count: Int) = timerService?.setTotalSets(count)
    fun setRepsForSet(set: Int, reps: Int) = timerService?.setRepsForSet(set, reps)
    fun resetToSet(set: Int) = timerService?.resetToSet(set)

    fun getTotalReps(): Int {
        val completedSets = (currentSet.value - 1).coerceAtLeast(0)
        return setReps.value.take(completedSets).sum()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true

            serviceStateCollectorJob = viewModelScope.launch {
                timerService?.let { service ->
                    launch { service.targetRowId.collect { _targetRowId.value = it } }
                    launch { service.exerciseName.collect { _exerciseName.value = it } }
                    launch { service.workoutType.collect { _workoutType.value = it } }
                    launch { service.isWorkoutStarted.collect { _isWorkoutStarted.value = it } }
                    launch { service.totalWorkoutTime.collect { _totalWorkoutTime.value = it } }
                    launch { service.totalRestTime.collect { _totalRestTime.value = it } }
                    launch { service.remainingRestTime.collect { _remainingRestTime.value = it } }
                    launch { service.isResting.collect { _isResting.value = it } }
                    launch { service.totalSets.collect { _totalSets.value = it } }
                    launch { service.currentSet.collect { _currentSet.value = it } }
                    launch { service.setReps.collect { _setReps.value = it } }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            timerService = null
            isBound = false
            serviceStateCollectorJob?.cancel()
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
            serviceStateCollectorJob?.cancel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceStateCollectorJob?.cancel()
    }
}
