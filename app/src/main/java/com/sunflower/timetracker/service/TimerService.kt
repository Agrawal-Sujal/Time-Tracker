package com.sunflower.timetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sunflower.timetracker.MainActivity
import com.sunflower.timetracker.data.repository.TimeTrackerRepository
import com.sunflower.timetracker.util.formatDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject
    lateinit var repository: TimeTrackerRepository

    private var tickJob: Job? = null
    private var currentTagName: String = ""
    private var sessionStartMs: Long = 0L
    private var pausedElapsedMs: Long = 0L
    private var isPaused: Boolean = false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "timer_channel"
        const val ACTION_STOP = "com.timetracker.ACTION_STOP"
        const val ACTION_PAUSE = "com.timetracker.ACTION_PAUSE"
        const val ACTION_RESUME = "com.timetracker.ACTION_RESUME"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                lifecycleScope.launch {
                    repository.stopActiveSession()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                isPaused = true
                tickJob?.cancel()
                updateNotification(currentTagName, formatDuration(pausedElapsedMs), paused = true)
                return START_STICKY
            }

            ACTION_RESUME -> {
                isPaused = false
                // re-read from DB to get fresh startTime
                lifecycleScope.launch {
                    val session = repository.getActiveSessionOnce()
                    if (session != null) {
                        sessionStartMs = session.startTime
                        pausedElapsedMs = session.pausedElapsedMs
                    }
                    startTicking()
                }
                return START_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting…", "0:00:00", false))
        observeActiveSession()
        return START_STICKY
    }

    private fun observeActiveSession() {
        lifecycleScope.launch {
            repository.getActiveSession().collectLatest { session ->
                if (session == null) {
                    tickJob?.cancel()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                isPaused = session.isPaused
                sessionStartMs = session.startTime
                pausedElapsedMs = session.pausedElapsedMs

                // Resolve tag name once
                val tags = repository.getAllTags().first()
                currentTagName = tags.find { it.id == session.tagId }?.name ?: "Timer"

                if (!isPaused) startTicking()
                else {
                    tickJob?.cancel()
                    updateNotification(
                        currentTagName,
                        formatDuration(pausedElapsedMs),
                        paused = true
                    )
                }
            }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = lifecycleScope.launch {
            while (isActive && !isPaused) {
                val elapsed = pausedElapsedMs + (System.currentTimeMillis() - sessionStartMs)
                updateNotification(currentTagName, formatDuration(elapsed), paused = false)
                delay(1000L)
            }
        }
    }

    private fun updateNotification(tagName: String, elapsed: String, paused: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(tagName, elapsed, paused))
    }

    private fun buildNotification(title: String, elapsed: String, paused: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseResumeIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply {
                action = if (paused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon =
            if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        val prefix = if (paused) "⏸" else "⏱"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$prefix $title")
            .setContentText(elapsed)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openIntent)
            .addAction(icon, if (paused) "Resume" else "Pause", pauseResumeIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Notifications",
            NotificationManager.IMPORTANCE_LOW
        )
            .apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}
