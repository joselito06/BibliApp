package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.core.tts.TtsController
import com.jbncode.bibliaapp.core.tts.TtsSettings
import com.jbncode.bibliaapp.core.tts.TtsState
import com.jbncode.bibliaapp.core.tts.TtsStatus
import com.jbncode.bibliaapp.core.tts.VoiceOption
import com.jbncode.bibliaapp.di.RoomModule
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.domain.model.Books
import com.jbncode.bibliaapp.domain.usescases.BookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Paso 3: toda la lógica de voz salió a core/tts/TtsController.
 * Este ViewModel conserva los mismos nombres públicos para no tocar BibleScreen/MainActivity.
 */
@HiltViewModel
class BibleViewmodel @Inject constructor(
    private val booksUC: BookUseCase,
    private val shared: SharedPreferenceBible,
    private val tts: TtsController,
    private val ttsSettings: TtsSettings,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val validChapterItem = MutableStateFlow(true)
    val numChapterItem = MutableStateFlow(0)
    val sharedComp = MutableStateFlow(shared)

    // ── Voz (fachada sobre TtsController) ────────────────────────────────────
    val ttsState: StateFlow<TtsState> = tts.state

    val isSpeaking: StateFlow<Boolean> = tts.state
        .map { it.status == TtsStatus.PLAYING || it.status == TtsStatus.LOADING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isPaused: StateFlow<Boolean> = tts.state
        .map { it.status == TtsStatus.PAUSED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** (capítulo, versículo) que se está leyendo, o null. Para resaltar en pantalla. */
    val speakingRef: StateFlow<Pair<Int, Int>?> = tts.state
        .map { if (it.status == TtsStatus.PLAYING) it.chapter to it.verse else null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Compatibilidad con BibleScreen (ya no hay nada que inicializar ni reiniciar).
    val currentTTSLanguage: StateFlow<AppLanguage> = shared.appLanguage
    val isTTSInitialized: StateFlow<Boolean> = MutableStateFlow(true)

    private val _announceVerseNumbers = MutableStateFlow(shared.getAnnounceVerseNumbers())
    val announceVerseNumbers: StateFlow<Boolean> = _announceVerseNumbers.asStateFlow()

    private val _announceChapterTitles = MutableStateFlow(shared.getAnnounceChapterTitles())
    val announceChapterTitles: StateFlow<Boolean> = _announceChapterTitles.asStateFlow()

    val speechRate: StateFlow<Float> = ttsSettings.rate

    fun setAnnounceVerseNumbers(enabled: Boolean) {
        _announceVerseNumbers.value = enabled
        shared.saveAnnounceVerseNumbers(enabled)
    }

    fun setAnnounceChapterTitles(enabled: Boolean) {
        _announceChapterTitles.value = enabled
        shared.saveAnnounceChapterTitles(enabled)
    }

    fun speakNextVerse() = tts.play(_Libro.value, _Capitulo.value)
    fun startReading() = speakNextVerse()
    fun pauseReading() = tts.pause()
    fun resumeReading() = tts.resume()
    fun stopReading() = tts.stop()
    fun setSpeechRate(v: Float) = tts.setRate(v)
    fun selectVoice(name: String?) = tts.setVoice(name)
    suspend fun voiceOptions(): List<VoiceOption> = tts.voiceOptions()

    @Deprecated("El servicio lo inicia y detiene TtsController")
    fun startReadingService(title: String, verseText: String) = Unit
    @Deprecated("El servicio lo inicia y detiene TtsController")
    fun stopReadingService() = Unit

    private var followJob: Job? = null

    /** Antes creaba el motor TTS; ahora solo engancha el auto-scroll al versículo leído. */
    fun initializeTTS(context: Context, coroutineScope: CoroutineScope, listState: LazyListState) {
        followJob?.cancel()
        followJob = coroutineScope.launch {
            speakingRef.filterNotNull().collect { (chapter, verse) ->
                val idx = _getAllBiblia.value.firstOrNull()?.Verse
                    ?.indexOfFirst { it.Chapter == chapter && it.Verse == verse } ?: -1
                if (idx >= 0) {
                    try { listState.animateScrollToItem(idx + 1) } catch (e: Exception) { Log.w("TTS", "scroll: ${e.message}") }
                }
            }
        }
    }

    // ── Lector (sin cambios) ─────────────────────────────────────────────────
    private val _getBooks = MutableStateFlow<List<Books>>(listOf())
    val getBooks: StateFlow<List<Books>> = _getBooks

    private val _getAllBook = MutableStateFlow<List<Books>>(listOf())
    val getAllBook: StateFlow<List<Books>> = _getAllBook

    private val _getAllBiblia = MutableStateFlow<List<Books>>(listOf())
    val getAllBiblia: StateFlow<List<Books>> = _getAllBiblia

    private var _Capitulo = MutableStateFlow(shared.getCapitulo())
    var capitulo: StateFlow<Int> = _Capitulo

    private var _Libro = MutableStateFlow(shared.getCurrentBook() ?: "Gen")
    var libro: StateFlow<String> = _Libro

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private var _LibroIndex = MutableStateFlow(0)
    var libroIndex: StateFlow<Int> = _LibroIndex

    private var _numLibro = MutableStateFlow(1)
    var numLibro: StateFlow<Int> = _numLibro

    init {
        RoomModule.forceCloseAllConnections(context)
        loadSavedState()
        followTtsChapter()
    }

    /** Si la lectura avanza de capítulo o de libro, el lector la sigue. */
    private fun followTtsChapter() {
        viewModelScope.launch {
            tts.state
                .filter { it.status != TtsStatus.IDLE && it.book != null }
                .map { it.book!! to it.chapter }
                .distinctUntilChanged()
                .collect { (b, c) ->
                    if (b != _Libro.value) getBook(b) else if (c != _Capitulo.value) setCapitulo(c)
                }
        }
    }

    private fun loadSavedState() {
        viewModelScope.launch(Dispatchers.IO) {
            val booksSelect = booksUC.invoke()
            _getAllBook.value = booksSelect

            _getBooks.value = booksUC.GetAllBook()

            val savedBook = shared.getCurrentBook() ?: booksSelect.first().Osis
            _Libro.value = savedBook
            _LibroIndex.value = booksSelect.indexOfFirst { it.Osis == savedBook }

            val list = booksUC.GetAllBookAndVerses(savedBook)
            _getAllBiblia.value = list
            _Capitulo.value = shared.getCapitulo()

            validChapterItem.value = true
            numChapterItem.value = 0
            sharedComp.value = shared
        }
    }

    fun scrollToChapter(coroutineScope: CoroutineScope, listState: LazyListState) {
        val chapterNum = _Capitulo.value.toString()
        val index = _getAllBiblia.value
            .firstOrNull { v -> v.Osis == _Libro.value }
            ?.Verse
            ?.indexOfFirst { it.ChapterNum.toString() == chapterNum }
            ?: -1

        if (index >= 0) {
            _currentIndex.value = index
            coroutineScope.launch { listState.animateScrollToItem(index) }
        } else {
            Log.e("BibleViewModel", "No se encontró el capítulo $chapterNum")
        }
    }

    fun plusBook() {
        viewModelScope.launch {
            if (_LibroIndex.value < _getAllBook.value.size - 1) {
                _LibroIndex.value += 1
                _Libro.value = _getAllBook.value[_LibroIndex.value].Osis

                try {
                    _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
                } catch (e: Exception) {
                    Log.e("Error PlusBook", e.message.toString())
                }

                _Capitulo.value = 1
                shared.saveCurrentBook(_Libro.value)
                shared.saveCapitulo(1)
            }
        }
    }

    fun getBook(osis: String) {
        viewModelScope.launch {
            val previousBook = _Libro.value

            _Libro.value = _getAllBook.value.first { v -> v.Osis == osis }.Osis
            _LibroIndex.value = _getAllBook.value.indexOfFirst { v -> v.Osis == osis }

            try {
                _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
            } catch (e: Exception) {
                Log.e("Error LessBook", e.message.toString())
            }
            _Capitulo.value = 1
            shared.saveCurrentBook(_Libro.value)
            shared.saveCapitulo(1)

            checkBookCompletion(previousBook)
        }
    }

    fun lessBook() {
        viewModelScope.launch {
            if (_LibroIndex.value >= 1) {
                _LibroIndex.value -= 1
                _Libro.value = _getAllBook.value[_LibroIndex.value].Osis

                try {
                    _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
                } catch (e: Exception) {
                    Log.e("Error LessBook", e.message.toString())
                }

                _Capitulo.value = 1
                shared.saveCurrentBook(_Libro.value)
                shared.saveCapitulo(1)
            }
        }
    }

    fun aumentarCapitulo() {
        if (_Capitulo.value <= _getAllBiblia.value.first().Chapters - 1) {
            _Capitulo.value += 1
            shared.saveCapitulo(_Capitulo.value)
        }
    }

    fun decrementCapitulo() {
        if (_Capitulo.value >= 2) {
            _Capitulo.value -= 1
            shared.saveCapitulo(_Capitulo.value)
        }
    }

    fun setLibro(value: String) {
        _Libro.value = value
        shared.saveCurrentBook(value)
    }

    fun setCapitulo(value: Int) {
        val previousChapter = _Capitulo.value
        _Capitulo.value = value
        shared.saveCapitulo(value)

        if (value > previousChapter) {
            trackChapterCompletion(_Libro.value, previousChapter)
        }
    }

    private fun trackChapterCompletion(bookOsis: String, chapter: Int) {
        shared.markChapterAsRead(bookOsis, chapter)
        shared.addReadingTime(4)

        val currentBook = _getAllBiblia.value.firstOrNull()
        currentBook?.let { book ->
            val versesInChapter = book.Verse.count { it.ChapterNum == chapter }
            shared.incrementVersesRead(versesInChapter)
        }

        Log.d("BibleStats", "Capítulo completado: $bookOsis capítulo $chapter")
    }

    private fun checkBookCompletion(bookOsis: String) {
        val book = _getAllBook.value.find { it.Osis == bookOsis }
        book?.let {
            val chaptersReadInBook = shared.getChaptersReadForBook(bookOsis)
            if (chaptersReadInBook >= it.Chapters) {
                shared.markBookAsRead(bookOsis)
                Log.d("BibleStats", "¡Libro completado!: ${it.Human}")
                Toast.makeText(context, context.getString(R.string.book_completed_toast, it.Human), Toast.LENGTH_LONG).show()
            }
        }
    }
}
