package com.example.bibliaapp.data.database.repositories

import com.example.bibliaapp.data.database.daos.BooksDao
import com.example.bibliaapp.data.database.daos.HeaderDao
import com.example.bibliaapp.data.database.entities.BooksEntity
import com.example.bibliaapp.data.database.entities.HeaderEntity
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.Header
import com.example.bibliaapp.domain.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HeaderRepository @Inject constructor(val headerDao: HeaderDao){

    suspend fun GetAll() : List<Header> {
        return withContext(Dispatchers.IO){
            val response: List<HeaderEntity> = headerDao.GetAll()
            response.map { it.toDomain() }
        }
    }

    suspend fun GetHeaderByLibro(libro: String) : List<Header>{
        return withContext(Dispatchers.IO){
            val response: List<HeaderEntity> = headerDao.GetHeaderByLibro(libro)
            response.map { it.toDomain() }
        }
    }
}


