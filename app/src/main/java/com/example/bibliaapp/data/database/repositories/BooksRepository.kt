package com.example.bibliaapp.data.database.repositories

import com.example.bibliaapp.data.database.daos.BooksDao
import com.example.bibliaapp.data.database.entities.BooksEntity
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BooksRepository @Inject constructor(val booksDao: BooksDao) {

    suspend fun GetAll() : List<Books> {
        return withContext(Dispatchers.IO){
            val response: List<BooksEntity> = booksDao.GetAll()
            response.map { it.toDomain() }
        }
    }

    suspend fun GetBookByLibro(libro: String) : List<Books>{
        return withContext(Dispatchers.IO){
            val response: List<BooksEntity> = booksDao.GetBookByLibro(libro)
            response.map { it.toDomain() }
        }
    }
}