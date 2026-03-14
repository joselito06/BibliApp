package com.jbncode.bibliaapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jbncode.bibliaapp.data.database.daos.BooksDao
import com.jbncode.bibliaapp.data.database.daos.HeaderDao
import com.jbncode.bibliaapp.data.database.daos.VersesDao
import com.jbncode.bibliaapp.data.database.entities.BooksEntity
import com.jbncode.bibliaapp.data.database.entities.Converts
import com.jbncode.bibliaapp.data.database.entities.HeaderEntity
import com.jbncode.bibliaapp.data.database.entities.VersesEntity

@Database(entities = [BooksEntity::class, HeaderEntity::class, VersesEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converts::class)
abstract class ApplicationDbContext : RoomDatabase(){
    abstract fun GetBookDao(): BooksDao
    abstract fun GetHeaderDao(): HeaderDao
    abstract fun GetVerseDao(): VersesDao
}