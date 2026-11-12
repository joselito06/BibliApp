package com.jbncode.bibliaapp.ui.feature.reader

import android.os.SystemClock
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.core.tts.TtsController
import com.jbncode.bibliaapp.core.tts.TtsStatus
import com.jbncode.bibliaapp.data.bible.BibleDbFactory
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.data.user.ReadingLogDao
import com.jbncode.bibliaapp.data.user.ReadingLogEntity
import com.jbncode.bibliaapp.domain.bible.BibleRef
import com.jbncode.bibliaapp.domain.bible.Book
import com.jbncode.bibliaapp.domain.bible.Verse
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BibleInfo(val code: String, val title: String, val ntStart: Int)

/**
 * Reemplaza a BibleViewmodel. Lee SOLO de BibleRepository (capa nueva), así que cambiar de
 * Biblia no requiere reiniciar: al cambiar el archivo activo, libros y capítulos se recargan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repo: BibleRepository,
    private val catalog: BibleUseCase,
    private val prefs: SharedPreferenceBible,
    private val readingLog: ReadingLogDao,
    private val tts: TtsController
) : ViewModel() {

    val activeFile: StateFlow<String> = repo.activeFile
        .stateIn(viewModelScope, SharingStarted.Eagerly, BibleDbFactory.DEFAULT_FILE)

    val books: StateFlow<List<Book>> = repo.books()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val info: StateFlow<BibleInfo> = activeFile.mapLatest { file ->
        val e = runCatching { catalog() }.getOrDefault(emptyList()).firstOrNull { it.database_name == file }
        BibleInfo(e?.code ?: "tla", e?.title ?: "TLA", e?.new_testament_number ?: 40)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BibleInfo("tla", "TLA", 40))

    private val _position = MutableStateFlow(BibleRef(prefs.getCurrentBook() ?: "Gen", prefs.getCapitulo()))
    val position: StateFlow<BibleRef> = _position.asStateFlow()

    private val cache = LruCache<String, List<Verse>>(10)

    // Seguimiento de lectura (reading_log)
    private var resumed = false
    private var shownRef: BibleRef? = null
    private var shownAt = 0L

    init {
        // Si la Biblia nueva no tiene el libro/capítulo guardado, se ajusta.
        viewModelScope.launch { books.filter { it.isNotEmpty() }.collect { clamp(it) } }
        // La lectura en voz alta avanza capítulos/libros: el lector la sigue.
        viewModelScope.launch {
            tts.state.filter { it.status != TtsStatus.IDLE && it.book != null }
                .map { it.book!! to it.chapter }.distinctUntilChanged()
                .collect { (b, c) -> open(b, c) }
        }
        viewModelScope.launch {
            tts.state.map { it.status }.distinctUntilChanged()
                .collect { if (it == TtsStatus.IDLE && !resumed) flush() }
        }
    }

    suspend fun loadChapter(book: String, chapter: Int): List<Verse> {
        val file = repo.activeFile.first()
        val key = "$file|$book|$chapter"
        cache.get(key)?.let { return it }
        return repo.chapter(book, chapter).also { cache.put(key, it) }
    }

    // ── Navegación ───────────────────────────────────────────────────────────
    fun open(book: String, chapter: Int = 1) = setPosition(BibleRef(book, chapter))
    fun setChapter(chapter: Int) = setPosition(_position.value.copy(chapter = chapter))

    fun nextChapter() {
        val b = currentBook() ?: return
        val p = _position.value
        if (p.chapter < b.chapters) setChapter(p.chapter + 1) else nextBook()
    }

    fun previousChapter() {
        val p = _position.value
        if (p.chapter > 1) setChapter(p.chapter - 1) else previousBook(toLastChapter = true)
    }

    fun nextBook() {
        val list = books.value; val i = list.indexOfFirst { it.osis == _position.value.book }
        list.getOrNull(i + 1)?.let { open(it.osis) }
    }

    fun previousBook(toLastChapter: Boolean = false) {
        val list = books.value; val i = list.indexOfFirst { it.osis == _position.value.book }
        list.getOrNull(i - 1)?.let { open(it.osis, if (toLastChapter) it.chapters else 1) }
    }

    private fun currentBook() = books.value.firstOrNull { it.osis == _position.value.book }

    private fun clamp(list: List<Book>) {
        val p = _position.value
        val b = list.firstOrNull { it.osis == p.book } ?: list.first()
        val ch = if (b.osis == p.book) p.chapter.coerceIn(1, maxOf(b.chapters, 1)) else 1
        if (b.osis != p.book || ch != p.chapter) setPosition(BibleRef(b.osis, ch))
    }

    private fun setPosition(ref: BibleRef) {
        if (ref == _position.value) return
        flush()
        _position.value = ref
        prefs.saveCurrentBook(ref.book)
        prefs.saveCapitulo(ref.chapter)
        if (resumed || tts.state.value.status == TtsStatus.PLAYING) startTracking()
    }

    // ── Estadísticas de lectura ──────────────────────────────────────────────
    fun onResumed() { resumed = true; if (shownRef == null) startTracking() }

    fun onPaused() {
        resumed = false
        if (tts.state.value.status != TtsStatus.PLAYING) flush()   // escuchando cuenta como leer
    }

    private fun startTracking() { shownRef = _position.value; shownAt = SystemClock.elapsedRealtime() }

    private fun flush() {
        val ref = shownRef ?: return
        shownRef = null
        val secs = ((SystemClock.elapsedRealtime() - shownAt) / 1000).toInt().coerceAtMost(600)
        if (secs < 15) return                       // pasar de largo no cuenta como leer
        viewModelScope.launch(Dispatchers.IO) {
            val count = runCatching { loadChapter(ref.book, ref.chapter).size }.getOrDefault(0)
            readingLog.insert(
                ReadingLogEntity(day = LocalDate.now().toString(), book = ref.book, chapter = ref.chapter, seconds = secs, versesRead = count)
            )
            // Mientras el Inicio siga leyendo SharedPreferences (parte B lo migra):
            prefs.markChapterAsRead(ref.book, ref.chapter)
            prefs.incrementVersesRead(count)
            books.value.firstOrNull { it.osis == ref.book }?.let {
                if (prefs.getChaptersReadForBook(ref.book) >= it.chapters) prefs.markBookAsRead(ref.book)
            }
        }
    }
}
