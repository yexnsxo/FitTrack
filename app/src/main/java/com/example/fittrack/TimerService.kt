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
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var restTimerJob: Job? = null
    private var totalWorkoutTimeJob: Job? = null
    private var viewModel: TimerViewModel? = null

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime: StateFlow<Int> get() = _remainingTime

    private val _isResting = MutableStateFlow(false)
    val isResting: StateFlow<Boolean> get() = _isResting

    private val _totalWorkoutTime = MutableStateFlow(0)
    val totalWorkoutTime: StateFlow<Int> get() = _totalWorkoutTime

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_FINISH_SET = "com.example.fittrack.ACTION_FINISH_SET"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FINISH_SET) {
            viewModel?.finishSet()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startWorkout() {
        _totalWorkoutTime.value = 0
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        totalWorkoutTimeJob?.cancel()
        totalWorkoutTimeJob = serviceScope.launch {
            while (isActive) {
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
        stopForeground(true)
    }

    fun startRest(totalTime: Int) {
        _remainingTime.value = totalTime
        _isResting.value = true
        updateNotification()

        restTimerJob?.cancel()
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

    private fun createNotification(): Notification {
        val finishSetIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_FINISH_SET
        }
        val finishSetPendingIntent = PendingIntent.getService(this, 0, finishSetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val title: String
        val contentText: String

        if (_isResting.value) {
            title = "휴식 중"
            contentText = "남은 시간: ${formatTime(_remainingTime.value)}"
        } else {
            val currentSet = viewModel?.currentSet?.value ?: 1
            val totalSets = viewModel?.totalSets?.value ?: 1
            title = "운동 중"
            contentText = "현재 ${currentSet}세트 / 총 ${totalSets}세트 | 총 시간: ${formatTime(_totalWorkoutTime.value)}"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(R.drawable.ic_launcher_foreground, "세트 완료", finishSetPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
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
        fun setViewModel(viewModel: TimerViewModel) {
            this@TimerService.viewModel = viewModel
        }
    }
}
