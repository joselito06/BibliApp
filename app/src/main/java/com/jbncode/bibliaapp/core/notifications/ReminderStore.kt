package com.jbncode.bibliaapp.core.notifications

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Ajustes del recordatorio nocturno y memoria de cuándo se avisó / cuándo abrió la app. */
@Singleton
class ReminderStore @Inject constructor(@ApplicationContext context: Context) {
    private val p = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    private val _eveningEnabled = MutableStateFlow(p.getBoolean("evening_enabled", false))
    val eveningEnabled: StateFlow<Boolean> = _eveningEnabled.asStateFlow()

    private val _eveningTime = MutableStateFlow(LocalTime.of(p.getInt("evening_h", 20), p.getInt("evening_m", 0)))
    val eveningTime: StateFlow<LocalTime> = _eveningTime.asStateFlow()

    fun setEveningEnabled(on: Boolean) { p.edit { putBoolean("evening_enabled", on) }; _eveningEnabled.value = on }
    fun setEveningTime(t: LocalTime) {
        p.edit { putInt("evening_h", t.hour); putInt("evening_m", t.minute) }
        _eveningTime.value = t
    }

    private val lastSentAt get() = p.getLong("last_sent", 0L)
    private val lastOpenAt get() = p.getLong("last_open", 0L)
    private val ignored get() = p.getInt("ignored", 0)

    fun markOpened(now: Long = System.currentTimeMillis()) { p.edit { putLong("last_open", now) } }

    fun shouldSend(now: Long = System.currentTimeMillis()) =
        ReminderPolicy.shouldSend(now, lastSentAt, lastOpenAt, ignored)

    fun markSent(now: Long = System.currentTimeMillis()) {
        val next = ReminderPolicy.afterSend(lastSentAt, lastOpenAt, ignored)
        p.edit { putInt("ignored", next); putLong("last_sent", now) }
    }
}
