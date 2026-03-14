package com.jbncode.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "footverseindice")
data class FooterVerseIndiceEntity(
    @PrimaryKey(autoGenerate = true)
    @NotNull
    val number: Int,
    val pos: Int,
    val indice: String,
    val book: String,
    val chapter: Int,
    val verse: Int
)
