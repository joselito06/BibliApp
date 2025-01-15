package com.example.bibliaapp.data.database.repositories

import com.example.bibliaapp.data.database.daos.VersesDao
import com.example.bibliaapp.data.database.entities.BooksEntity
import com.example.bibliaapp.data.database.entities.VersesEntity
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.Verses
import com.example.bibliaapp.domain.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VerseRepository @Inject constructor(val verseDao: VersesDao) {
    suspend fun GetAll() : List<Verses> {
        return withContext(Dispatchers.IO){
            val response: List<VersesEntity> = verseDao.GetAll()
            response.map { it.toDomain() }
        }
    }

    suspend fun GetVerseByLibro(libro: String) : List<Verses>{
        return withContext(Dispatchers.IO){
            val response: List<VersesEntity> = verseDao.GetHeaderByLibro(libro)
            response.map { it.toDomain() }
        }
    }
}