package com.jbncode.bibliaapp.domain.model

import com.jbncode.bibliaapp.R


enum class AppLanguage(val code: String, val displayNameResId: Int) {
    ENGLISH("en", R.string.language_english), // Necesitarás añadir R.string.language_english, etc.
    SPANISH("es", R.string.language_spanish),
    FRENCH("fr", R.string.language_french),
    PORTUGUESE("pt", R.string.language_portuguese),
    ROMANIAN("ro", R.string.language_romanian);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return AppLanguage.entries.find { it.code == code } ?: ENGLISH // Default a Inglés si no se encuentra o es nulo
        }

        fun getAllLanguages(): List<AppLanguage> {
            return entries.toList()
        }

    }

    // Método helper para obtener el nombre del idioma (necesitarás contexto para esto)
    fun getDisplayName(context: android.content.Context): String {
        return context.getString(displayNameResId)
    }
}