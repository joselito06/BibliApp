package com.example.bibliaapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bibliaapp.domain.model.Books
import org.jetbrains.annotations.NotNull

@Entity(tableName = "header")
data class HeaderEntity(

    @PrimaryKey @NotNull val number : Int,
    @NotNull val titulo : String,
    @ColumnInfo(name = "bookid")  val bookId : String?,
    @ColumnInfo(name = "chapterid") val chapteId : Int?,
    @ColumnInfo(name = "verseid") val verseId : Int?
)
