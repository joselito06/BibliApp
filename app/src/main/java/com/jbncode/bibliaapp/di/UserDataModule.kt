package com.jbncode.bibliaapp.di

import android.content.Context
import androidx.room.Room
import com.jbncode.bibliaapp.data.user.ReadingLogDao
import com.jbncode.bibliaapp.data.user.UserDatabase
import com.jbncode.bibliaapp.data.user.UserItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserDataModule {
    @Provides @Singleton
    fun userDb(@ApplicationContext c: Context): UserDatabase =
        Room.databaseBuilder(c, UserDatabase::class.java, "user_data.db").build()

    @Provides fun userItemDao(db: UserDatabase): UserItemDao = db.userItemDao()
    @Provides fun readingLogDao(db: UserDatabase): ReadingLogDao = db.readingLogDao()
}
