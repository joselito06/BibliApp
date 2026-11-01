package com.jbncode.bibliaapp.utils

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

object AdGate {
    private const val PREFS = "ad_gate"
    private const val KEY_LAST_BG = "last_background_ms"
    private const val KEY_SKIP_APP_OPEN = "skip_next_app_open"

    // Tiempo de gracia tras cerrar cualquier anuncio a pantalla completa
    private const val GRACE_AFTER_AD_MS = 5_000L

    @Volatile var isFullScreenAdShowing = false
    @Volatile var lastFullScreenAdDismissedAt = 0L

    // ---- Valores ajustables desde Firebase Remote Config ----
    private fun rc() = FirebaseRemoteConfig.getInstance()
    fun appOpenMinMinutes(): Long = rc().getLong("app_open_min_minutes").let { if (it <= 0) 10 else it }
    fun interstitialMinIntervalSec(): Long = rc().getLong("interstitial_min_interval_seconds").let { if (it <= 0) 180 else it }
    fun interstitialEveryN(): Long = rc().getLong("interstitial_every_n").let { if (it <= 0) 2 else it }

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Llamar en MainActivity.onStop() */
    fun markBackground(ctx: Context) {
        if (isFullScreenAdShowing) return // un anuncio tapó la app, no cuenta como "salir"
        prefs(ctx).edit().putLong(KEY_LAST_BG, System.currentTimeMillis()).apply()
    }

    /** Llamar antes de reiniciar la app a propósito (cambio de idioma / Biblia) */
    fun skipNextAppOpen(ctx: Context) {

        prefs(ctx).edit().putBoolean(KEY_SKIP_APP_OPEN, true).commit() // commit: el proceso va a morir
    }

    /** Llamar en onResume. Consume el estado, solo evalúa una vez por salida. */
    fun shouldShowAppOpen(ctx: Context): Boolean {
        val p = prefs(ctx)
        val skip = p.getBoolean(KEY_SKIP_APP_OPEN, false)
        val lastBg = p.getLong(KEY_LAST_BG, 0L)
        p.edit().remove(KEY_SKIP_APP_OPEN).remove(KEY_LAST_BG).apply()

        if (skip) return false
        if (isFullScreenAdShowing) return false
        val now = System.currentTimeMillis()
        if (now - lastFullScreenAdDismissedAt < GRACE_AFTER_AD_MS) return false
        if (lastBg == 0L) return false
        return now - lastBg >= appOpenMinMinutes() * 60_000L
    }
}