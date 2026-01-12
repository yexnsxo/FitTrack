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

    // Service-managed state
    private val _isWorkoutStarted = MutableStateFlow(false)
    val isWorkoutStarted = _isWorkoutStarted.asStateFlow()

    private val _totalTime = MutableStateFlow(60)
    val totalTime = _totalTime.asStateFlow()

    private val _totalWorkoutTime = MutableStateFlow(0)
    val totalWorkoutTime = _totalWorkoutTime.asStateFlow()

    private val _totalSets = MutableStateFlow(5)
    val totalSets = _totalSets.asStateFlow()

    private val _currentSet = MutableStateFlow(1)
    val currentSet = _currentSet.asStateFlow()

    private val _setReps = MutableStateFlow(List(_totalSets.value) { 10 })
    val setReps = _setReps.asStateFlow()

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime = _remainingTime.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()


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
            ACTION_STOP_WORKOUT -> stopWorkout()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
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
    }

    fun stopWorkout() {
        totalWorkoutTimeJob?.cancel()
        restTimerJob?.cancel()
        _isResting.value = false
        _remainingTime.value = 0
        _totalWorkoutTime.value = 0
        _isWorkoutStarted.value = false
        _currentSet.value = 1
        _setReps.value = List(_totalSets.value) { 10 }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun startRest(totalTime: Int) {
        if (_isResting.value) {
            return
        }
        _remainingTime.value = totalTime
        _isResting.value = true
        updateNotification()

        restTimerJob = serviceScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value--
                updateNotification()
            }
            _isResting.value = false
            updateNotification()
        }
    }

    fun stopRest() {
        restTimerJob?.cancel()
        _isResting.value = false
        _remainingTime.value = 0
        updateNotification()
    }

    fun finishSet() {
        stopRest()
        if (_currentSet.value < _totalSets.value) {
            _currentSet.value++
            startRest(_totalTime.value)
        } else if (_currentSet.value == _totalSets.value) {
            _currentSet.value++
        }
        updateNotification()
    }

     fun setRestTime(seconds: Int) {
        if (!_isResting.value) {
            _totalTime.value = seconds
        }
        updateNotification()
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
        updateNotification()
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
        val title: String
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
                contentText = "남은 시간: ${formatTime(_remainingTime.value)}"
                val stopRestIntent = Intent(this, TimerService::class.java).apply {
                    action = ACTION_STOP_REST
                }
                val stopRestPendingIntent = PendingIntent.getService(
                    this, 1, stopRestIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "건너뛰기", stopRestPendingIntent)
            } else {
                title = "운동 중"
                contentText = "현재 ${currentSet.value}세트 / 총 ${totalSets.value}세트 | 총 시간: ${formatTime(totalWorkoutTime.value)}"
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
