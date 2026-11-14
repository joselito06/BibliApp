package com.jbncode.bibliaapp.ui.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.core.tts.TtsController
import com.jbncode.bibliaapp.core.tts.TtsSettings
import com.jbncode.bibliaapp.core.tts.TtsState
import com.jbncode.bibliaapp.core.tts.TtsStatus
import com.jbncode.bibliaapp.core.tts.VoiceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Fachada de voz para la UI (lector, ajustes y barra inferior). */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val tts: TtsController,
    private val settings: TtsSettings,
    private val shared: SharedPreferenceBible
) : ViewModel() {

    val state: StateFlow<TtsState> = tts.state

    val isSpeaking: StateFlow<Boolean> = tts.state
        .map { it.status == TtsStatus.PLAYING || it.status == TtsStatus.LOADING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isPaused: StateFlow<Boolean> = tts.state
        .map { it.status == TtsStatus.PAUSED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** (capítulo, versículo) en lectura, o null. Para resaltar y seguir con el scroll. */
    val speakingRef: StateFlow<Pair<Int, Int>?> = tts.state
        .map { if (it.status == TtsStatus.PLAYING) it.chapter to it.verse else null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** true mientras suena un texto suelto (versículo del día). */
    val isSpeakingText: StateFlow<Boolean> = tts.textPlaying
    fun speakText(text: String) = tts.speakText(text)

    val speechRate: StateFlow<Float> = settings.rate

    private val _announceVerseNumbers = MutableStateFlow(shared.getAnnounceVerseNumbers())
    val announceVerseNumbers: StateFlow<Boolean> = _announceVerseNumbers.asStateFlow()
    private val _announceChapterTitles = MutableStateFlow(shared.getAnnounceChapterTitles())
    val announceChapterTitles: StateFlow<Boolean> = _announceChapterTitles.asStateFlow()

    fun setAnnounceVerseNumbers(on: Boolean) { _announceVerseNumbers.value = on; shared.saveAnnounceVerseNumbers(on) }
    fun setAnnounceChapterTitles(on: Boolean) { _announceChapterTitles.value = on; shared.saveAnnounceChapterTitles(on) }

    fun play(book: String, chapter: Int, fromVerse: Int = 1) = tts.play(book, chapter, fromVerse)
    fun pause() = tts.pause()
    fun resume() = tts.resume()
    fun stop() = tts.stop()
    fun skip(delta: Int) = tts.skipVerse(delta)

    /** Versículo donde reanudar este capítulo (1 si no hay posición guardada aquí). */
    fun resumeVerse(book: String, chapter: Int): Int =
        settings.lastPlayback()?.takeIf { it.book == book && it.chapter == chapter }?.verse ?: 1
    fun setSpeechRate(v: Float) = tts.setRate(v)
    fun selectVoice(name: String?) = tts.setVoice(name)
    suspend fun voiceOptions(): List<VoiceOption> = tts.voiceOptions()
}
