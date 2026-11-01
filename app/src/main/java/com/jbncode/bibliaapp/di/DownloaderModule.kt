package com.jbncode.bibliaapp.di

import android.content.Context
import com.jbncode.bibliaapp.DatabaseDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Instalar en el componente Singleton para una única instancia
object DownloaderModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton // Opcional: si quieres que sea un Singleton
    fun provideDatabaseDownloader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): DatabaseDownloader {
        return DatabaseDownloader(context, okHttpClient)
    }
}