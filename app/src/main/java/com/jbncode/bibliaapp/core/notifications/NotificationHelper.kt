package com.jbncode.bibliaapp.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jbncode.bibliaapp.MainActivity
import com.jbncode.bibliaapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ReminderStore
) {
    @SuppressLint("MissingPermission")
    fun show(type: ReminderType, content: ReminderContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(type)
        // Al tocarla, la app abre directo donde corresponde (MainActivity lee EXTRA_DESTINATION).
        val open = PendingIntent.getActivity(
            context, type.requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, content.destination)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(R.drawable.ic_launcher_bible_logo)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(type.notificationId, notification)
        store.markSent()
    }

    private fun ensureChannel(type: ReminderType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val (name, desc) = when (type) {
            ReminderType.VERSE -> R.string.bible_notifications_channel_name to R.string.bible_notifications_channel_desc
            ReminderType.EVENING -> R.string.reminder_channel_name to R.string.reminder_channel_desc
        }
        val channel = NotificationChannel(type.channelId, context.getString(name), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
