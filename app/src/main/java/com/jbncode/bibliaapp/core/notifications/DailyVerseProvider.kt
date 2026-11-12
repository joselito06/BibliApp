package com.jbncode.bibliaapp.core.notifications

import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.domain.usescases.VersesDailyUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** El versículo del día para la Biblia activa (misma lógica que VersesViewmodel, ahora para segundo plano). */
@Singleton
class DailyVerseProvider @Inject constructor(
    private val useCase: VersesDailyUseCase,
    private val bible: BibleRepository,
    private val prefs: SharedPreferenceBible
) {
    suspend fun today(): VersesDaily? {
        val code = bible.activeFile.first().removeSuffix(".sqlite3").lowercase()
        val all = runCatching { useCase("VersosDiarios") }.getOrDefault(emptyList())
        val list = all.filter { it.code.equals(code, true) }.ifEmpty { all.filter { it.code.equals("tla", true) } }
        return if (list.isEmpty()) null else prefs.getVerseOfTheDay(list)
    }
}
