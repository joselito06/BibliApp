package com.jbncode.bibliaapp.domain.bible

/** Modelos limpios de la capa de dominio (reemplazan gradualmente a Books/Verses/Header). */
data class Book(val osis: String, val name: String, val chapters: Int, val order: Int)

data class Verse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val heading: String? = null
)

/** Referencia independiente de la versión: sirve para notas, favoritos y enlaces. */
data class BibleRef(val book: String, val chapter: Int, val verse: Int? = null)

data class SearchHit(val ref: BibleRef, val snippet: String)
