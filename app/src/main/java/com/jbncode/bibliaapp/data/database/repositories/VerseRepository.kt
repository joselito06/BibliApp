package com.jbncode.bibliaapp.data.database.repositories

import com.jbncode.bibliaapp.data.database.daos.VersesDao
import com.jbncode.bibliaapp.data.database.entities.VersesEntity
import com.jbncode.bibliaapp.domain.model.Verses
import com.jbncode.bibliaapp.domain.model.toDomain
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