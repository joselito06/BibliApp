package com.example.bibliaapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bibliaapp.data.database.daos.BooksDao
import com.example.bibliaapp.data.database.daos.HeaderDao
import com.example.bibliaapp.data.database.daos.VersesDao
import com.example.bibliaapp.data.database.entities.BooksEntity
import com.example.bibliaapp.data.database.entities.Converts
import com.example.bibliaapp.data.database.entities.HeaderEntity
import com.example.bibliaapp.data.database.entities.VersesEntity

@Database(entities = [BooksEntity::class, HeaderEntity::class, VersesEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converts::class)
abstract class ApplicationDbContext : RoomDatabase(){
    abstract fun GetBookDao(): BooksDao
    abstract fun GetHeaderDao(): HeaderDao
    abstract fun GetVerseDao(): VersesDao
}