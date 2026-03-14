package com.jbncode.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "footverse")
data class FooterVerseEntity(
    @PrimaryKey(autoGenerate = true)
    @NotNull
    val number: Int,
    val bookId: String,
    val chapterId: Int,
    val verseId: Int,
    val contenido: String
)
