package com.example.bibliaapp.domain.model

import com.example.bibliaapp.data.database.entities.HeaderEntity

data class Header(
    val Number : Int,
    val Titulo : String,
    val BookId : String?,
    val ChapteId : Int?,
    val VerseId : Int?
)
fun HeaderEntity.toDomain() = Header(number, titulo, bookId, chapteId, verseId)