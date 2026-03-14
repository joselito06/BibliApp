package com.jbncode.bibliaapp.di

import android.content.Context
import com.jbncode.bibliaapp.DatabaseDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Instalar en el componente Singleton para una única instancia
object DownloaderModule {

    @Provides
    @Singleton // Opcional: si quieres que sea un Singleton
    fun provideDatabaseDownloader(
        @ApplicationContext context: Context
    ): DatabaseDownloader {
        return DatabaseDownloader(context)
    }
}