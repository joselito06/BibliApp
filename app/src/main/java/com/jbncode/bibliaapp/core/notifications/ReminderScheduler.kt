package com.jbncode.bibliaapp.core.notifications

import com.jbncode.bibliaapp.NotificationScheduler
import com.jbncode.bibliaapp.SharedPreferenceBible
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Une los ajustes de cada tipo con las alarmas del sistema. */
@Singleton
class ReminderScheduler @Inject constructor(
    private val alarms: NotificationScheduler,
    private val prefs: SharedPreferenceBible,
    private val store: ReminderStore
) {
    private fun enabled(t: ReminderType) = when (t) {
        ReminderType.VERSE -> prefs.notificationsEnabled.value
        ReminderType.EVENING -> store.eveningEnabled.value
    }

    private fun time(t: ReminderType): LocalTime = when (t) {
        ReminderType.VERSE -> prefs.selectedTime.value
        ReminderType.EVENING -> store.eveningTime.value
    }

    fun rescheduleOne(t: ReminderType) { if (enabled(t)) alarms.schedule(t, time(t)) else alarms.cancel(t) }
    fun rescheduleAll() = ReminderType.entries.forEach(::rescheduleOne)
}
