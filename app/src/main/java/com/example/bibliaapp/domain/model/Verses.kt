package com.example.bibliaapp.domain.model

import com.example.bibliaapp.data.database.entities.HeaderEntity
import com.example.bibliaapp.data.database.entities.VersesEntity

data class Verses(
    val Number : Int,
    val Book : String?,
    val Chapter : Int?,
    val Verse : Int?,
    val Contenido : String?,
    val ChapterName : String?,
    val ChapterNum: Int?
)
fun VersesEntity.toDomain() = Verses(number, book, chapter, verse, contenido, "", null)
