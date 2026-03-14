package com.jbncode.bibliaapp.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jbncode.bibliaapp.data.database.entities.VersesEntity

@Dao
interface VersesDao {
    @Query("SELECT * FROM verses")
    fun GetAll(): List<VersesEntity>

    @Query("SELECT * FROM verses WHERE book = :libro")
    fun GetHeaderByLibro(libro:String): List<VersesEntity>
}