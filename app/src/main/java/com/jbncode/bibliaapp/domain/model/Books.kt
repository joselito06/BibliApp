package com.jbncode.bibliaapp.domain.model

import com.jbncode.bibliaapp.data.database.entities.BooksEntity


data class Books(
    val Number : Int?,
    val Osis : String,
    val Human : String,
    val Chapters: Int,
    val Verse: List<Verses>
)
fun BooksEntity.toDomain() = Books(number, osis, human, chapters, mutableListOf<Verses>())
