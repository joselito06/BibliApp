package com.jbncode.bibliaapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "header")
data class HeaderEntity(

    @PrimaryKey  val number : Int,
    val titulo : String?,
    @ColumnInfo(name = "verseid") val verseId : Int?,
    @ColumnInfo(name = "bookid")  val bookId : String?,
    @ColumnInfo(name = "chapterid") val chapteId : Int?

)
