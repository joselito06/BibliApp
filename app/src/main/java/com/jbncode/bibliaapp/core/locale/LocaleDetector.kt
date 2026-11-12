package com.jbncode.bibliaapp.core.locale

import android.content.Context
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import com.jbncode.bibliaapp.domain.model.AppLanguage
import java.util.Locale

object LocaleDetector {
    data class Detected(val language: AppLanguage, val country: String)

    /**
     * Lee el idioma del SISTEMA (no el de la app, que ya pudo ser forzado).
     * Idiomas no soportados caen a inglés.
     */
    fun detect(@Suppress("UNUSED_PARAMETER") context: Context): Detected {
        val locale: Locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
            ?: Locale.getDefault()
        return Detected(AppLanguage.fromCode(locale.language), locale.country.uppercase())
    }
}
