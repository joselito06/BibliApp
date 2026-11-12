package com.jbncode.bibliaapp.core.tts

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Velocidad y voz elegida (una por idioma). */
@Singleton
class TtsSettings @Inject constructor(@ApplicationContext context: Context) {
    private val p = context.getSharedPreferences("tts_settings", Context.MODE_PRIVATE)

    private val _rate = MutableStateFlow(p.getFloat("rate", 1f))
    val rate: StateFlow<Float> = _rate.asStateFlow()

    fun setRate(v: Float) { p.edit { putFloat("rate", v) }; _rate.value = v }

    fun voiceName(lang: String): String? = p.getString("voice_$lang", null)
    fun setVoice(lang: String, name: String?) = p.edit {
        if (name == null) remove("voice_$lang") else putString("voice_$lang", name)
    }
}
