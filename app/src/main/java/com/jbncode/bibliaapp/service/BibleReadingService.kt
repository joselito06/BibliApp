package com.jbncode.bibliaapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jbncode.bibliaapp.MainActivity
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.core.tts.TtsController
import com.jbncode.bibliaapp.core.tts.TtsState
import com.jbncode.bibliaapp.core.tts.TtsStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Solo mantiene viva la lectura (servicio en primer plano) y dibuja la notificación.
 * El audio lo maneja TtsController; ya no hay broadcasts hacia la Activity.
 */
@AndroidEntryPoint
class BibleReadingService : Service() {

    @Inject lateinit var controller: TtsController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_PLAY = "ACTION_PLAY"
        private const val CHANNEL_ID = "bible_reading_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.bible_notification_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_PLAY -> controller.resume()
            ACTION_STOP -> { controller.stop(); stopSelf(); return START_NOT_STICKY }
        }
        // Debe llamarse enseguida tras startForegroundService.
        startForeground(NOTIFICATION_ID, build(controller.state.value))
        if (observer == null) {
            observer = scope.launch {
                controller.state.collect { st ->
                    if (st.status == TtsStatus.IDLE) {
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, build(st))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun action(name: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, BibleReadingService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun build(st: TtsState): Notification {
        val paused = st.status == TtsStatus.PAUSED
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (st.bookName.isBlank()) getString(R.string.bible_notification_playing) else "${st.bookName} ${st.chapter}"
        val text = if (st.verse > 0) "${getString(R.string.tts_verse_label)} ${st.verse}"
        else getString(R.string.bible_notification_default_content)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_bible_logo)
            .setContentIntent(open).setOngoing(true).setSilent(true)
            .addAction(
                if (paused) R.drawable.ic_play else R.drawable.ic_pause,
                getString(if (paused) R.string.bible_notification_play else R.string.bible_notification_pause),
                action(if (paused) ACTION_PLAY else ACTION_PAUSE, 2)
            )
            .addAction(R.drawable.ic_stop, getString(R.string.bible_notification_stop), action(ACTION_STOP, 1))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
