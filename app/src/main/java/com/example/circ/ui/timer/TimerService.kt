package com.example.circ.ui.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.circ.MainActivity
import com.example.circ.data.AppDatabase
import com.example.circ.data.CompletedSession
import com.example.circ.data.TimerConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var cachedBuilder: NotificationCompat.Builder? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var targetEndTimeMillis: Long = 0
    private var workSecondsRemaining = TimerConstants.DEFAULT_WORK_MINS * 60L
    private var breakSecondsRemaining = TimerConstants.DEFAULT_BREAK_MINS * 60L

    val timeLeft = MutableStateFlow(workSecondsRemaining)
    val isRunning = MutableStateFlow(false)
    val isWorkMode = MutableStateFlow(true)

    private val channelId = "timer_alerts_v10"
    private val notificationId = 1
    private val alertId = 2

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = TimerBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "TIMER_DONE") handleTimerFinished()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CIRC::TimerWake")
    }

    fun toggleTimer() {
        if (isRunning.value) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        isRunning.value = true
        val seconds = if (isWorkMode.value) workSecondsRemaining else breakSecondsRemaining

        targetEndTimeMillis = SystemClock.elapsedRealtime() + (seconds * 1000)
        if (wakeLock?.isHeld == false) wakeLock?.acquire(seconds * 1000 + 10000)

        val wallClockTrigger = System.currentTimeMillis() + (seconds * 1000)
        scheduleAlarm(wallClockTrigger)

        startForeground(notificationId, buildNotification())

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isRunning.value) {
                val remaining = ((targetEndTimeMillis - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)
                if (timeLeft.value != remaining) {
                    timeLeft.value = remaining
                    updateNotification()
                }
                if (remaining <= 0) break
                delay(500)
            }
        }
    }

    fun pauseTimer() {
        isRunning.value = false
        timerJob?.cancel()
        cancelAlarm()
        if (wakeLock?.isHeld == true) wakeLock?.release()

        val remaining = ((targetEndTimeMillis - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)
        if (isWorkMode.value) workSecondsRemaining = remaining else breakSecondsRemaining = remaining
        timeLeft.value = remaining

        stopForeground(STOP_FOREGROUND_DETACH)
        updateNotification()
    }

    private fun handleTimerFinished() {
        isRunning.value = false
        timerJob?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()

        stopForeground(STOP_FOREGROUND_REMOVE)

        if (isWorkMode.value) {
            // Logic: Calculate start time based on the fact that 30m just finished.
            // This is robust against pauses and midnight rollovers.
            val totalMinutes = TimerConstants.DEFAULT_WORK_MINS
            val endTime = LocalDateTime.now()
            val startTime = endTime.minusMinutes(totalMinutes)

            serviceScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext).sessionDao()

                if (startTime.toLocalDate() != endTime.toLocalDate()) {
                    // Midnight Split
                    val endOfDay = startTime.toLocalDate().atTime(LocalTime.MAX)
                    val minsOnStartDay = Duration.between(startTime, endOfDay).toMinutes() + 1
                    val minsOnEndDay = (totalMinutes - minsOnStartDay).coerceAtLeast(0)

                    if (minsOnStartDay > 0) {
                        db.insertSession(CompletedSession(
                            durationMinutes = minsOnStartDay,
                            date = startTime.toLocalDate().toString(),
                            timestamp = startTime.toLocalTime().toString().take(5)
                        ))
                    }
                    if (minsOnEndDay > 0) {
                        db.insertSession(CompletedSession(
                            durationMinutes = minsOnEndDay,
                            date = endTime.toLocalDate().toString(),
                            timestamp = "00:00"
                        ))
                    }
                } else {
                    db.insertSession(CompletedSession(
                        durationMinutes = totalMinutes,
                        date = startTime.toLocalDate().toString(),
                        timestamp = startTime.toLocalTime().toString().take(5)
                    ))
                }
            }
            workSecondsRemaining = TimerConstants.DEFAULT_WORK_MINS * 60L
        } else {
            breakSecondsRemaining = TimerConstants.DEFAULT_BREAK_MINS * 60L
        }

        triggerCompletionAlert() // Show alert AFTER db processing
        timeLeft.value = if (isWorkMode.value) workSecondsRemaining else breakSecondsRemaining
    }

    private fun scheduleAlarm(triggerAtWallClockMillis: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback for missing permission
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallClockMillis, pendingIntent)
            return
        }

        try {
            val info = AlarmManager.AlarmClockInfo(triggerAtWallClockMillis, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallClockMillis, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun triggerCompletionAlert() {
        val manager = getSystemService(NotificationManager::class.java)
        val title = if (isWorkMode.value) "FOCUS SESSION DONE" else "BREAK OVER"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val completionNotify = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("Your session is complete.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Required for heads-up
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setFullScreenIntent(pendingIntent, true) // Crucial for overlay
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(alertId, completionNotify)
    }

    fun switchMode(isWork: Boolean) {
        if (isRunning.value) return
        if (isWorkMode.value) workSecondsRemaining = timeLeft.value else breakSecondsRemaining = timeLeft.value
        isWorkMode.value = isWork
        timeLeft.value = if (isWork) workSecondsRemaining else breakSecondsRemaining

        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(alertId)
        updateNotification()
    }

    fun resetTimer() {
        pauseTimer()
        if (isWorkMode.value) workSecondsRemaining = TimerConstants.DEFAULT_WORK_MINS * 60L
        else breakSecondsRemaining = TimerConstants.DEFAULT_BREAK_MINS * 60L
        timeLeft.value = if (isWorkMode.value) workSecondsRemaining else breakSecondsRemaining
        updateNotification()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "CIRC Timer Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", timeLeft.value / 60, timeLeft.value % 60)
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        if (cachedBuilder == null) {
            cachedBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return cachedBuilder!!.setContentTitle(if (isWorkMode.value) "FOCUSING" else "RESTING").setContentText(timeStr).build()
    }

    private fun updateNotification() {
        if (isRunning.value) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(notificationId, buildNotification())
        }
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}