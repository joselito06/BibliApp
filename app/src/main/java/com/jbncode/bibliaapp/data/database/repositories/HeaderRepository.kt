package com.jbncode.bibliaapp.data.database.repositories

import com.jbncode.bibliaapp.data.database.daos.HeaderDao
import com.jbncode.bibliaapp.data.database.entities.HeaderEntity
import com.jbncode.bibliaapp.domain.model.Header
import com.jbncode.bibliaapp.domain.model.toDomain
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


