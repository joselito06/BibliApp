package com.jbncode.bibliaapp.core.notifications

import android.content.Context
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.data.user.ReadingLogDao
import com.jbncode.bibliaapp.domain.usecase.StreakCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Decide QUÉ decir (y si decir algo) en el momento en que suena cada alarma. */
@Singleton
class ReminderPlanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verse: DailyVerseProvider,
    private val log: ReadingLogDao,
    private val bible: BibleRepository,
    private val prefs: SharedPreferenceBible,
    private val store: ReminderStore
) {
    suspend fun build(type: ReminderType): ReminderContent? {
        if (!store.shouldSend()) return null
        return when (type) {
            ReminderType.VERSE -> verseContent()
            ReminderType.EVENING -> eveningContent()
        }
    }

    private suspend fun verseContent(): ReminderContent {
        val v = verse.today()
        val title = context.getString(R.string.bible_notifications_remind_title)
        val text = if (v?.verse_content.isNullOrBlank()) context.getString(R.string.bible_notifications_remind_msg)
        else "${v!!.verse_content} — ${v.verse_direction.orEmpty()}".trim().trimEnd('—').trim()
        return ReminderContent(title, text, destination = "home")
    }

    private suspend fun eveningContent(): ReminderContent? {
        val today = LocalDate.now()
        val days = log.observeDays().first().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        if (today in days) return null                       // ya leyó hoy: no molestar

        val streak = StreakCalculator.current(days, today)  // hoy no cuenta aún: sale desde ayer
        val osis = prefs.getCurrentBook() ?: "Gen"
        val name = bible.books().first().firstOrNull { it.osis == osis }?.name ?: osis
        val ref = "$name ${prefs.getCapitulo()}"

        return if (streak >= 2) {
            ReminderContent(
                context.resources.getQuantityString(R.plurals.reminder_streak_title, streak, streak),
                context.getString(R.string.reminder_streak_text, ref), "reader"
            )
        } else {
            ReminderContent(
                context.getString(R.string.reminder_continue_title),
                context.getString(R.string.reminder_continue_text, ref), "reader"
            )
        }
    }
}
