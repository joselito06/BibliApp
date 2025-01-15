package com.example.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "verses")
data class VersesEntity(
    @PrimaryKey
    @NotNull
    val number : Int,
    val book : String?,
    val chapter : Int?,
    val verse : Int?,
    val contenido : String?
)
