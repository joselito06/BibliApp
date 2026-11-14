package com.jbncode.bibliaapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jbncode.bibliaapp.core.notifications.ReminderType
import com.jbncode.bibliaapp.notifications.DailyNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject

class NotificationScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(type: ReminderType): PendingIntent = PendingIntent.getBroadcast(
        context, type.requestCode,
        Intent(context, DailyNotificationReceiver::class.java).apply {
            action = DailyNotificationReceiver.ACTION_SHOW
            putExtra(DailyNotificationReceiver.EXTRA_TYPE, type.name)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun schedule(type: ReminderType, time: LocalTime) {
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour); set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }.timeInMillis

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(type))
                return
            } catch (_: SecurityException) { /* cae a la ventana */ }
        }
        // Sin permiso de alarma exacta: ventana de 10 min, siempre funciona.
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60 * 1000L, pending(type))
    }

    fun cancel(type: ReminderType) = alarmManager.cancel(pending(type))

    // Compatibilidad con SettingsViewmodel y OnboardingViewModel (versículo del día).
    fun scheduleDailyNotification(time: LocalTime) = schedule(ReminderType.VERSE, time)
    fun cancelDailyNotification() = cancel(ReminderType.VERSE)
}
