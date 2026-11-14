package com.jbncode.bibliaapp.core.locale

import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.domain.model.AppLanguage

/** Al elegir una Biblia, el idioma de la app pasa a ser el de esa Biblia (si la app lo soporta). */
object BibleLanguage {
    fun appLanguageFor(langVisualCode: String?): AppLanguage? {
        val code = langVisualCode?.trim()?.lowercase()?.take(2) ?: return null
        return AppLanguage.entries.firstOrNull { it.code == code }
    }

    /** @return el idioma aplicado, o null si no hubo cambio. */
    fun sync(prefs: SharedPreferenceBible, langVisualCode: String?): AppLanguage? {
        val target = appLanguageFor(langVisualCode) ?: return null
        if (prefs.getAppLanguageCode() == target.code) return null
        prefs.saveAppLanguageCode(target.code)
        return target
    }
}
