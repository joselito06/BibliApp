package com.example.bibliaapp.view.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.di.RoomModule
import com.example.bibliaapp.domain.model.AppLanguage
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.usescases.BookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun initializeTTS(context: Context, coroutineScope: CoroutineScope, listState: LazyListState) {
        // Detener TTS anterior si existe
        tts?.shutdown()

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTTSInitialized.value = true
                // Configurar el idioma inicial
                val currentLanguage = _currentTTSLanguage.value
                setTTSLanguage(currentLanguage)

                setupTTSProgressListener(coroutineScope, listState)
            } else {
                _isTTSInitialized.value = false
                Toast.makeText(context, "Error al inicializar TextToSpeech", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTTSProgressListener(coroutineScope: CoroutineScope, listState: LazyListState) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _currentVerseIndex.value = utteranceId?.toInt() ?: 0
                coroutineScope.launch {
                    listState.animateScrollToItem(utteranceId?.toInt() ?: 0)
                }
            }

            override fun onDone(utteranceId: String?) {
                val nextIndex = (utteranceId?.toInt() ?: 0) + 1

                if (nextIndex >= _getAllBiblia.value.first().Verse.size) {
                    val nextChapter = capitulo.value + 1
                    if (nextChapter < _getAllBiblia.value.first().Chapters) {
                        val textoIntro = "${_getAllBiblia.value.first().Human}, Capítulo $nextChapter"
                        tts?.speak(textoIntro, TextToSpeech.QUEUE_FLUSH, null, "chapter_intro")
                        tts?.speak(_getAllBiblia.value.first().Verse[0].Contenido, TextToSpeech.QUEUE_ADD, null, "0")
                        setCapitulo(nextChapter)
                    } else {
                        _isSpeaking.value = false
                    }
                } else {
                    tts?.speak(_getAllBiblia.value.first().Verse[nextIndex].Contenido, TextToSpeech.QUEUE_FLUSH, null, nextIndex.toString())
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error en utterance: $utteranceId")
            }
        })
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
                // Si tienes más idiomas en tu enum, agrégalos aquí
            }

            val result = textToSpeech.setLanguage(locale)
            val languageDisplayName = language.getDisplayName(context)

            when (result) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                    Log.w("TTS", "Idioma $languageDisplayName no soportado, usando inglés")
                    Toast.makeText(
                        context,
                        "Idioma $languageDisplayName no disponible para lectura de voz, usando inglés",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Fallback a inglés (tu idioma por defecto)
                    textToSpeech.setLanguage(Locale("en", "US"))
                }
                TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    Log.i("TTS", "Idioma cambiado exitosamente a: $languageDisplayName")
                    Toast.makeText(
                        context,
                        "Idioma de voz cambiado a: $languageDisplayName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Manejar cualquier otro resultado inesperado
                    Log.w("TTS", "Resultado inesperado al cambiar idioma: $result")
                    Toast.makeText(
                        context,
                        "Resultado inesperado al cambiar idioma TTS",
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

    fun pauseReading() {
        tts?.stop()
        _pausedText.value = _getAllBiblia.value.first().Verse[_currentVerseIndex.value].Contenido!!
        _isSpeaking.value = false
        _isPaused.value = true
    }

    fun resumeReading() {
        if (_isPaused.value) {
            tts?.speak(_pausedText.value, TextToSpeech.QUEUE_FLUSH, null, _currentVerseIndex.value.toString())
            _isPaused.value = false
        } else {
            speakNextVerse()
        }
        _isSpeaking.value = true
    }

    fun speakNextVerse() {
        val currentVerse = _getAllBiblia.value.first().Verse[_currentVerseIndex.value]
        val text = "Libro: ${currentVerse.Book}, Capítulo: ${currentVerse.ChapterNum}, Versículo: ${currentVerse.Verse}. ${currentVerse.Contenido}"
        speakText(text, _currentVerseIndex.value.toString())
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
        _Capitulo.value = value
        shared.saveCapitulo(value)
    }


}