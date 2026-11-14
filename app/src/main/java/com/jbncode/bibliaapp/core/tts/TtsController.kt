package com.jbncode.bibliaapp.core.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.domain.bible.Verse
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import com.jbncode.bibliaapp.service.BibleReadingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único dueño del TextToSpeech. Vive en el proceso (singleton), así la lectura sigue
 * con la pantalla apagada: la UI y el servicio solo le dan órdenes y observan [state].
 * El idioma de la VOZ sale del idioma de la Biblia activa, no del de la app.
 */
@Singleton
class TtsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bible: BibleRepository,
    private val catalog: BibleUseCase,
    private val prefs: SharedPreferenceBible,
    private val settings: TtsSettings
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var engineReady: CompletableDeferred<Boolean>? = null
    private var playJob: Job? = null

    private var generation = 0                 // invalida callbacks de lecturas anteriores
    private var verses: List<Verse> = emptyList()
    private var index = 0
    private var book = ""
    private var bookName = ""
    private var chapter = 0
    private var locale: Locale = Locale.getDefault()

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _textPlaying = MutableStateFlow(false)
    /** true mientras se lee un texto suelto (p. ej. el versículo del día). */
    val textPlaying: StateFlow<Boolean> = _textPlaying.asStateFlow()

    // ── API pública ──────────────────────────────────────────────────────────

    /** Lee un texto corto (versículo del día) con la voz y el idioma de la Biblia activa. */
    fun speakText(text: String) {
        if (text.isBlank()) return
        playJob?.cancel()
        playJob = scope.launch {
            if (_state.value.status != TtsStatus.IDLE) stop()   // corta cualquier lectura de capítulo
            val e = engine() ?: return@launch fail()
            locale = resolveLocale()
            if (!applyLocale(e)) { notifyMissingVoice(); return@launch }
            if (!requestFocus()) return@launch
            generation++; e.stop()
            e.setSpeechRate(settings.rate.value)
            _textPlaying.value = true
            e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$generation:text")
        }
    }

    fun play(book: String, chapter: Int, fromVerse: Int = 1) {
        playJob?.cancel()
        playJob = scope.launch {
            _state.update { it.copy(status = TtsStatus.LOADING, book = book, chapter = chapter, verse = 0) }
            val e = engine() ?: return@launch fail()
            locale = resolveLocale()
            if (!applyLocale(e)) { notifyMissingVoice(); return@launch stop() }
            if (!requestFocus()) return@launch stop()
            loadChapter(book, chapter, fromVerse)
            if (verses.isEmpty()) return@launch stop()
            registerNoisy()
            ContextCompat.startForegroundService(context, Intent(context, BibleReadingService::class.java))
            speakFrom(index, intro = fromVerse <= 1)
        }
    }

    fun pause() {
        if (_state.value.status != TtsStatus.PLAYING) return
        generation++; tts?.stop()
        _state.update { it.copy(status = TtsStatus.PAUSED) }
    }

    fun resume() {
        if (_state.value.status != TtsStatus.PAUSED) return
        scope.launch { if (requestFocus()) speakFrom(index, intro = false) }
    }

    fun stop() {
        playJob?.cancel()
        _textPlaying.value = false
        generation++; tts?.stop()
        abandonFocus(); unregisterNoisy()
        _state.value = TtsState()
        context.stopService(Intent(context, BibleReadingService::class.java))
    }

    fun skipVerse(delta: Int) {
        if (_state.value.status == TtsStatus.IDLE || verses.isEmpty()) return
        val target = (index + delta).coerceIn(0, verses.lastIndex)
        scope.launch { if (requestFocus()) speakFrom(target, intro = false) }
    }

    fun setRate(v: Float) {
        settings.setRate(v.coerceIn(0.5f, 2f))
        if (_state.value.status == TtsStatus.PLAYING) speakFrom(index, intro = false)
    }

    fun setVoice(name: String?) {
        settings.setVoice(locale.language, name)
        if (_state.value.status == TtsStatus.PLAYING) scope.launch {
            engine()?.let { applyLocale(it) }
            speakFrom(index, intro = false)
        }
    }

    suspend fun voiceOptions(): List<VoiceOption> {
        val e = engine() ?: return emptyList()
        val loc = resolveLocale()
        val saved = settings.voiceName(loc.language)
        return installedVoices(e, loc.language).sortedWith(voiceOrder(loc))
            .map { VoiceOption(it.name, it.locale.toLanguageTag(), it.isNetworkConnectionRequired, it.name == saved) }
    }

    // ── Motor ────────────────────────────────────────────────────────────────

    private suspend fun engine(): TextToSpeech? {
        val d = engineReady ?: CompletableDeferred<Boolean>().also { deferred ->
            engineReady = deferred
            tts = TextToSpeech(context) { status -> deferred.complete(status == TextToSpeech.SUCCESS) }
            tts?.setOnUtteranceProgressListener(listener)
        }
        return if (d.await()) tts else { tts?.shutdown(); tts = null; engineReady = null; null }
    }

    private suspend fun resolveLocale(): Locale {
        val file = bible.activeFile.first()
        val code = runCatching { catalog() }.getOrDefault(emptyList())
            .firstOrNull { it.database_name == file }?.langVisualCode ?: "es"
        val device = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
        return if (device != null && device.language == code) device else Locale(code)
    }

    private fun applyLocale(e: TextToSpeech): Boolean {
        val r = e.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        pickVoice(e)?.let { e.voice = it }
        e.setSpeechRate(settings.rate.value)
        e.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        )
        _state.update { it.copy(languageTag = locale.toLanguageTag()) }
        return true
    }

    private fun installedVoices(e: TextToSpeech, lang: String): List<Voice> = try {
        e.voices.orEmpty().filter {
            it.locale.language == lang && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
    } catch (ex: Exception) { emptyList() }

    private fun voiceOrder(loc: Locale) = compareByDescending<Voice> { it.locale.country == loc.country }
        .thenBy { it.isNetworkConnectionRequired }
        .thenByDescending { it.quality }
        .thenBy { it.name }

    private fun pickVoice(e: TextToSpeech): Voice? {
        val all = installedVoices(e, locale.language)
        val saved = settings.voiceName(locale.language)
        return all.firstOrNull { it.name == saved } ?: all.sortedWith(voiceOrder(locale)).firstOrNull()
    }

    private fun notifyMissingVoice() {
        val lang = locale.getDisplayLanguage(locale)
        Toast.makeText(context, context.getString(R.string.tts_voice_missing, lang), Toast.LENGTH_LONG).show()
        try {
            context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    private fun fail() {
        Toast.makeText(context, context.getString(R.string.tts_error_initialization), Toast.LENGTH_SHORT).show()
        stop()
    }

    // ── Lectura ──────────────────────────────────────────────────────────────

    private suspend fun loadChapter(b: String, c: Int, fromVerse: Int) {
        bookName = bible.books().first().firstOrNull { it.osis == b }?.name ?: b
        verses = bible.chapter(b, c)
        book = b; chapter = c
        index = verses.indexOfFirst { it.verse >= fromVerse }.coerceAtLeast(0)
    }

    /** Encola todo el capítulo: el motor lo reproduce seguido, sin huecos ni depender de la UI. */
    private fun speakFrom(start: Int, intro: Boolean) {
        val e = tts ?: return
        if (verses.isEmpty()) return
        generation++
        e.stop()
        e.setSpeechRate(settings.rate.value)
        val g = generation
        val labels = labelContext()
        if (intro) {
            e.speak("$bookName, ${labels.getString(R.string.tts_chapter_label)} $chapter", TextToSpeech.QUEUE_ADD, null, "$g:intro")
        }
        for (i in start until verses.size) {
            e.speak(utterance(verses[i], labels), TextToSpeech.QUEUE_ADD, null, "$g:$i")
        }
        index = start
        _state.update {
            it.copy(status = TtsStatus.PLAYING, book = book, bookName = bookName, chapter = chapter, verse = verses[start].verse)
        }
    }

    // Las etiquetas ("Versículo", "Capítulo") se leen en el idioma de la Biblia, no en el de la app.
    private fun labelContext(): Context =
        context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })

    private fun utterance(v: Verse, labels: Context): String = buildString {
        if (prefs.getAnnounceChapterTitles() && !v.heading.isNullOrBlank()) append(v.heading).append(". ")
        if (prefs.getAnnounceVerseNumbers()) append(labels.getString(R.string.tts_verse_label)).append(' ').append(v.verse).append(". ")
        append(v.text)
    }

    private suspend fun advanceChapter() {
        val books = bible.books().first()
        val pos = books.indexOfFirst { it.osis == book }
        val current = books.getOrNull(pos) ?: return stop()
        if (chapter < current.chapters) loadChapter(book, chapter + 1, 1)
        else loadChapter((books.getOrNull(pos + 1) ?: return stop()).osis, 1, 1)
        if (verses.isEmpty()) return stop()
        speakFrom(0, intro = true)
    }

    private fun split(id: String?): Pair<Int, String>? {
        val parts = id?.split(":") ?: return null
        if (parts.size != 2) return null
        return (parts[0].toIntOrNull() ?: return null) to parts[1]
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val (g, tail) = split(utteranceId) ?: return
            scope.launch {
                if (g != generation) return@launch
                val i = tail.toIntOrNull() ?: return@launch
                index = i
                verses.getOrNull(i)?.let { v ->
                    _state.update { it.copy(verse = v.verse) }
                    settings.saveLastPlayback(book, chapter, v.verse)   // para reanudar luego
                }
            }
        }
        override fun onDone(utteranceId: String?) {
            val (g, tail) = split(utteranceId) ?: return
            scope.launch {
                if (tail == "text" && g == generation) { _textPlaying.value = false; abandonFocus(); return@launch }
                if (g == generation && tail.toIntOrNull() == verses.lastIndex) advanceChapter()
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val g = split(utteranceId)?.first
            scope.launch { if (g == null || g == generation) { _textPlaying.value = false; stop() } }
        }
    }

    // ── Foco de audio y auriculares ──────────────────────────────────────────

    private val audio = context.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.status == TtsStatus.PLAYING) { resumeOnGain = true; pause() }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnGain) { resumeOnGain = false; resume() }
        }
    }

    private fun requestFocus(): Boolean {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            .setOnAudioFocusChangeListener(focusListener).build()
        focusRequest = req
        return audio.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null; resumeOnGain = false
    }

    private var noisyRegistered = false
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { pause() }   // se desconectaron los auriculares
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        ContextCompat.registerReceiver(
            context, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        try { context.unregisterReceiver(noisy) } catch (_: Exception) { }
        noisyRegistered = false
    }
}
