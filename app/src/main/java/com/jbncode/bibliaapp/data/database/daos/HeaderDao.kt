package com.jbncode.bibliaapp.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jbncode.bibliaapp.data.database.entities.HeaderEntity

@Dao
interface HeaderDao {
    @Query("SELECT * FROM header")
    fun GetAll(): List<HeaderEntity>

    @Query("SELECT * FROM header WHERE bookid = :libro")
    fun GetHeaderByLibro(libro:String): List<HeaderEntity>
}