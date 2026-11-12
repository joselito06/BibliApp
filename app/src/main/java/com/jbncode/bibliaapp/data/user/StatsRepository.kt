package com.jbncode.bibliaapp.data.user

import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.domain.usecase.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class HomeStats(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val last7: List<Boolean> = List(7) { false },   // del más antiguo a hoy
    val readToday: Boolean = false,
    val chaptersRead: Int = 0,
    val totalSeconds: Int = 0,
    val versesRead: Int = 0,
    val biblePercent: Int = 0,
    val completedBooks: Set<String> = emptySet(),
    val chaptersByBook: Map<String, Int> = emptyMap()
)

@Singleton
class StatsRepository @Inject constructor(private val dao: ReadingLogDao, bible: BibleRepository) {

    val stats: Flow<HomeStats> = combine(
        combine(dao.observeDays(), dao.observeTotalSeconds(), dao.observeTotalVerses()) { d, s, v -> Triple(d, s, v) },
        combine(dao.observeChaptersRead(), dao.observeBookProgress(), bible.books()) { c, p, b -> Triple(c, p, b) }
    ) { (dayStrings, seconds, verses), (chapters, progress, books) ->
        val today = LocalDate.now()
        val days = dayStrings.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        val totalChapters = books.sumOf { it.chapters }.coerceAtLeast(1)
        HomeStats(
            streak = StreakCalculator.current(days, today),
            bestStreak = StreakCalculator.best(days),
            last7 = (6 downTo 0).map { today.minusDays(it.toLong()) in days },
            readToday = today in days,
            chaptersRead = chapters,
            totalSeconds = seconds,
            versesRead = verses,
            biblePercent = (chapters * 100 / totalChapters).coerceIn(0, 100),
            completedBooks = progress.filter { p -> books.firstOrNull { it.osis == p.book }?.let { p.chapters >= it.chapters } == true }
                .map { it.book }.toSet(),
            chaptersByBook = progress.associate { it.book to it.chapters }
        )
    }
}

/** Una sola vez: pasa el progreso de SharedPreferences (sistema anterior) a la base de datos. */
@Singleton
class StatsMigration @Inject constructor(private val prefs: SharedPreferenceBible, private val dao: ReadingLogDao) {
    suspend fun runIfNeeded() {
        if (prefs.isStatsMigrated()) return
        val last = prefs.getLastReadingDate()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val rows = mutableListOf<ReadingLogEntity>()
        prefs.getChaptersRead().forEach { s ->               // formato "Joh-3"
            val i = s.lastIndexOf('-')
            if (i > 0) s.substring(i + 1).toIntOrNull()?.let { ch ->
                rows += ReadingLogEntity(day = last.toString(), book = s.substring(0, i), chapter = ch, seconds = 0, versesRead = 0)
            }
        }
        val minutes = prefs.getTotalReadingTime()
        if (minutes > 0 || prefs.getVersesRead() > 0) {
            rows += ReadingLogEntity(day = last.toString(), book = "LEGACY", chapter = 0, seconds = minutes * 60, versesRead = prefs.getVersesRead())
        }
        repeat(prefs.getReadingStreak()) { i ->               // conserva la racha que ya llevaba
            rows += ReadingLogEntity(day = last.minusDays(i.toLong()).toString(), book = "LEGACY", chapter = 0, seconds = 0, versesRead = 0)
        }
        if (rows.isNotEmpty()) dao.insertAll(rows)
        prefs.markStatsMigrated()
    }
}
