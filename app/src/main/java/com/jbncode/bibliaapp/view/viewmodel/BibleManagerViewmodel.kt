package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.DatabaseDownloader
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.core.locale.LocaleDetector
import com.jbncode.bibliaapp.data.bible.BibleDbFactory
import com.jbncode.bibliaapp.data.bible.DbValidator
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BibleManagerViewmodel @Inject constructor(
    val sharedPreferenceBible: SharedPreferenceBible,
    private val bibleUseCase: BibleUseCase,
    @ApplicationContext private val context: Context,
    private val databaseDownloader: DatabaseDownloader
) : ViewModel() {
    val DEFAULT_ASSET_DB_NAME = BibleDbFactory.DEFAULT_FILE

    /** Ya no se usa para activar (no hay reinicio); se conserva por compatibilidad con la pantalla. */
    var onNavigateToRestartScreen: (() -> Unit)? = null

    private val _originalBibleList = MutableStateFlow<List<BibleVersionInfo>>(emptyList())

    private val _selectedLanguageFilter = MutableStateFlow<String?>("Todos")
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<String>>(listOf("Todos"))
    val availableLanguages: StateFlow<List<String>> = _availableLanguages.asStateFlow()

    val filteredBibleList: StateFlow<List<BibleVersionInfo>> =
        _originalBibleList.combine(_selectedLanguageFilter) { list, lang ->
            if (lang == null || lang == "Todos") list else list.filter { it.lang == lang }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadStatus = MutableLiveData<String>()
    val downloadStatus: LiveData<String> = _downloadStatus

    init { loadBibleList() }

    fun loadBibleList() {
        viewModelScope.launch {
            val parsedList: List<BibleVersionInfo> = bibleUseCase.invoke()
            val deviceLang = LocaleDetector.detect(context).language.code

            // Las Biblias del idioma del teléfono van primero.
            val updatedList = parsedList.map { bible ->
                bible.copy(isDownloaded = context.getDatabasePath(bible.database_name).exists())
            }.sortedWith(
                compareByDescending<BibleVersionInfo> { it.langVisualCode.equals(deviceLang, true) }
                    .thenBy { it.lang }.thenBy { it.title }
            )
            _originalBibleList.value = updatedList

            val languages = mutableSetOf("Todos")
            parsedList.forEach { languages.add(it.lang) }
            _availableLanguages.value = languages.toList().sorted()
        }
    }

    fun setLanguageFilter(language: String?) { _selectedLanguageFilter.value = language }

    fun startDownload(bibleToDownload: BibleVersionInfo) {
        if (bibleToDownload.isDownloading) {
            _downloadStatus.postValue("Descarga ya en progreso para ${bibleToDownload.title}")
            return
        }

        // Clave por archivo (no por code): en el catálogo hay 'code' repetidos.
        updateBibleItemState(bibleToDownload.database_name) {
            it.copy(isDownloading = true, downloadProgress = 0, isDownloaded = false)
        }

        val downloadUrl = "${bibleToDownload.server_url}${bibleToDownload.database_name}"
        val destinationDir = context.getDatabasePath("any_temp_name.sqlite3").parentFile
        if (destinationDir != null && !destinationDir.exists()) destinationDir.mkdirs()
        val myLocalFile = File(destinationDir, bibleToDownload.database_name)

        _downloadStatus.postValue("Iniciando descarga de ${bibleToDownload.title}...")

        viewModelScope.launch(Dispatchers.IO) {
            databaseDownloader.downloadDatabase(
                downloadUrl,
                myLocalFile,
                object : DatabaseDownloader.DownloadProgressListener {
                    override fun onProgress(progressPercentage: Int) {
                        updateBibleItemState(bibleToDownload.database_name) {
                            it.copy(downloadProgress = progressPercentage)
                        }
                    }

                    override fun onCompleted(file: File) {
                        Log.i("VM_Downloader", "Descarga completa: ${file.name}")
                        updateBibleItemState(bibleToDownload.database_name) {
                            it.copy(isDownloading = false, isDownloaded = true, downloadProgress = 100)
                        }
                        // Recién descargada → queda como Biblia activa al instante.
                        activateDownloadedDatabase(file.name)
                        _downloadStatus.postValue("${bibleToDownload.title} descargada y activada.")
                    }

                    override fun onError(exception: Exception) {
                        Log.e("VM_Downloader", "Error descargando ${bibleToDownload.title}", exception)
                        if (myLocalFile.exists()) myLocalFile.delete()
                        _downloadStatus.postValue("Error al descargar ${bibleToDownload.title}: ${exception.message}")
                        updateBibleItemState(bibleToDownload.database_name) {
                            it.copy(isDownloading = false, downloadProgress = 0)
                        }
                    }
                }
            )
        }
    }

    private fun updateBibleItemState(databaseName: String, updateLogic: (BibleVersionInfo) -> BibleVersionInfo) {
        _originalBibleList.value = _originalBibleList.value.map {
            if (it.database_name == databaseName) updateLogic(it) else it
        }
    }

    /** Activar = guardar cuál es. El lector observa el cambio; no hay copia ni reinicio. */
    fun activateDownloadedDatabase(downloadedDbFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!DbValidator.isValid(context.getDatabasePath(downloadedDbFileName))) {
                _downloadStatus.postValue("Error: '$downloadedDbFileName' no es válida.")
                return@launch
            }
            sharedPreferenceBible.saveSelectedDatabaseFileName(downloadedDbFileName)
            // El idioma de la app sigue al de la Biblia elegida.
            val lang = _originalBibleList.value.firstOrNull { it.database_name == downloadedDbFileName }?.langVisualCode
            com.jbncode.bibliaapp.core.locale.BibleLanguage.sync(sharedPreferenceBible, lang)
        }
    }

    fun activateDefaultDatabase() {
        sharedPreferenceBible.saveSelectedDatabaseFileName(null)
        com.jbncode.bibliaapp.core.locale.BibleLanguage.sync(sharedPreferenceBible, "es") // la incluida (TLA) es en español
    }
}
