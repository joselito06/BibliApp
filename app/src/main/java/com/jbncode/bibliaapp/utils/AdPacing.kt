package com.jbncode.bibliaapp.utils

/**
 * Ritmo de los anuncios dentro del lector: uno al final de cada tercer capítulo distinto
 * que se abre en la sesión. Nunca interrumpe la lectura ni se repite al volver al mismo capítulo.
 */
object AdPacing {
    private const val EVERY = 3
    private val decided = HashMap<String, Boolean>()
    private var opened = 0

    fun shouldShowInline(chapterKey: String): Boolean = decided.getOrPut(chapterKey) {
        opened++
        opened % EVERY == 0
    }
}
