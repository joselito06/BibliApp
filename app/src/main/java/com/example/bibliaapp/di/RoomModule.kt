package com.example.bibliaapp.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.data.database.ApplicationDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    private const val DATABASE_NAME = "db_bible.sqlite3"
    const val ACTIVE_DATABASE_FILENAME = "biblia_activa.sqlite3"
    private const val DEFAULT_ASSET_DB_NAME = "tla.sqlite3" // CAMBIA ESTO

    /*@Singleton
    @Provides
    fun providerRoom(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ApplicationDbContext::class.java, DATABASE_NAME)
            //.createFromAsset(DATABASE_NAME)
            .createFromAsset("tla.sqlite3")
            //.addMigrations(migration1to2)
            .fallbackToDestructiveMigration()
            .build()*/

    @Singleton
    @Provides
    fun providerRoom(
        @ApplicationContext context: Context,
        // Inyecta tu SharedPreferenceBible
        sharedPreferences: SharedPreferenceBible
    ): ApplicationDbContext {
        ensureActiveDatabaseIsReady(context, sharedPreferences)

        val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
        if (!activeDbFile.exists()) {
            throw IllegalStateException("La BD activa '$ACTIVE_DATABASE_FILENAME' no pudo ser preparada.")
        }

        return Room.databaseBuilder(
            context,
            ApplicationDbContext::class.java,
            ACTIVE_DATABASE_FILENAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun providerBookDao(db:ApplicationDbContext) = db.GetBookDao()

    @Singleton
    @Provides
    fun providerHeaderDao(db:ApplicationDbContext) = db.GetHeaderDao()

    @Singleton
    @Provides
    fun providerVerseDao(db:ApplicationDbContext) = db.GetVerseDao()

    private fun ensureActiveDatabaseIsReady(
        context: Context,
        // Ahora inyectas tu clase SharedPreferenceBible directamente
        sharedPrefs: SharedPreferenceBible
    ) {
        val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
        // Usa tu método de SharedPreferenceBible
        val selectedUserDbName = sharedPrefs.getSelectedDatabaseFileName()

        // Lógica de copia (similar a la anterior, pero usando sharedPrefs)
        // ...
        // Ejemplo de una parte de la lógica:
        if (selectedUserDbName != null) {
            val userSelectedDbFile = context.getDatabasePath(selectedUserDbName)
            if (userSelectedDbFile.exists()) {
                try {
                    userSelectedDbFile.copyTo(activeDbFile, overwrite = true)
                    Log.d("RoomModule", "BD seleccionada '$selectedUserDbName' copiada a activa.")
                    // No es necesario llamar a sharedPrefs.saveSelectedDatabaseFileName aquí de nuevo
                    // porque ya se guardó cuando el usuario hizo la selección.
                } catch (e: IOException) {
                    Log.e("RoomModule", "Error al copiar BD seleccionada '$selectedUserDbName'", e)
                    // Si falla la copia, y la activa no existe, se intentará la de assets
                    if (!activeDbFile.exists()) {
                        copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
                    }
                }
            } else if (!activeDbFile.exists()) {
                // El archivo seleccionado no existe, pero el activo tampoco, usa assets
                Log.w("RoomModule", "BD seleccionada '$selectedUserDbName' no encontrada. Usando assets.")
                copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
            }
        } else if (!activeDbFile.exists()) {
            // No hay BD seleccionada por el usuario y la activa no existe -> usa assets
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
        }
    }

    private fun copyDatabaseFromAssets(context: Context, assetName: String, destinationName: String) {
        // ... (misma función de copia que antes)
        val dbFile = context.getDatabasePath(destinationName)
        Log.d("RoomModule", "Copiando BD desde assets '$assetName' a '$destinationName'.")
        try {
            if (!dbFile.parentFile.exists()) {
                dbFile.parentFile.mkdirs()
            }
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("RoomModule", "BD copiada desde assets exitosamente.")
        } catch (e: IOException) {
            Log.e("RoomModule", "Fallo al copiar BD desde assets: $assetName", e)
            throw RuntimeException("Fallo al copiar BD por defecto desde assets.", e)
        }
    }

}