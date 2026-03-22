package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.di.RoomModule
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.domain.model.Books
import com.jbncode.bibliaapp.domain.model.Verses
import com.jbncode.bibliaapp.domain.usescases.BookUseCase
import com.jbncode.bibliaapp.service.BibleReadingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BibleViewmodel @Inject constructor(
    private val booksUC: BookUseCase,
    private val shared: SharedPreferenceBible,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val validChapterItem = MutableStateFlow(true)
    val numChapterItem = MutableStateFlow(0)
    val sharedComp = MutableStateFlow(shared)

    private var tts: TextToSpeech? = null
    private val _currentVerseIndex = MutableStateFlow(0)
    private val _isSpeaking = MutableStateFlow(false)
    private val _isPaused = MutableStateFlow(false)
    private val _pausedText = MutableStateFlow("")

    // Nuevo: Estado para el idioma del TTS
    private val _currentTTSLanguage = MutableStateFlow(AppLanguage.fromCode(shared.getAppLanguageCode()))
    val currentTTSLanguage: StateFlow<AppLanguage> = _currentTTSLanguage

    // Nuevo: Estado para indicar si TTS está inicializado
    private val _isTTSInitialized = MutableStateFlow(false)
    val isTTSInitialized: StateFlow<Boolean> = _isTTSInitialized

    //val currentVerseIndex: StateFlow<Int> get() = _currentVerseIndex
    val isSpeaking: StateFlow<Boolean> get() = _isSpeaking
    val isPaused: StateFlow<Boolean> get() = _isPaused
    val pausedText: StateFlow<String> get() = _pausedText

    // NUEVAS CONFIGURACIONES PARA TTS
    private val _announceVerseNumbers = MutableStateFlow(true) // Configuración para anunciar números de versículos
    val announceVerseNumbers: StateFlow<Boolean> = _announceVerseNumbers.asStateFlow()

    private val _announceChapterTitles = MutableStateFlow(true) // Configuración para anunciar títulos
    val announceChapterTitles: StateFlow<Boolean> = _announceChapterTitles.asStateFlow()


    private val _getBooks = MutableStateFlow<List<Books>>(listOf())
    val getBooks: StateFlow<List<Books>> = _getBooks

    private val _getAllBook = MutableStateFlow<List<Books>>(listOf())
    val getAllBook: StateFlow<List<Books>> = _getAllBook

    private val _getAllBiblia = MutableStateFlow<List<Books>>(listOf())
    val getAllBiblia : StateFlow<List<Books>> = _getAllBiblia

    private var _Capitulo = MutableStateFlow(shared.getCapitulo())
    var capitulo : StateFlow<Int> = _Capitulo

    private var _Libro = MutableStateFlow(shared.getCurrentBook() ?: "Gen")
    var libro : StateFlow<String> = _Libro

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private var _LibroIndex = MutableStateFlow(0)
    var libroIndex : StateFlow<Int> = _LibroIndex

    private var _numLibro = MutableStateFlow(1)
    var numLibro : StateFlow<Int> = _numLibro

    init {
        RoomModule.forceCloseAllConnections(context)
        loadSavedState() // Cargar el último libro y capítulo al iniciar la app
        // Observar cambios en el idioma desde SharedPreferences
        observeLanguageChanges()

    }

    override fun onCleared() {
        super.onCleared()
        // Liberar el motor de TTS
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            // Observar cambios en el idioma de la app usando tu StateFlow existente
            shared.appLanguage.collect { newLanguage ->
                if (newLanguage != _currentTTSLanguage.value && _isTTSInitialized.value) {
                    _currentTTSLanguage.value = newLanguage
                    updateTTSLanguage(newLanguage)
                }
            }
        }
    }

    private fun loadSavedState() {
        viewModelScope.launch(Dispatchers.IO) {
            val booksSelect = booksUC.invoke()
            _getAllBook.value = booksSelect

            _getBooks.value = booksUC.GetAllBook()

            // Buscar el índice del libro guardado
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

    /*fun initializeTTS(context: Context, coroutineScope: CoroutineScope, listState: LazyListState) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Idioma no soportado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error al inicializar TextToSpeech", Toast.LENGTH_SHORT).show()
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _currentVerseIndex.value = utteranceId?.toInt() ?: 0
                    // Aquí podemos agregar lógica para hacer scroll o hacer otras cosas.
                    coroutineScope.launch {
                        listState.animateScrollToItem(utteranceId?.toInt() ?: 0)
                    }

                }

                override fun onDone(utteranceId: String?) {
                    val nextIndex = (utteranceId?.toInt() ?: 0) + 1

                    // Si terminamos el capítulo, pasamos al siguiente
                    if (nextIndex >= _getAllBiblia.value.first().Verse.size) {
                        val nextChapter = capitulo.value + 1
                        if (nextChapter < _getAllBiblia.value.first().Chapters) {
                            // Mencionar el nuevo capítulo
                            val textoIntro = "${_getAllBiblia.value.first().Human}, Capítulo $nextChapter"
                            tts?.speak(textoIntro, TextToSpeech.QUEUE_FLUSH, null, "chapter_intro")

                            // Luego, leer el primer versículo del nuevo capítulo
                            tts?.speak(_getAllBiblia.value.first().Verse[0].Contenido, TextToSpeech.QUEUE_ADD, null, "0")

                            setCapitulo(nextChapter)
                        } else {
                            _isSpeaking.value = false // Si no hay más capítulos, detener lectura
                        }
                    } else {
                        // Leer solo el verso (sin repetir libro y capítulo)
                        tts?.speak(_getAllBiblia.value.first().Verse[nextIndex].Contenido, TextToSpeech.QUEUE_FLUSH, null, nextIndex.toString())
                    }
                }

                override fun onError(utteranceId: String?) {}
            })
        }
    }*/

    // Métodos para cambiar configuraciones
    fun setAnnounceVerseNumbers(enabled: Boolean) {
        _announceVerseNumbers.value = enabled
        // Optionally save to SharedPreferences
        shared.saveAnnounceVerseNumbers(enabled)
    }

    fun setAnnounceChapterTitles(enabled: Boolean) {
        _announceChapterTitles.value = enabled
        // Optionally save to SharedPreferences
        shared.saveAnnounceChapterTitles(enabled)
    }

    // FUNCIONES TTS MEJORADAS CON SOPORTE PARA IDIOMAS

    // Función para obtener strings localizados
    private fun getLocalizedString(resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }

    // Función para obtener el nombre completo del libro
    private fun getBookDisplayName(bookCode: String?): String {
        val currentBook = _getAllBiblia.value.firstOrNull()
        return currentBook?.Human ?: bookCode ?: getLocalizedString(R.string.unknown_book)
    }

    fun initializeTTS(context: Context, coroutineScope: CoroutineScope, listState: LazyListState) {
        // Detener TTS anterior si existe
        tts?.shutdown()

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTTSInitialized.value = true
                // Configurar el idioma inicial
                val currentLanguage = _currentTTSLanguage.value
                setTTSLanguage(currentLanguage)

                setupTTSProgressListener(coroutineScope, listState)
            } else {
                _isTTSInitialized.value = false
                Toast.makeText(context, getLocalizedString(R.string.tts_error_initialization), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTTSProgressListener(coroutineScope: CoroutineScope, listState: LazyListState) {

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            private var startTime: Long = 0

            override fun onStart(utteranceId: String?) {
                startTime = System.currentTimeMillis()
                // Actualizar el índice actual basado en el utteranceId
                utteranceId?.toIntOrNull()?.let { index ->
                    _currentVerseIndex.value = index
                    // Hacer scroll al versículo actual
                    coroutineScope.launch {
                        try {
                            listState.animateScrollToItem(index)
                        } catch (e: Exception) {
                            Log.e("TTS", "Error en scroll: ${e.message}")
                        }
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                // ✅ NUEVO: Calcular tiempo de escucha
                val listeningTime = (System.currentTimeMillis() - startTime) / 60000 // en minutos
                if (listeningTime > 0) {
                    shared.addReadingTime(listeningTime.toInt())
                }

                val currentIndex = utteranceId?.toIntOrNull() ?: _currentVerseIndex.value
                val nextIndex = currentIndex + 1

                val currentBook = _getAllBiblia.value.firstOrNull()
                if (currentBook == null) {
                    _isSpeaking.value = false
                    return
                }

                // Verificar si hay más versículos en el capítulo actual
                if (nextIndex < currentBook.Verse.size) {
                    val nextVerse = currentBook.Verse[nextIndex]
                    val currentChapter = if (currentIndex >= 0) currentBook.Verse[currentIndex].ChapterNum else null
                    val nextChapter = nextVerse.ChapterNum

                    _currentVerseIndex.value = nextIndex

                    // Si cambiamos de capítulo, manejar transición
                    if (currentChapter != null && currentChapter != nextChapter) {
                        handleChapterTransition(nextVerse, nextIndex, currentBook)
                    } else {
                        // Mismo capítulo, leer siguiente versículo
                        readNextVerseInSameChapter(nextVerse, nextIndex)
                    }
                } else {
                    // No hay más versículos, verificar si hay siguiente capítulo
                    handleEndOfChapter(currentBook)
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error en utterance: $utteranceId")
                _isSpeaking.value = false
            }
        })
    }

    private fun handleChapterTransition(nextVerse: Verses, nextIndex: Int, currentBook: Books) {
        val bookName = getBookDisplayName(nextVerse.Book)
        val chapterNum = nextVerse.ChapterNum

        val chapterLabel = getLocalizedString(R.string.tts_chapter_label)
        val chapterIntro = "$bookName, $chapterLabel $chapterNum"

        // Primero anunciar el capítulo
        tts?.speak(chapterIntro, TextToSpeech.QUEUE_FLUSH, null, "chapter_intro_$chapterNum")

        // Preparar el texto del versículo
        val verseText = buildVerseText(nextVerse, isNewChapter = true)
        tts?.speak(verseText, TextToSpeech.QUEUE_ADD, null, nextIndex.toString())
    }

    private fun readNextVerseInSameChapter(nextVerse: Verses, nextIndex: Int) {
        val verseText = buildVerseText(nextVerse, isNewChapter = false)
        tts?.speak(verseText, TextToSpeech.QUEUE_FLUSH, null, nextIndex.toString())
    }

    private fun buildVerseText(verse: Verses, isNewChapter: Boolean): String {
        val textBuilder = StringBuilder()

        // Verificar si hay título de capítulo y si está habilitado
        if (_announceChapterTitles.value && !verse.ChapterName.isNullOrEmpty()) {
            textBuilder.append("${verse.ChapterName}. ")
        }

        if (_announceVerseNumbers.value) {
            if (isNewChapter) {
                // Para nuevo capítulo, incluir información completa
                val bookName = getBookDisplayName(verse.Book)
                val bookLabel = getLocalizedString(R.string.tts_book_label)
                val chapterLabel = getLocalizedString(R.string.tts_chapter_label)
                val verseLabel = getLocalizedString(R.string.tts_verse_label)

                textBuilder.append("$bookLabel $bookName, $chapterLabel ${verse.ChapterNum}, $verseLabel ${verse.Verse}. ")
            } else {
                // Mismo capítulo, solo número de versículo
                val verseLabel = getLocalizedString(R.string.tts_verse_label)
                textBuilder.append("$verseLabel ${verse.Verse}. ")
            }
        }

        textBuilder.append(verse.Contenido ?: "")
        return textBuilder.toString()
    }

    private fun handleEndOfChapter(currentBook: Books) {
        val nextChapter = capitulo.value + 1
        if (nextChapter <= currentBook.Chapters) {
            // Pasar al siguiente capítulo
            setCapitulo(nextChapter)
            _currentVerseIndex.value = 0

            // Buscar el primer versículo del nuevo capítulo
            val firstVerseOfNewChapter = currentBook.Verse.find { it.ChapterNum == nextChapter }
            if (firstVerseOfNewChapter != null) {
                val bookName = getBookDisplayName(firstVerseOfNewChapter.Book)
                val chapterLabel = getLocalizedString(R.string.tts_chapter_label)
                val chapterIntro = "$bookName, $chapterLabel $nextChapter"

                tts?.speak(chapterIntro, TextToSpeech.QUEUE_FLUSH, null, "chapter_intro_$nextChapter")

                val verseText = buildVerseText(firstVerseOfNewChapter, isNewChapter = true)
                val newVerseIndex = currentBook.Verse.indexOf(firstVerseOfNewChapter)
                tts?.speak(verseText, TextToSpeech.QUEUE_ADD, null, newVerseIndex.toString())

                _currentVerseIndex.value = newVerseIndex
            }
        } else {
            // No hay más capítulos, detener lectura
            _isSpeaking.value = false
            Log.d("TTS", "Lectura completada - fin del libro")
            Toast.makeText(context, getLocalizedString(R.string.reading_completed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTTSLanguage(newLanguage: AppLanguage) {
        if (_isTTSInitialized.value) {
            // Pausar lectura actual si está en progreso
            val wasPlaying = _isSpeaking.value
            if (wasPlaying) {
                pauseReading()
            }

            // Cambiar idioma
            setTTSLanguage(newLanguage)

            // Resumir lectura si estaba en progreso
            if (wasPlaying && !_isPaused.value) {
                resumeReading()
            }
        }
    }

    private fun setTTSLanguage(language: AppLanguage) {
        tts?.let { textToSpeech ->
            val locale = when (language) {
                AppLanguage.SPANISH -> Locale("es", "ES")
                AppLanguage.ENGLISH -> Locale("en", "US")
                AppLanguage.FRENCH -> Locale("fr", "FR")
                AppLanguage.PORTUGUESE -> Locale("pt", "PT")
                AppLanguage.ROMANIAN -> Locale("ro", "RO")
                // Si tienes más idiomas en tu enum, agrégalos aquí
            }

            val result = textToSpeech.setLanguage(locale)
            val languageDisplayName = language.getDisplayName(context)

            when (result) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                    Log.w("TTS", "Idioma $languageDisplayName no soportado, usando inglés")
                    val msg = getLocalizedString(R.string.tts_lang_not_supported, languageDisplayName)
                    Toast.makeText(
                        context,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                    // Fallback a inglés (tu idioma por defecto)
                    textToSpeech.setLanguage(Locale("en", "US"))
                }
                TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    Log.i("TTS", "Idioma cambiado exitosamente a: $languageDisplayName")
                    val msg = getLocalizedString(R.string.tts_lang_changed, languageDisplayName)
                    Toast.makeText(
                        context,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Manejar cualquier otro resultado inesperado
                    Log.w("TTS", "Resultado inesperado al cambiar idioma: $result")
                    val msg = getLocalizedString(R.string.tts_lang_error_unexpected)
                    Toast.makeText(
                        context,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Función pública para cambiar idioma manualmente (si es necesario)
    fun changeTTSLanguage(language: AppLanguage) {
        _currentTTSLanguage.value = language
        updateTTSLanguage(language)
    }

    fun speakText(text: String, utteranceId: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        _isSpeaking.value = true
    }

    fun stopReading() {
        tts?.stop()
        _isSpeaking.value = false
        _isPaused.value = false
    }

    fun startReadingService(title: String, verseText: String) {
        val intent = Intent(context, BibleReadingService::class.java).apply {
            putExtra("title", title)
            putExtra("content", verseText)
        }
        context.startForegroundService(intent)
    }

    fun stopReadingService() {
        val intent = Intent(context, BibleReadingService::class.java)
        context.stopService(intent)
    }

    fun pauseReading() {
//        tts?.stop()
//        _pausedText.value = _getAllBiblia.value.first().Verse[_currentVerseIndex.value].Contenido!!
//        _isSpeaking.value = false
//        _isPaused.value = true
        if (_isSpeaking.value) {
            tts?.stop()

            val currentBook = _getAllBiblia.value.firstOrNull()
            val currentIndex = _currentVerseIndex.value

            if (currentBook != null && currentIndex < currentBook.Verse.size) {
                val currentVerse = currentBook.Verse[currentIndex]
                _pausedText.value = currentVerse.Contenido ?: ""
            }

            _isSpeaking.value = false
            _isPaused.value = true
            Log.d("TTS", "Lectura pausada en versículo ${currentIndex + 1}")
        }
    }

    // Función para iniciar la lectura desde el versículo actual
    fun startReading() {
        if (!_isTTSInitialized.value) {
            Log.w("TTS", "TTS no está inicializado")
            Toast.makeText(context, getLocalizedString(R.string.tts_not_initialized), Toast.LENGTH_SHORT).show()
            return
        }

        if (_isSpeaking.value) {
            Log.d("TTS", "Ya se está reproduciendo")
            return
        }

        speakNextVerse()
    }

    fun resumeReading() {
//        if (_isPaused.value) {
//            tts?.speak(_pausedText.value, TextToSpeech.QUEUE_FLUSH, null, _currentVerseIndex.value.toString())
//            _isPaused.value = false
//        } else {
//            speakNextVerse()
//        }
//        _isSpeaking.value = true

        if (_isPaused.value && !_isSpeaking.value) {
            _isPaused.value = false
            speakNextVerse() // Usar la función principal para resumir
            Log.d("TTS", "Lectura reanudada desde versículo ${_currentVerseIndex.value + 1}")
        }
    }

    fun speakNextVerse() {
//        val currentVerse = _getAllBiblia.value.first().Verse[_currentVerseIndex.value]
//        val text = "Libro: ${currentVerse.Book}, Capítulo: ${currentVerse.ChapterNum}, Versículo: ${currentVerse.Verse}. ${currentVerse.Contenido}"
//        speakText(text, _currentVerseIndex.value.toString())

        if (_getAllBiblia.value.isEmpty() || _getAllBiblia.value.first().Verse.isEmpty()) {
            Log.e("TTS", "No hay datos de biblia disponibles")
            return
        }
        val currentBook = _getAllBiblia.value.first()
        val verseIndex = _currentVerseIndex.value

        // Verificar que el índice sea válido
        if (verseIndex < 0 || verseIndex >= currentBook.Verse.size) {
            Log.e("TTS", "Índice de versículo inválido: $verseIndex")
            return
        }

        val currentVerse = currentBook.Verse[verseIndex]

        // Construir el texto de anuncio más claro
        val bookName = getBookDisplayName(currentVerse.Book)
        val chapterNum = currentVerse.ChapterNum ?: 1
        val verseNum = currentVerse.Verse ?: (verseIndex + 1)
        val verseContent = currentVerse.Contenido ?: ""

        // Construir el texto basado en configuraciones y usando strings localizados
        val textBuilder = StringBuilder()

        // Verificar si hay título de capítulo y si está habilitado
        if (_announceChapterTitles.value && !currentVerse.ChapterName.isNullOrEmpty()) {
            textBuilder.append("${currentVerse.ChapterName}. ")
        }

        if (_announceVerseNumbers.value) {
            // Usar strings localizados
            val bookLabel = getLocalizedString(R.string.tts_book_label) // "Libro"
            val chapterLabel = getLocalizedString(R.string.tts_chapter_label) // "Capítulo"
            val verseLabel = getLocalizedString(R.string.tts_verse_label) // "Versículo"

            textBuilder.append("$bookLabel $bookName, $chapterLabel $chapterNum, $verseLabel $verseNum. ")
        }

        textBuilder.append(verseContent)

        val finalText = textBuilder.toString()
        Log.d("TTS", "Leyendo: $finalText")
        speakText(finalText, verseIndex.toString())
    }

    // Métodos para cambiar capítulos, aumentar/decrementar capítulo, etc.
    fun changeChapter(newChapter: Int) {
        _currentVerseIndex.value = newChapter
        speakNextVerse()
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
            coroutineScope.launch {
                listState.animateScrollToItem(index)
                //delay(300)
                //onScrollEnd() // Llamar al final para evitar bucles
            }
        } else {
            Log.e("BibleViewModel", "No se encontró el capítulo $chapterNum")
        }
    }

    fun plusBook(){
        viewModelScope.launch {
            if (_LibroIndex.value < _getAllBook.value.size - 1){
                _LibroIndex.value += 1
                _Libro.value = _getAllBook.value[_LibroIndex.value].Osis

                try {
                    _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
                }
                catch (e: Exception){
                    Log.e("Error PlusBook", e.message.toString())
                }

                _Capitulo.value = 1
                shared.saveCurrentBook(_Libro.value)
                shared.saveCapitulo(1)
            }


        }

    }
    fun getBook(osis: String){
        viewModelScope.launch {
            val previousBook = _Libro.value

            _Libro.value = _getAllBook.value.first { v -> v.Osis == osis }.Osis
            _LibroIndex.value = _getAllBook.value.indexOfFirst { v -> v.Osis == osis }

            try{
                _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
            }catch (e: Exception){
                Log.e("Error LessBook", e.message.toString())
            }
            _Capitulo.value = 1
            shared.saveCurrentBook(_Libro.value)
            shared.saveCapitulo(1)

            // ✅ NUEVO: Verificar si el libro anterior fue completado
            checkBookCompletion(previousBook)
        }
    }
    fun lessBook(){
        viewModelScope.launch {
            if (_LibroIndex.value >= 1){
                _LibroIndex.value -= 1
                _Libro.value = _getAllBook.value[_LibroIndex.value].Osis

                try{
                    _getAllBiblia.value = booksUC.GetAllBookAndVerses(_Libro.value)
                }catch (e: Exception){
                    Log.e("Error LessBook", e.message.toString())
                }

                _Capitulo.value = 1
                shared.saveCurrentBook(_Libro.value)
                shared.saveCapitulo(1)
            }


        }
    }

    fun aumentarCapitulo(){
        if (_Capitulo.value <= _getAllBiblia.value.first().Chapters - 1){
            _Capitulo.value += 1
            shared.saveCapitulo(_Capitulo.value) // Guardar capítulo actualizado
        }

    }

    fun decrementCapitulo(){
        if(_Capitulo.value >= 2){
            _Capitulo.value -= 1
            shared.saveCapitulo(_Capitulo.value)
        }

    }

    fun setLibro(value: String){
        _Libro.value = value
        shared.saveCurrentBook(value)
    }

    fun setCapitulo(value: Int){
        val previousChapter = _Capitulo.value
        _Capitulo.value = value
        shared.saveCapitulo(value)

        // ✅ NUEVO: Si el capítulo aumentó, marcar el anterior como leído
        if (value > previousChapter) {
            trackChapterCompletion(_Libro.value, previousChapter)
        }
    }

    // NUEVO: función para trackear finalización de capítulo
    private fun trackChapterCompletion(bookOsis: String, chapter: Int) {
        // Marcar capítulo como leído
        shared.markChapterAsRead(bookOsis, chapter)

        // Estimar tiempo de lectura (ejemplo: 3-5 minutos por capítulo)
        shared.addReadingTime(4) // 4 minutos promedio

        // Incrementar versículos leídos (estimar ~25 versículos por capítulo)
        val currentBook = _getAllBiblia.value.firstOrNull()
        currentBook?.let { book ->
            val versesInChapter = book.Verse.count { it.ChapterNum == chapter }
            shared.incrementVersesRead(versesInChapter)
        }

        Log.d("BibleStats", "Capítulo completado: $bookOsis capítulo $chapter")
    }

    // NUEVO: verificar si un libro fue completado
    private fun checkBookCompletion(bookOsis: String) {
        val book = _getAllBook.value.find { it.Osis == bookOsis }
        book?.let {
            val chaptersReadInBook = shared.getChaptersReadForBook(bookOsis)
            if (chaptersReadInBook >= it.Chapters) {
                shared.markBookAsRead(bookOsis)
                Log.d("BibleStats", "¡Libro completado!: ${it.Human}")

                // Mostrar celebración (opcional)
                Toast.makeText(context, context.getString(R.string.book_completed_toast, it.Human), Toast.LENGTH_LONG).show()
            }
        }
    }

}