package com.jbncode.bibliaapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jbncode.bibliaapp.notifications.DailyNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject

class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            action = DailyNotificationReceiver.Companion.ACTION_SHOW_NOTIFICATION
        }
        return PendingIntent.getBroadcast(
            context,
            DailyNotificationReceiver.Companion.NOTIFICATION_ID, // Request code único
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun scheduleDailyNotification(time: LocalTime) {
        // ✅ Verificar si podemos programar alarmas exactas antes de intentarlo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // No tenemos permiso: usar alarma inexacta como fallback
                scheduleInexactNotification(time)
                return
            }
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programar para mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            // Para alarmas exactas (necesita permiso SCHEDULE_EXACT_ALARM en Android 12+)
            // Si no puedes/quieres usar alarmas exactas, puedes usar setInexactRepeating o setWindow.
            alarmManager.setExactAndAllowWhileIdle( // Permite que se dispare incluso en modo Doze
                AlarmManager.RTC_WAKEUP, // Despierta el dispositivo para disparar la alarma
                calendar.timeInMillis,
                createPendingIntent()
            )
        } catch (se: SecurityException) {
            // Manejar el caso donde no tienes el permiso SCHEDULE_EXACT_ALARM
            // Podrías pedir al usuario que lo active o usar una alarma no exacta.
            scheduleInexactNotification(time)
            se.printStackTrace()
        }
    }

    // Fallback: alarma inexacta (puede llegar con hasta 15 min de retraso,
    // pero SIEMPRE funciona sin permisos especiales)
    private fun scheduleInexactNotification(time: LocalTime) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }
        // setWindow da una ventana de 10 minutos — más exacto que setInexactRepeating
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            10 * 60 * 1000L, // ventana de 10 minutos
            createPendingIntent()
        )
    }

    fun cancelDailyNotification() {
        alarmManager.cancel(createPendingIntent())
    }
}
