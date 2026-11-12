package com.jbncode.bibliaapp.data.bible

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abre cada Biblia directamente desde su archivo, sin copiarla a "biblia_activa"
 * y sin reiniciar la app. Cambiar de versión = abrir otro archivo.
 */
@Singleton
class BibleDbFactory @Inject constructor(@ApplicationContext private val context: Context) {

    companion object { const val DEFAULT_FILE = "tla.sqlite3" }

    private val cache = ConcurrentHashMap<String, BibleDatabase>()
    private val mutex = Mutex()

    fun exists(fileName: String) = context.getDatabasePath(fileName).exists()

    suspend fun open(requested: String): Pair<String, BibleDatabase> = mutex.withLock {
        // Si la versión pedida no existe, se cae a la de assets en vez de crashear.
        val file = if (requested == DEFAULT_FILE || !exists(requested)) DEFAULT_FILE else requested
        if (file == DEFAULT_FILE) ensureDefaultCopied()
        file to cache.getOrPut(file) {
            Room.databaseBuilder(context, BibleDatabase::class.java, file)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
        }
    }

    /** Llamar antes de borrar o reemplazar un archivo de Biblia. */
    fun close(fileName: String) { cache.remove(fileName)?.close() }

    private suspend fun ensureDefaultCopied() = withContext(Dispatchers.IO) {
        val target = context.getDatabasePath(DEFAULT_FILE)
        if (target.exists() && target.length() > 0) return@withContext
        target.parentFile?.mkdirs()
        context.assets.open(DEFAULT_FILE).use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
    }
}
