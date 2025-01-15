package com.example.bibliaapp.di

import android.content.Context
import androidx.room.Room
import com.example.bibliaapp.data.database.ApplicationDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    private const val DATABASE_NAME = "db_bible.sqlite3"

    @Singleton
    @Provides
    fun providerRoom(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ApplicationDbContext::class.java, DATABASE_NAME)
            //.createFromAsset(DATABASE_NAME)
            .createFromAsset("tla.sqlite3")
            //.addMigrations(migration1to2)
            .fallbackToDestructiveMigration()
            .build()
    @Singleton
    @Provides
    fun providerBookDao(db:ApplicationDbContext) = db.GetBookDao()

    @Singleton
    @Provides
    fun providerHeaderDao(db:ApplicationDbContext) = db.GetHeaderDao()

    @Singleton
    @Provides
    fun providerVerseDao(db:ApplicationDbContext) = db.GetVerseDao()

}