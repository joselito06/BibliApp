package com.jbncode.bibliaapp.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "metadata", indices = [Index(value = ["name"], unique = true)])
data class MetadataEntity(
    @PrimaryKey
    val id: Int,
    @NotNull
    val name: String,
    val value: String
)