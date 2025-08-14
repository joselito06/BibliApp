package com.example.bibliaapp.domain.model

import com.example.bibliaapp.R


enum class AppLanguage(val code: String, val displayNameResId: Int) {
    ENGLISH("en", R.string.language_english), // Necesitarás añadir R.string.language_english, etc.
    SPANISH("es", R.string.language_spanish);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return AppLanguage.entries.find { it.code == code } ?: ENGLISH // Default a Inglés si no se encuentra o es nulo
        }
    }
}