package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.DatabaseDownloader
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.di.RoomModule
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class BibleManagerViewmodel @Inject constructor(
    val sharedPreferenceBible: SharedPreferenceBible,
    private val BibleUseCase: BibleUseCase,
    @ApplicationContext private val context: Context,
    private val databaseDownloader: DatabaseDownloader // Inyectado o instanciado
) : ViewModel() {
    val DEFAULT_ASSET_DB_NAME = "tla.sqlite3"

    // Callback para la navegación
    var onNavigateToRestartScreen: (() -> Unit)? = null

    // Lista original sin filtrar, cargada desde JSON
    private val _originalBibleList = MutableStateFlow<List<BibleVersionInfo>>(emptyList())

    // Estado para el idioma seleccionado en el filtro. "Todos" podría ser una opción.
    private val _selectedLanguageFilter = MutableStateFlow<String?>("Todos") // null o "Todos" para mostrar todos
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    // Lista de idiomas disponibles para el filtro (derivada de la lista original)
    private val _availableLanguages = MutableStateFlow<List<String>>(listOf("Todos"))
    val availableLanguages: StateFlow<List<String>> = _availableLanguages.asStateFlow()

    // La lista de Biblias que se mostrará en la UI (filtrada)
    // Se actualiza cuando _originalBibleList o _selectedLanguageFilter cambian.
    val filteredBibleList: StateFlow<List<BibleVersionInfo>> =
        _originalBibleList.combine(_selectedLanguageFilter) { list, lang ->
            if (lang == null || lang == "Todos") {
                list
            } else {
                list.filter { it.lang == lang } // O it.langVisualCode si prefieres filtrar por eso
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())


    private val _downloadStatus = MutableLiveData<String>() // Para mensajes de estado simples
    val downloadStatus: LiveData<String> = _downloadStatus

    init {
        loadBibleList() // Cargar la lista al iniciar el ViewModel
    }

    // Cargar la lista de Biblias (desde assets/listaBiblia.json o desde una API)
    fun loadBibleList() {
        viewModelScope.launch {
            val parsedList: List<BibleVersionInfo> = BibleUseCase.invoke()

            // Verificar cuáles ya están descargadas
            val updatedList = parsedList.map { bible ->
                val dbFile = context.getDatabasePath(bible.database_name) // Usa database_name como nombre local
                bible.copy(isDownloaded = dbFile.exists())
            }
            _originalBibleList.value = updatedList
            //_bibleList.postValue(updatedList)

            // Actualizar la lista de idiomas disponibles para el filtro
            val languages = mutableSetOf("Todos") // "Todos" como primera opción
            parsedList.forEach { languages.add(it.lang) } // o it.langVisualCode
            _availableLanguages.value = languages.toList().sorted() // Ordenar alfabéticamente
        }
    }

    // Función para actualizar el filtro de idioma
    fun setLanguageFilter(language: String?) {
        _selectedLanguageFilter.value = language
    }

    fun startDownload(bibleToDownload: BibleVersionInfo) {
        if (bibleToDownload.isDownloading) {
            _downloadStatus.postValue("Descarga ya en progreso para ${bibleToDownload.title}")
            return
        }

        // Marcar como descargando y reiniciar progreso para la UI
        updateBibleItemState(bibleToDownload.code) { it.copy(isDownloading = true, downloadProgress = 0, isDownloaded = false) }

        val downloadUrl = "${bibleToDownload.server_url}${bibleToDownload.database_name}"
        // El nombre del archivo local será el mismo que en el servidor (database_name)
        val destinationFileName = bibleToDownload.database_name

        _downloadStatus.postValue("Iniciando descarga de ${bibleToDownload.title}...")

        viewModelScope.launch(Dispatchers.IO) { // Usar Dispatchers.IO para operaciones de red/disco
            databaseDownloader.downloadDatabase(
                downloadUrl,
                destinationFileName,
                object : DatabaseDownloader.DownloadProgressListener {
                    override fun onProgress(progressPercentage: Int) {
                        Log.d("VM_Downloader", "Progreso ${bibleToDownload.title}: $progressPercentage%")
                        updateBibleItemState(bibleToDownload.code) { it.copy(downloadProgress = progressPercentage) }
                    }

                    override fun onCompleted(file: File) {
                        Log.i("VM_Downloader", "Descarga completa: ${file.name}")
                        _downloadStatus.postValue("${bibleToDownload.title} descargada exitosamente.")
                        updateBibleItemState(bibleToDownload.code) { it.copy(isDownloading = false, isDownloaded = true, downloadProgress = 100) }
                        // Opcional: ¿Activarla automáticamente o dejar que el usuario lo haga?
                        // Si quieres activarla: activateDownloadedDatabase(file.name) // y manejar reinicio
                    }

                    override fun onError(exception: Exception) {
                        Log.e("VM_Downloader", "Error descargando ${bibleToDownload.title}", exception)
                        _downloadStatus.postValue("Error al descargar ${bibleToDownload.title}: ${exception.message}")
                        updateBibleItemState(bibleToDownload.code) { it.copy(isDownloading = false, downloadProgress = 0) }
                        // Asegurarse de que el archivo parcial se elimine (ya lo hace el downloader)
                    }
                }
            )
        }
    }

    private fun updateBibleItemState(bibleCode: String, updateLogic: (BibleVersionInfo) -> BibleVersionInfo) {
        val currentList = _originalBibleList.value
        val updatedList = currentList.map {
            if (it.code == bibleCode) {
                updateLogic(it)
            } else {
                it
            }
        }
        _originalBibleList.value = updatedList
    }


    // La función activateDownloadedDatabase (del ejemplo anterior) iría aquí también
    fun activateDownloadedDatabase(downloadedDbFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeDbPath = context.getDatabasePath(RoomModule.ACTIVE_DATABASE_FILENAME)
            val downloadedDbPath = context.getDatabasePath(downloadedDbFileName)

            if (!downloadedDbPath.exists()) {
                _downloadStatus.postValue("Error: BD '$downloadedDbFileName' no encontrada para activar.")
                return@launch
            }
            try {
                downloadedDbPath.copyTo(activeDbPath, overwrite = true)
                sharedPreferenceBible.saveSelectedDatabaseFileName(downloadedDbFileName)

                Log.i("BibleManagerVM", "BD descargada '$downloadedDbFileName' activada exitosamente.")
                withContext(Dispatchers.Main) { // Navegar desde el hilo principal
                    onNavigateToRestartScreen?.invoke()
                }
                //_downloadStatus.postValue("${downloadedDbFileName} activada. Reinicia la aplicación.")
                // Aquí deberías manejar la notificación para el reinicio de la app.
            } catch (e: IOException) {
                _downloadStatus.postValue("Error al activar $downloadedDbFileName: ${e.message}")
            }
        }
    }

    /**
     * Activa la base de datos por defecto (la que se incluye en los assets de la app).
     * Esto implica copiar la BD desde assets al archivo de BD activa y limpiar
     * la preferencia de la BD seleccionada por el usuario.
     */
    fun activateDefaultDatabase() {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de archivo, usar IO dispatcher
            val activeDbFile = context.getDatabasePath(RoomModule.ACTIVE_DATABASE_FILENAME)

            _downloadStatus.postValue("Activando Biblia por defecto...")
            Log.i("BibleManagerVM", "Activando BD por defecto desde assets ('$DEFAULT_ASSET_DB_NAME') hacia '${activeDbFile.name}'.")

            try {
                // Paso 1: Copiar la base de datos desde la carpeta assets a la ubicación activa.
                // Esta lógica es similar a la de RoomModule, pero aquí la ejecutamos "bajo demanda".
                context.assets.open(DEFAULT_ASSET_DB_NAME).use { inputStream ->
                    FileOutputStream(activeDbFile).use { outputStream -> // Sobrescribe activeDbFile
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.i("BibleManagerVM", "BD desde assets ('$DEFAULT_ASSET_DB_NAME') copiada a activa ('${activeDbFile.name}') exitosamente.")

                // Paso 2: Limpiar la preferencia de la base de datos seleccionada.
                // Esto asegura que en el próximo inicio, RoomModule usará la de assets
                // si no hay una copia explícita de una BD de usuario.
                sharedPreferenceBible.saveSelectedDatabaseFileName(null)
                Log.i("BibleManagerVM", "Preferencia de BD seleccionada por usuario limpiada (ahora es null).")

                // (Opcional) Actualizar estado visual de las Biblias en la lista
                // Si la BD de assets 'tla.sqlite3' tiene un 'code' correspondiente en tu lista,
                // podrías querer marcar otras como 'no activas' visualmente.
                // La lógica de 'isActive' en BibleDownloadScreen ya debería manejar esto
                // cuando `selectedDbName` se vuelve null.
                Log.i("BibleManagerVM", "BD por defecto activada. Navegando a pantalla de reinicio.")
                withContext(Dispatchers.Main) { // Navegar desde el hilo principal
                    onNavigateToRestartScreen?.invoke()
                }

                _downloadStatus.postValue("Biblia por defecto activada. Reinicia la aplicación para ver los cambios.")
                Log.w("BibleManagerVM", "¡REINICIO DE APP REQUERIDO para que Room use la nueva BD por defecto activa!")
                // De nuevo, crucial informar al usuario sobre el reinicio.

            } catch (e: IOException) {
                _downloadStatus.postValue("Error al activar la Biblia por defecto: ${e.message}")
                Log.e("BibleManagerVM", "Fallo al copiar BD desde assets '$DEFAULT_ASSET_DB_NAME' o al limpiar preferencias.", e)
                // Considera qué hacer si esto falla. ¿Dejar la BD activa anterior?
                // ¿Intentar de nuevo? Por ahora, solo logueamos y mostramos error.
            }
        }
    }

    // Función auxiliar para obtener el 'code' de un nombre de archivo de BD
    // Asume que el nombre del archivo es "code.sqlite3" (ej: "tla.sqlite3" -> "tla")
    // Necesitarás ajustar esto si tu convención de nombres es diferente.
    private fun codeFromDbName(dbName: String): String {
        return dbName.removeSuffix(".sqlite3")
    }
}