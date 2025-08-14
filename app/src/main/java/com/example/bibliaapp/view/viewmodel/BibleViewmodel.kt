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
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.usescases.BookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BibleViewmodel @Inject constructor(private val booksUC: BookUseCase, private val shared: SharedPreferenceBible) : ViewModel() {
    val validChapterItem = MutableStateFlow(true)
    val numChapterItem = MutableStateFlow(0)
    val sharedComp = MutableStateFlow(shared)

    private var tts: TextToSpeech? = null
    private val _currentVerseIndex = MutableStateFlow(0)
    private val _isSpeaking = MutableStateFlow(false)
    private val _isPaused = MutableStateFlow(false)
    private val _pausedText = MutableStateFlow("")

    val currentVerseIndex: StateFlow<Int> get() = _currentVerseIndex
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
        loadSavedState() // Cargar el último libro y capítulo al iniciar la app
    }
    private fun loadSavedState() {
        viewModelScope.launch {
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

    fun initializeTTS(context: Context, coroutineScope: CoroutineScope, listState: LazyListState) {
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