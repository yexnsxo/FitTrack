package com.example.fittrack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var restTimerJob: Job? = null
    private var totalWorkoutTimeJob: Job? = null
    private var setTimerJob: Job? = null


    // Service-managed state
    private val _targetRowId = MutableStateFlow<Long?>(null)
    val targetRowId = _targetRowId.asStateFlow()

    private val _exerciseName = MutableStateFlow("")
    val exerciseName = _exerciseName.asStateFlow()

    private val _workoutType = MutableStateFlow("reps")
    val workoutType = _workoutType.asStateFlow()

    private val _isWorkoutStarted = MutableStateFlow(false)
    val isWorkoutStarted = _isWorkoutStarted.asStateFlow()

    private val _totalWorkoutTime = MutableStateFlow(0)
    val totalWorkoutTime = _totalWorkoutTime.asStateFlow()

    private val _totalRestTime = MutableStateFlow(60)
    val totalRestTime = _totalRestTime.asStateFlow()

    private val _remainingRestTime = MutableStateFlow(0)
    val remainingRestTime = _remainingRestTime.asStateFlow()

    private val _remainingSetTime = MutableStateFlow(0)
    val remainingSetTime = _remainingSetTime.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet = _currentSet.asStateFlow()

    private val _totalSets = MutableStateFlow(5)
    val totalSets = _totalSets.asStateFlow()

    private val _setReps = MutableStateFlow(List(_totalSets.value) { 10 })
    val setReps = _setReps.asStateFlow()

    private val _setWeights = MutableStateFlow(List(_totalSets.value) { 0 })
    val setWeights = _setWeights.asStateFlow()


    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_FINISH_SET = "com.example.fittrack.ACTION_FINISH_SET"
        const val ACTION_STOP_REST = "com.example.fittrack.ACTION_STOP_REST"
        const val ACTION_STOP_WORKOUT = "com.example.fittrack.ACTION_STOP_WORKOUT"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FINISH_SET -> finishSet()
            ACTION_STOP_REST -> stopRest()
            ACTION_STOP_WORKOUT -> clearWorkout()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    fun initWorkout(
        rowId: Long,
        name: String,
        target: String,
        type: String,
        targetSets: Int,
        setReps: String,
        setWeights: String
    ) {
        // If a new workout is started for the same ID, just continue
        if (_targetRowId.value == rowId && _isWorkoutStarted.value) return

        resetWorkout()

        _targetRowId.value = rowId
        _exerciseName.value = name
        _workoutType.value = type
        _totalSets.value = targetSets.coerceAtLeast(1)

        val repsList = if (setReps.isNotBlank()) {
            setReps.split(',').mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }

        val weightsList = if (setWeights.isNotBlank()) {
            setWeights.split(',').mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }

        // Initialize reps or durations for each set
        if (type == "time") {
            val durationList = if (target.isNotBlank()) {
                target.split(',').mapNotNull { it.trim().toIntOrNull() }
            } else {
                emptyList()
            }
            _setReps.value = List(_totalSets.value) { i ->
                durationList.getOrElse(i) { 60 } // Default duration 60 seconds
            }
        } else { // reps
            _setReps.value = List(_totalSets.value) { i ->
                repsList.getOrElse(i) { 10 } // default for reps
            }
        }


        // Initialize weights for each set
        _setWeights.value = List(_totalSets.value) { i ->
            weightsList.getOrElse(i) { 0 } // default weight is 0
        }

        _currentSet.value = 1
    }


    fun startWorkout() {
        if (_isWorkoutStarted.value) return
        _isWorkoutStarted.value = true

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        totalWorkoutTimeJob?.cancel()
        totalWorkoutTimeJob = serviceScope.launch {
            while (true) {
                delay(1000)
                _totalWorkoutTime.value++
                updateNotification()
            }
        }
        if (_workoutType.value == "time") {
            startSetTimer()
        }
    }
    private fun startSetTimer() {
        setTimerJob?.cancel()
        val setDurationInSeconds = _setReps.value.getOrNull(_currentSet.value - 1) ?: 0
        _remainingSetTime.value = setDurationInSeconds

        setTimerJob = serviceScope.launch {
            while (_remainingSetTime.value > 0) {
                delay(1000)
                _remainingSetTime.value--
                updateNotification()
            }
            if (_isWorkoutStarted.value && _remainingSetTime.value == 0) {
                finishSet()
            }
        }
    }
    private fun stopSetTimer() {
        setTimerJob?.cancel()
        _remainingSetTime.value = 0
    }


    fun clearWorkout() {
        totalWorkoutTimeJob?.cancel()
        restTimerJob?.cancel()
        setTimerJob?.cancel()
        _isResting.value = false
        _remainingRestTime.value = 0
        _totalWorkoutTime.value = 0
        _isWorkoutStarted.value = false
        _currentSet.value = 1
        _setReps.value = List(_totalSets.value) { 10 }
        _setWeights.value = List(_totalSets.value) { 0 }
        _targetRowId.value = null
        _exerciseName.value = ""
        _workoutType.value = "reps"
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun resetWorkout() {
        totalWorkoutTimeJob?.cancel()
        restTimerJob?.cancel()
        setTimerJob?.cancel()
        _isResting.value = false
        _remainingRestTime.value = 0
        _remainingSetTime.value = 0
        _totalWorkoutTime.value = 0
        _isWorkoutStarted.value = false
        _currentSet.value = 1
    }
    fun resetIfMatches(rowId: Long) {
        if (_targetRowId.value == rowId) {
            resetWorkout()
        }
    }

    fun startRest() {
        if (_isResting.value) return

        _remainingRestTime.value = _totalRestTime.value
        _isResting.value = true
        updateNotification()

        restTimerJob = serviceScope.launch {
            while (_remainingRestTime.value > 0) {
                delay(1000)
                _remainingRestTime.value--
                updateNotification()
            }
            _isResting.value = false
            if (_workoutType.value == "time") { // 시간이 다 되면 다음 세트 타이머 시작
                startSetTimer()
            }

            updateNotification()
        }
    }

    fun stopRest() {
        restTimerJob?.cancel()
        _isResting.value = false
        _remainingRestTime.value = 0
        if (_isWorkoutStarted.value && _workoutType.value == "time" && _currentSet.value <= _totalSets.value) {
            startSetTimer()
        }

        updateNotification()
    }

    fun finishSet() {
        stopRest()
        stopSetTimer()
        if (_currentSet.value < _totalSets.value) {
            _currentSet.value++
            startRest()
        } else if (_currentSet.value == _totalSets.value) {
            _currentSet.value++
        }
        updateNotification()
    }

     fun setRestTime(seconds: Int) {
        if (!_isResting.value) {
            _totalRestTime.value = seconds
        }
        updateNotification()
    }

    fun setTotalSets(count: Int) {
        if (count > 0) {
            _totalSets.value = count
            val currentReps = _setReps.value
            _setReps.value = List(count) { index -> currentReps.getOrNull(index) ?: 10 }
            val currentWeights = _setWeights.value
            _setWeights.value = List(count) { index -> currentWeights.getOrNull(index) ?: 0 }
            if (_currentSet.value > count) {
                _currentSet.value = count
            }
        }
        updateNotification()
    }

    fun setRepsForSet(set: Int, reps: Int) {
        if (set > 0 && set <= _setReps.value.size) {
            val updatedReps = _setReps.value.toMutableList()
            updatedReps[set - 1] = reps
            _setReps.value = updatedReps
        }
    }

    fun setWeightForSet(set: Int, weight: Int) {
        if (set > 0 && set <= _setWeights.value.size) {
            val updatedWeights = _setWeights.value.toMutableList()
            updatedWeights[set - 1] = weight
            _setWeights.value = updatedWeights
        }
    }

    fun setWorkoutType(type: String) {
        _workoutType.value = type
    }

    fun resetToSet(set: Int) {
        if (set > 0 && set <= _totalSets.value) {
            stopRest()
            stopSetTimer()
            _currentSet.value = set
            _isWorkoutStarted.value = true
            if (_workoutType.value == "time") {
                startSetTimer()
            }
        } else if (set > _totalSets.value) {
            _currentSet.value = _totalSets.value + 1
        }
    }


    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", "timer")
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isFinished = currentSet.value > totalSets.value
        var title: String
        val contentText: String
        val isOngoing: Boolean

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(activityPendingIntent)

        if (isFinished) {
            title = "운동 완료!"
            contentText = "총 운동 시간: ${formatTime(totalWorkoutTime.value)}"
            isOngoing = false
        } else {
            isOngoing = true
            if (_isResting.value) {
                title = "휴식 중"
                contentText = "남은 시간: ${formatTime(_remainingRestTime.value)}"
                val stopRestIntent = Intent(this, TimerService::class.java).apply {
                    action = ACTION_STOP_REST
                }
                val stopRestPendingIntent = PendingIntent.getService(
                    this, 1, stopRestIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "건너뛰기", stopRestPendingIntent)
            } else {
                title = if (exerciseName.value.isNotEmpty()) "운동 중 - ${exerciseName.value}" else "운동 중"
                if (workoutType.value == "time") {
                    contentText = "현재 ${currentSet.value}세트 / 총 ${totalSets.value}세트 | 남은 시간: ${formatTime(remainingSetTime.value)}"
                } else {
                    contentText = "현재 ${currentSet.value}세트 / 총 ${totalSets.value}세트 | 총 시간: ${formatTime(totalWorkoutTime.value)}"
                }

                val finishSetIntent = Intent(this, TimerService::class.java).apply {
                    action = ACTION_FINISH_SET
                }
                val finishSetPendingIntent = PendingIntent.getService(
                    this, 0, finishSetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "세트 완료", finishSetPendingIntent)
            }
        }

        return builder
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(isOngoing)
            .setAutoCancel(!isOngoing)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Timer Service Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
