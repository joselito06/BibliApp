package com.jbncode.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BooksEntity(
    @PrimaryKey(autoGenerate = false)
    val number : Int?,
    val osis : String,
    val human : String,
    val chapters: Int
)
