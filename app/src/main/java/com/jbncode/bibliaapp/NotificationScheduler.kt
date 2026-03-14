package com.jbncode.bibliaapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jbncode.bibliaapp.notifications.DailyNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject

class NotificationScheduler @Inject constructor(@ApplicationContext private val context: Context) {

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
            // Para alarmas repetitivas (más eficiente pero menos exacto a veces)
            // alarmManager.setRepeating(
            //     AlarmManager.RTC_WAKEUP,
            //     calendar.timeInMillis,
            //     AlarmManager.INTERVAL_DAY,
            //     createPendingIntent()
            // )
        } catch (se: SecurityException) {
            // Manejar el caso donde no tienes el permiso SCHEDULE_EXACT_ALARM
            // Podrías pedir al usuario que lo active o usar una alarma no exacta.
            se.printStackTrace()
        }
    }

    fun cancelDailyNotification() {
        alarmManager.cancel(createPendingIntent())
    }
}
