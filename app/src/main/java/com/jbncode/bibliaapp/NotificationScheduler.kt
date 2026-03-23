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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Tiene permiso → alarma exacta
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        createPendingIntent()
                    )
                } catch (e: SecurityException) {
                    scheduleInexactNotification(calendar)
                }
            } else {
                // No tiene permiso → fallback inexacta (±15 min)
                // Opcionalmente puedes llevar al usuario a ajustes:
                // Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                scheduleInexactNotification(calendar)
            }
        } else {
            // Android 11 y menos → siempre exacta sin necesitar permiso
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                createPendingIntent()
            )
        }
    }

    // Fallback: alarma inexacta (puede llegar con hasta 15 min de retraso,
    // pero SIEMPRE funciona sin permisos especiales)
    private fun scheduleInexactNotification(calendar: Calendar) {

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
