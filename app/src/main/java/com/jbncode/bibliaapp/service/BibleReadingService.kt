package com.jbncode.bibliaapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jbncode.bibliaapp.MainActivity
import com.jbncode.bibliaapp.R


class BibleReadingService : Service() {
    private val CHANNEL_ID = "bible_reading_channel"
    private val NOTIFICATION_ID = 1

    // Acciones constantes
    companion object {
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_PLAY = "ACTION_PLAY"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val isPaused = intent?.getBooleanExtra("isPaused", false) ?: false

        when (action) {
            ACTION_STOP -> {
                // Notificar a la app que debe detener el TTS
                val broadcastIntent = Intent("STOP_TTS").apply { setPackage(packageName) }
                sendBroadcast(broadcastIntent)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                val broadcastIntent = Intent("PAUSE_TTS").apply { setPackage(packageName) }
                sendBroadcast(broadcastIntent)
                // Actualizar notificación a estado pausa
                updateNotification(intent, true)
            }
            ACTION_PLAY -> {
                val broadcastIntent = Intent("PLAY_TTS").apply { setPackage(packageName) }
                sendBroadcast(broadcastIntent)
                // Actualizar notificación a estado reproduciendo
                updateNotification(intent, false)
            }
        }

        if (action == null) {
            updateNotification(intent, isPaused)
        }

        return START_STICKY
    }

    private fun updateNotification(intent: Intent?, isPaused: Boolean) {
        val title = intent?.getStringExtra("title") ?: getString(R.string.bible_notification_playing)
        val content = intent?.getStringExtra("content") ?: getString(R.string.bible_notification_default_content)

        val notification = createNotification(title, content, isPaused)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(title: String, content: String, isPaused: Boolean): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para STOP
        val stopPendingIntent = PendingIntent.getService(
            this, 1, Intent(this, BibleReadingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent para PLAY/PAUSE
        val playPauseAction = if (isPaused) ACTION_PLAY else ACTION_PAUSE
        val playPauseIcon = if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
        val playPauseTitle = if (isPaused) getString(R.string.bible_notification_play) else getString(R.string.bible_notification_pause)

        val playPausePendingIntent = PendingIntent.getService(
            this, 2, Intent(this, BibleReadingService::class.java).apply {
                action = playPauseAction
                putExtra("title", title)
                putExtra("content", content)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_bible_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            // Botón PAUSA/PLAY (Posición 0)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            // Botón DETENER (Posición 1)
            .addAction(R.drawable.ic_stop, getString(R.string.bible_notification_stop), stopPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1) // Muestra ambos botones en vista pequeña
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bible_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}