package com.example.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "books")
data class BooksEntity(
    @PrimaryKey(autoGenerate = false)
    val number : Int,
    @NotNull
    val osis : String,
    @NotNull
    val human : String,
    @NotNull
    val chapters: Int
)
