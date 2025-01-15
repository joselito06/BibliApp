package com.example.bibliaapp.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.example.bibliaapp.data.database.entities.BooksEntity

@Dao
interface BooksDao {
    @Query("SELECT * FROM books")
    fun GetAll(): List<BooksEntity>

    @Query("SELECT * FROM books WHERE osis = :libro")
    fun GetBookByLibro(libro:String): List<BooksEntity>

    /*@Query("SELECT * FROM books WHERE libro = :libro AND capitulo = :capitulo")
    fun GetEscrituraByLibroAndCapitulo(libro:Int, capitulo:Int): List<BibliaEntity>

    @Query("SELECT * FROM books WHERE libro = :libro AND capitulo = :capitulo AND versiculo = :versiculo")
    fun GetEscrituraByLibAndCapAndVersc(libro:Int, capitulo:Int, versiculo:Int): List<BibliaEntity> */
}