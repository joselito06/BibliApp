package com.jbncode.bibliaapp.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.database.ApplicationDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    const val ACTIVE_DATABASE_FILENAME = "biblia_activa.sqlite3"
    const val DEFAULT_ASSET_DB_NAME = "tla.sqlite3" // CAMBIA ESTO
    const val TEMP_DATABASE_FILENAME = "temp_biblia.sqlite3"
    private val databaseMutex = Mutex()
    private const val TAG = "RoomModule"
    private const val MAX_RETRY_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 1000L

    @Singleton
    @Provides
    fun providerRoom(
        @ApplicationContext context: Context,
        // Inyecta tu SharedPreferenceBible
        sharedPreferences: SharedPreferenceBible
    ): ApplicationDbContext {
        return runBlocking {
            databaseMutex.withLock {
                //ensureActiveDatabaseIsReady(context, sharedPreferences)
                prepareCleanDatabase(context, sharedPreferences)

                val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
                if (!activeDbFile.exists()) {
                    throw IllegalStateException("La BD activa '$ACTIVE_DATABASE_FILENAME' no pudo ser preparada.")
                }

                Room.databaseBuilder(
                    context,
                    ApplicationDbContext::class.java,
                    ACTIVE_DATABASE_FILENAME
                )
                    .fallbackToDestructiveMigration()
                    // Configurar WAL mode manualmente para mayor control
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
            }
        }

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

    private suspend fun prepareCleanDatabase(
        context: Context,
        sharedPrefs: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
        val selectedUserDbName = sharedPrefs.getSelectedDatabaseFileName()

        Log.d(TAG, "Preparando base de datos activa. Seleccionada: $selectedUserDbName")

        try {
            // CRÍTICO: Limpiar completamente archivos WAL existentes
            cleanupDatabaseFiles(context, ACTIVE_DATABASE_FILENAME)

            when {
                selectedUserDbName != null -> {
                    handleUserSelectedDatabaseWithCleanup(context, selectedUserDbName, activeDbFile, sharedPrefs)
                }
                !activeDbFile.exists() -> {
                    Log.i(TAG, "No hay BD seleccionada y la activa no existe. Usando assets.")
                    copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPrefs)
                }
                else -> {
                    Log.d(TAG, "BD activa ya existe y no hay nueva selección.")
                    // Verificar integridad incluso si existe
                    if (!isDatabaseValidWithRetry(activeDbFile)) {
                        Log.w(TAG, "BD activa existente está corrupta, recreando desde assets.")
                        cleanupDatabaseFiles(context, ACTIVE_DATABASE_FILENAME)
                        copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPrefs)
                    }
                }
            }

            // Verificación final robusta
            verifyDatabaseIntegrityWithRetry(context, activeDbFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico preparando la base de datos", e)
            handleDatabaseError(context, activeDbFile, e, sharedPrefs)
        }
    }

    private suspend fun handleUserSelectedDatabaseWithCleanup(
        context: Context,
        selectedUserDbName: String,
        activeDbFile: File,
        sharedPreferences: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        val userSelectedDbFile = context.getDatabasePath(selectedUserDbName)

        if (!userSelectedDbFile.exists()) {
            Log.w(TAG, "BD seleccionada '$selectedUserDbName' no encontrada. Usando assets.")
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPreferences)
            return@withContext
        }

        // CRÍTICO: Limpiar archivos WAL de la BD origen también
        //cleanupDatabaseFiles(context, selectedUserDbName)
        // CRÍTICO: Solo limpiar archivos WAL, NO el archivo principal
        cleanupWalFilesOnly(context, selectedUserDbName)

        // Verificar integridad con reintentos
        if (!isDatabaseValidWithRetry(userSelectedDbFile)) {
            Log.e(TAG, "BD seleccionada '$selectedUserDbName' está corrupta después de cleanup. Usando assets.")
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPreferences)
            return@withContext
        }

        performSecureDatabaseCopy(context, userSelectedDbFile, activeDbFile, selectedUserDbName, sharedPreferences)
    }

    private suspend fun performSecureDatabaseCopy(
        context: Context,
        sourceFile: File,
        targetFile: File,
        sourceName: String,
        sharedPreferences: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        val tempDbFile = context.getDatabasePath(TEMP_DATABASE_FILENAME)

        try {
            Log.d(TAG, "Iniciando copia segura de '$sourceName'")

            // Asegurar que no hay archivos temporales previos
            cleanupDatabaseFiles(context, TEMP_DATABASE_FILENAME)

            // Crear copia temporal usando método seguro
            performAtomicCopy(sourceFile, tempDbFile)

            // Verificar integridad del archivo temporal
            if (!isDatabaseValidWithRetry(tempDbFile)) {
                throw IOException("El archivo temporal falló la verificación de integridad")
            }

            // Limpiar archivos de destino antes de mover
            cleanupDatabaseFiles(context, ACTIVE_DATABASE_FILENAME)

            // Mover archivo temporal al destino final
            if (!tempDbFile.renameTo(targetFile)) {
                throw IOException("Fallo al renombrar archivo temporal a destino final")
            }

            Log.d(TAG, "BD seleccionada '$sourceName' copiada exitosamente con cleanup completo.")

        } catch (e: Exception) {
            Log.e(TAG, "Error en copia segura de BD '$sourceName'", e)

            // Limpiar archivo temporal en caso de error
            cleanupDatabaseFiles(context, TEMP_DATABASE_FILENAME)

            // Fallback a assets
            if (!targetFile.exists()) {
                copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPreferences)
            }else{}
        }
    }

    private fun performAtomicCopy(source: File, destination: File) {
        Log.d(TAG, "Realizando copia atómica: ${source.name} -> ${destination.name}")

        // Usar método más robusto para copia
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(8192) // Buffer de 8KB
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }

                output.flush()
                output.fd.sync() // Forzar escritura inmediata al disco
            }
        }

        Log.d(TAG, "Copia atómica completada. Tamaño: ${destination.length()} bytes")
    }

    private fun cleanupDatabaseFiles(context: Context, databaseName: String) {
        val dbFile = context.getDatabasePath(databaseName)
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val journalFile = File(dbFile.path + "-journal")

        Log.d(TAG, "Limpiando archivos de BD para: $databaseName")

        listOf(walFile, shmFile, journalFile).forEach { file ->
            try {
                if (file.exists() && file.delete()) {
                    Log.d(TAG, "Eliminado archivo: ${file.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error eliminando archivo ${file.name}", e)
            }
        }

        // Eliminar archivo principal al final
        try {
            if (dbFile.exists() && dbFile.delete()) {
                Log.d(TAG, "Eliminado archivo principal: ${dbFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando archivo principal ${dbFile.name}", e)
        }
    }

    private fun cleanupWalFilesOnly(context: Context, databaseName: String) {
        val dbFile = context.getDatabasePath(databaseName)
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val journalFile = File(dbFile.path + "-journal")

        Log.d(TAG, "Limpiando solo archivos WAL para: $databaseName")

        listOf(walFile, shmFile, journalFile).forEach { file ->
            try {
                if (file.exists() && file.delete()) {
                    Log.d(TAG, "Eliminado archivo WAL: ${file.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error eliminando archivo WAL ${file.name}", e)
            }
        }
        // NO eliminar el archivo principal aquí
    }

    private suspend fun copyDatabaseFromAssets(
        context: Context,
        assetName: String,
        destinationName: String,
        sharedPreferences: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(destinationName)
        Log.d(TAG, "Copiando BD desde assets '$assetName' a '$destinationName'.")

        try {
            // Asegurar directorio padre
            dbFile.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
            }

            // Limpiar cualquier archivo existente
            cleanupDatabaseFiles(context, destinationName)

            // Copiar desde assets con buffer optimizado
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.flush()
                    outputStream.fd.sync()
                }
            }
            // Guardar el nombre del archivo seleccionado
            sharedPreferences.saveSelectedDatabaseFileName(DEFAULT_ASSET_DB_NAME)

            Log.d(TAG, "BD copiada desde assets exitosamente. Tamaño: ${dbFile.length()} bytes")

        } catch (e: IOException) {
            Log.e(TAG, "Fallo al copiar BD desde assets: $assetName", e)
            throw RuntimeException("Fallo al copiar BD por defecto desde assets.", e)
        }
    }

    private suspend fun verifyDatabaseIntegrityWithRetry(
        context: Context,
        dbFile: File,
        maxRetries: Int = MAX_RETRY_ATTEMPTS
    ) = withContext(Dispatchers.IO) {
        repeat(maxRetries) { attempt ->
            try {
                if (isDatabaseValidWithRetry(dbFile)) {
                    Log.d(TAG, "Verificación de integridad exitosa en intento ${attempt + 1}")
                    return@withContext
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error en verificación de integridad, intento ${attempt + 1}", e)
            }

            if (attempt < maxRetries - 1) {
                Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
            }
        }

        throw IllegalStateException("La base de datos falló la verificación de integridad después de $maxRetries intentos.")
    }

    private fun isDatabaseValidWithRetry(dbFile: File, maxRetries: Int = 2): Boolean {
        repeat(maxRetries) { attempt ->
            if (isDatabaseValid(dbFile)) {
                return true
            }
            if (attempt < maxRetries - 1) {
                Thread.sleep(500)
            }
        }
        return false
    }

    private suspend fun handleDatabaseError(
        context: Context,
        activeDbFile: File,
        originalError: Exception,
        sharedPreferences: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        Log.e(TAG, "Manejando error crítico de base de datos", originalError)

        try {
            // Limpieza completa incluyendo archivos WAL
            cleanupDatabaseFiles(context, ACTIVE_DATABASE_FILENAME)

            // Recuperar desde assets
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME, sharedPreferences)

        } catch (recoveryError: Exception) {
            Log.e(TAG, "Fallo en recuperación de emergencia", recoveryError)
            throw RuntimeException("No se pudo recuperar la base de datos", recoveryError)
        }
    }

    fun isDatabaseValid(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() == 0L) {
            Log.w(TAG, "Archivo de BD no existe o está vacío: ${dbFile.path}")
            return false
        }

        var database: SQLiteDatabase? = null

        return try {
            database = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )

            // Verificación robusta con múltiples consultas
            database.rawQuery("PRAGMA integrity_check(1)", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    if (result != "ok") {
                        Log.e(TAG, "PRAGMA integrity_check falló: $result para ${dbFile.path}")
                        return false
                    }
                }
            }

            // Verificar que tiene contenido
            database.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table'", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val tableCount = cursor.getInt(0)
                    Log.d(TAG, "BD válida con $tableCount tablas: ${dbFile.path}")
                    return tableCount > 0
                } else {
                    Log.e(TAG, "No se pudieron contar las tablas en: ${dbFile.path}")
                    return false
                }
            }

        } catch (e: SQLiteException) {
            Log.e(TAG, "BD inválida o corrupta: ${dbFile.path}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando BD: ${dbFile.path}", e)
            false
        } finally {
            try {
                database?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error cerrando BD durante verificación", e)
            }
        }
    }

    // Función de utilidad para limpiar archivos temporales
    fun cleanupTempFiles(context: Context) {
        try {
            cleanupDatabaseFiles(context, TEMP_DATABASE_FILENAME)
            Log.d(TAG, "Archivos temporales limpiados.")
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando archivos temporales", e)
        }
    }

    // Función para forzar cierre de todas las conexiones
    fun forceCloseAllConnections(context: Context) {
        try {
            // Sugerir garbage collection agresivo
            System.gc()
            System.runFinalization()
            Thread.sleep(200)
            Log.d(TAG, "Forzado cierre de conexiones.")
        } catch (e: Exception) {
            Log.w(TAG, "Error forzando cierre de conexiones", e)
        }
    }

    //*********************************************************************************************
    //*********************************************************************************************
    //*********************************************************************************************

    /*private suspend fun ensureActiveDatabaseIsReady(
        context: Context,
        sharedPrefs: SharedPreferenceBible
    ) = withContext(Dispatchers.IO) {
        val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
        val selectedUserDbName = sharedPrefs.getSelectedDatabaseFileName()

        Log.d(TAG, "Preparando base de datos activa. Seleccionada: $selectedUserDbName")

        try {
            // Cerrar cualquier conexión existente antes de manipular archivos
            closeExistingConnections(activeDbFile)

            when {
                selectedUserDbName != null -> {
                    handleUserSelectedDatabase(context, selectedUserDbName, activeDbFile)
                }
                !activeDbFile.exists() -> {
                    Log.i(TAG, "No hay BD seleccionada y la activa no existe. Usando assets.")
                    copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
                }
                else -> {
                    Log.d(TAG, "BD activa ya existe y no hay nueva selección.")
                }
            }

            // Verificación final con reintentos
            verifyDatabaseIntegrityWithRetry(context, activeDbFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico preparando la base de datos", e)
            handleDatabaseError(context, activeDbFile, e)
        }
    }

    private suspend fun handleUserSelectedDatabase(
        context: Context,
        selectedUserDbName: String,
        activeDbFile: File
    ) = withContext(Dispatchers.IO) {
        val userSelectedDbFile = context.getDatabasePath(selectedUserDbName)

        if (!userSelectedDbFile.exists()) {
            Log.w(TAG, "BD seleccionada '$selectedUserDbName' no encontrada. Usando assets.")
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
            return@withContext
        }

        // Verificar integridad de la BD origen antes de copiar
        if (!isDatabaseValid(userSelectedDbFile)) {
            Log.e(TAG, "BD seleccionada '$selectedUserDbName' está corrupta. Usando assets.")
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
            return@withContext
        }

        // Usar archivo temporal para copia segura
        val tempDbFile = context.getDatabasePath(TEMP_DATABASE_FILENAME)

        try {
            // Copiar a archivo temporal primero
            Log.d(TAG, "Copiando BD seleccionada '$selectedUserDbName' a archivo temporal.")
            userSelectedDbFile.copyTo(tempDbFile, overwrite = true)

            // Verificar integridad del archivo temporal
            if (!isDatabaseValid(tempDbFile)) {
                throw IOException("El archivo temporal falló la verificación de integridad")
            }

            // Solo si todo está bien, mover el archivo temporal al activo
            if (activeDbFile.exists()) {
                activeDbFile.delete()
            }

            tempDbFile.renameTo(activeDbFile)
            Log.d(TAG, "BD seleccionada '$selectedUserDbName' copiada exitosamente.")

        } catch (e: IOException) {
            Log.e(TAG, "Error al copiar BD seleccionada '$selectedUserDbName'", e)

            // Limpiar archivo temporal si existe
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }

            // Fallback a assets si no existe la activa
            if (!activeDbFile.exists()) {
                copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)
            }
        }
    }

    private suspend fun copyDatabaseFromAssets(
        context: Context,
        assetName: String,
        destinationName: String
    ) = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(destinationName)
        Log.d(TAG, "Copiando BD desde assets '$assetName' a '$destinationName'.")

        try {
            // Asegurar que el directorio padre existe
            dbFile.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
            }

            // Eliminar archivo existente si está corrupto
            if (dbFile.exists() && !isDatabaseValid(dbFile)) {
                Log.w(TAG, "Eliminando BD existente corrupta antes de copiar desde assets.")
                dbFile.delete()
            }

            // Copiar desde assets
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                    outputStream.getFD().sync() // Forzar escritura a disco
                }
            }

            Log.d(TAG, "BD copiada desde assets exitosamente.")

        } catch (e: IOException) {
            Log.e(TAG, "Fallo al copiar BD desde assets: $assetName", e)
            throw RuntimeException("Fallo al copiar BD por defecto desde assets.", e)
        }
    }

    private fun closeExistingConnections(dbFile: File) {
        try {
            // Intentar cerrar cualquier conexión SQLite existente
            if (dbFile.exists()) {
                System.gc() // Sugerir recolección de basura
                Thread.sleep(100) // Dar tiempo para que se liberen recursos
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al intentar cerrar conexiones existentes", e)
        }
    }

    private suspend fun verifyDatabaseIntegrityWithRetry(
        context: Context,
        dbFile: File,
        maxRetries: Int = 3
    ) = withContext(Dispatchers.IO) {
        repeat(maxRetries) { attempt ->
            try {
                if (isDatabaseValid(dbFile)) {
                    Log.d(TAG, "Verificación de integridad exitosa en intento ${attempt + 1}")
                    return@withContext
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error en verificación de integridad, intento ${attempt + 1}", e)
            }

            if (attempt < maxRetries - 1) {
                Thread.sleep((500 * (attempt + 1)).toLong()) // Backoff exponencial
            }
        }

        throw IllegalStateException("La base de datos falló la verificación de integridad después de $maxRetries intentos.")
    }

    private suspend fun handleDatabaseError(
        context: Context,
        activeDbFile: File,
        originalError: Exception
    ) = withContext(Dispatchers.IO) {
        Log.e(TAG, "Manejando error crítico de base de datos", originalError)

        try {
            // Eliminar archivo corrupto
            if (activeDbFile.exists()) {
                activeDbFile.delete()
            }

            // Intentar recuperar desde assets
            copyDatabaseFromAssets(context, DEFAULT_ASSET_DB_NAME, ACTIVE_DATABASE_FILENAME)

        } catch (recoveryError: Exception) {
            Log.e(TAG, "Fallo en recuperación de emergencia", recoveryError)
            throw RuntimeException("No se pudo recuperar la base de datos", recoveryError)
        }
    }

    fun isDatabaseValid(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() == 0L) {
            Log.w(TAG, "Archivo de BD no existe o está vacío: ${dbFile.path}")
            return false
        }

        return try {
            SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                // Verificación adicional: intentar una consulta simple
                db.rawQuery("SELECT COUNT(*) FROM sqlite_master", null).use { cursor ->
                    cursor.moveToFirst()
                    val tableCount = cursor.getInt(0)
                    Log.d(TAG, "BD válida con $tableCount tablas: ${dbFile.path}")
                    tableCount > 0
                }
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "BD inválida o corrupta: ${dbFile.path}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando BD: ${dbFile.path}", e)
            false
        }
    }

    // Función de utilidad para limpiar archivos temporales
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDbFile = context.getDatabasePath(TEMP_DATABASE_FILENAME)
            if (tempDbFile.exists()) {
                tempDbFile.delete()
                Log.d(TAG, "Archivo temporal limpiado.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando archivos temporales", e)
        }
    }*/

    //*********************************************************************************************
    //*********************************************************************************************

    /*private fun ensureActiveDatabaseIsReady(
        context: Context,
        // Ahora inyectas tu clase SharedPreferenceBible directamente
        sharedPrefs: SharedPreferenceBible
    ) {
        val activeDbFile = context.getDatabasePath(ACTIVE_DATABASE_FILENAME)
        // Usa tu método de SharedPreferenceBible
        val selectedUserDbName = sharedPrefs.getSelectedDatabaseFileName()
        Log.d(TAG, "Preparando base de datos activa. Seleccionada: $selectedUserDbName")
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

        // Verifica al final
        if (!isDatabaseValid(activeDbFile)) {
            throw IllegalStateException("La base de datos no es válida después de copiarla.")
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

    // Verifica integridad
    fun isDatabaseValid(dbFile: File): Boolean {
        return try {
            SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { true }
        } catch (e: Exception) {
            false
        }
    }*/

}