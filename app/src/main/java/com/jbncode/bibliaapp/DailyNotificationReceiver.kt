package com.jbncode.bibliaapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jbncode.bibliaapp.MainActivity // La actividad que se abrirá al hacer clic
import com.jbncode.bibliaapp.NotificationScheduler
import com.jbncode.bibliaapp.R // Para el icono de la notificación
import com.jbncode.bibliaapp.SharedPreferenceBible
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationReceiverEntryPoint {
    fun notificationScheduler(): NotificationScheduler
    fun sharedPreferenceBible(): SharedPreferenceBible
}

class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "daily_verse_channel"
        const val ACTION_SHOW_NOTIFICATION = "com.example.bibliaapp.ACTION_SHOW_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Obtener dependencias manualmente desde el grafo de Hilt
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationReceiverEntryPoint::class.java
        )
        val scheduler = entryPoint.notificationScheduler()
        val sharedPrefs = entryPoint.sharedPreferenceBible()

        when (intent.action) {
            ACTION_SHOW_NOTIFICATION -> {
                // ✅ Verificar permiso antes de mostrar (crítico en Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) return
                }

                showNotification(
                    context,
                    context.getString(R.string.bible_notifications_remind_title),
                    context.getString(R.string.bible_notifications_remind_msg)
                )

                // ✅ Reprogramar para mañana a la misma hora
                // Sin esto la alarma se dispara una vez y nunca más
                if (sharedPrefs.notificationsEnabled.value) {
                    scheduler.scheduleDailyNotification(sharedPrefs.selectedTime.value)
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // El dispositivo reinició: reprogramar si las notificaciones están activas
                if (sharedPrefs.notificationsEnabled.value) {
                    scheduler.scheduleDailyNotification(sharedPrefs.selectedTime.value)
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android Oreo y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.bible_notifications_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.bible_notifications_channel_desc)
                // Puedes configurar más opciones del canal aquí (sonido, vibración, etc.)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingActivityIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_bible_logo) // Reemplaza con tu icono
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingActivityIntent) // Acción al hacer clic
            .setAutoCancel(true) // La notificación se cierra al hacer clic
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
