package com.jbncode.bibliaapp.core.tts

enum class TtsStatus { IDLE, LOADING, PLAYING, PAUSED }

data class TtsState(
    val status: TtsStatus = TtsStatus.IDLE,
    val book: String? = null,
    val bookName: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val languageTag: String = ""
)

data class VoiceOption(val name: String, val tag: String, val online: Boolean, val selected: Boolean)
