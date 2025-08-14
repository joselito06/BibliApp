package com.example.bibliaapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bibliaapp.MainActivity // La actividad que se abrirá al hacer clic
import com.example.bibliaapp.R // Para el icono de la notificación

class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "daily_verse_channel"
        const val ACTION_SHOW_NOTIFICATION = "com.example.bibliaapp.ACTION_SHOW_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_NOTIFICATION || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Si es BOOT_COMPLETED, necesitarías reprogramar la alarma aquí,
            // leyendo la hora guardada de SharedPreferences.
            // Por ahora, nos enfocamos en mostrar la notificación cuando la alarma se dispara.

            // Si la acción es solo para mostrar la notificación (no boot_completed), crea y muestra.
            if (intent.action == ACTION_SHOW_NOTIFICATION) {
                showNotification(context, "Versículo del Día", "¡No olvides leer tu porción de hoy!")
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android Oreo y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones Diarias de Versículos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para el recordatorio diario del versículo."
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
            .setSmallIcon(R.drawable.ic_launcher_background) // Reemplaza con tu icono
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingActivityIntent) // Acción al hacer clic
            .setAutoCancel(true) // La notificación se cierra al hacer clic
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
