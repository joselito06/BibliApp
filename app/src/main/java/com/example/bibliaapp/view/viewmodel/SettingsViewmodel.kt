package com.example.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.data.networking.models.BibleDisplayInfo
import com.example.bibliaapp.data.networking.models.BibleVersionInfo
import com.example.bibliaapp.di.RoomModule
import com.example.bibliaapp.domain.model.AppLanguage
import com.example.bibliaapp.domain.usescases.BibleUseCase
import com.example.bibliaapp.notifications.NotificationScheduler
import com.example.bibliaapp.ui.theme.AppThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime
import javax.inject.Inject


@HiltViewModel
class SettingsViewmodel @Inject constructor(
    val sharedPreferenceBible: SharedPreferenceBible, // Inyectar
    private val notificationScheduler: NotificationScheduler,
    @ApplicationContext private val context: Context,
    private val biblesUsesCases: BibleUseCase
) : ViewModel() {

    // --- Configuración de Idioma ---
    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(sharedPreferenceBible.getAppLanguageCode()))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Exponer los StateFlows directamente desde SharedPreferenceBible
    val notificationsEnabled: StateFlow<Boolean> = sharedPreferenceBible.notificationsEnabled
    val selectedTime: StateFlow<LocalTime> = sharedPreferenceBible.selectedTime

    private val _selectedBibleStorageId = MutableStateFlow<String?>(null) // Almacena el ID de la BD seleccionada
    // Detalle completo de la Biblia activa
    private val _activeBibleDetails = MutableStateFlow<BibleVersionInfo?>(null)
    val activeBibleDetails: StateFlow<BibleVersionInfo?> = _activeBibleDetails.asStateFlow()

    // Este es el nombre legible para mostrar en la UI, derivado del ID almacenado
    val selectedBibleDisplayName: StateFlow<String> = _selectedBibleStorageId.map { storedId ->
        // Necesitas una forma de convertir storedId a displayName
        // Esto podría implicar buscar en una lista de metadatos de biblias descargadas
        // o tener una convención de nombres.
        // Por ahora, si es null, indica que es la de assets.
        if (storedId == null) {
            // Asume que tienes un nombre para tu biblia de assets
            // Podrías obtenerlo de tu RoomModule.DEFAULT_ASSET_DB_NAME
            // y luego mapearlo a un nombre legible.
            getDisplayNameForIdentifier(RoomModule.DEFAULT_ASSET_DB_NAME) ?: "Biblia por Defecto"
        } else {
            getDisplayNameForIdentifier(storedId) ?: storedId // Fallback al ID si no hay nombre
        }
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000), "Cargando...")

    private val _downloadedBibles = MutableStateFlow<List<BibleDisplayInfo>>(emptyList())
    val downloadedBibles: StateFlow<List<BibleDisplayInfo>> = _downloadedBibles.asStateFlow()

    // El identificador de la base de datos por defecto en assets (ej: "tla.sqlite3")
    // Debería coincidir con RoomModule.DEFAULT_ASSET_DB_NAME
    val defaultAssetDbIdentifier = RoomModule.DEFAULT_ASSET_DB_NAME

    // --- NUEVO: Configuración del Tema ---
    private val _appTheme = MutableStateFlow(sharedPreferenceBible.getAppTheme())
    val appTheme: StateFlow<AppThemeType> = _appTheme.asStateFlow()

    private val allBiblesJson = MutableStateFlow<List<BibleVersionInfo>>(emptyList())

    init {
        initializeBibleData()
    }

    private fun initializeBibleData() {
        viewModelScope.launch {
            // Inicializar en estado de carga
            _activeBibleDetails.value = null

            withContext(Dispatchers.IO) {
                try {
                    // 1. Cargar metadatos primero
                    val biblesList = biblesUsesCases.invoke()
                    allBiblesJson.value = biblesList

                    // 2. Configurar biblias seleccionada
                    val selectedId = sharedPreferenceBible.getSelectedDatabaseFileName()
                    _selectedBibleStorageId.value = selectedId

                    // 3. Buscar la biblia activa y validar antes de asignar
                    val activeBible = biblesList.find {
                        it.database_name == selectedId && it.database_name.isNotEmpty()
                    }

                    withContext(Dispatchers.Main){
                        _activeBibleDetails.value = activeBible
                    }

                    // 3. Cargar lista de biblias descargadas con metadatos ya disponibles
                    loadDownloadedBiblesListWithMetadata(biblesList)

                } catch (e: Exception) {
                    Log.e("SettingsVM", "Error cargando metadata de Biblias", e)
                    allBiblesJson.value = emptyList()
                    // Cargar lista sin metadatos como fallback
                    loadDownloadedBiblesListWithMetadata(emptyList())
                }
            }

        }
    }

    private fun loadDownloadedBiblesListWithMetadata(bibleMetadata: List<BibleVersionInfo>) {
        val filesDir = context.filesDir.parentFile ?: return
        val databasesDir = File(filesDir, "databases")
        val downloadedList = mutableListOf<BibleDisplayInfo>()

        // Biblia por defecto
        //val defaultDisplayName = getDisplayNameForIdentifier(defaultAssetDbIdentifier, bibleMetadata) ?: defaultAssetDbIdentifier
        val defaultBibleVersion = getBibleVersionForIdentifier(defaultAssetDbIdentifier, bibleMetadata)
        downloadedList.add(
            BibleDisplayInfo(
                id = defaultBibleVersion?.id ?: -1,
                displayName = defaultBibleVersion?.title ?: "",
                storageIdentifier =  defaultAssetDbIdentifier,
                language = defaultBibleVersion?.langVisualCode ?: "es",
                langCode = defaultBibleVersion?.langCode ?: "spa",
                country = defaultBibleVersion?.country ?: "MX",
                lang = defaultBibleVersion?.lang ?: "Español"
            )
        )

        if (databasesDir.exists() && databasesDir.isDirectory) {
            databasesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".sqlite3") || file.name.endsWith(".db_") || file.name.endsWith(".sqlite")) {
                    if (file.name != RoomModule.ACTIVE_DATABASE_FILENAME && file.name != defaultAssetDbIdentifier) {
                        val displayName = getBibleVersionForIdentifier(file.name, bibleMetadata)
                        downloadedList.add(
                            BibleDisplayInfo(
                                id = displayName?.id ?: -1,
                                displayName =  displayName?.title ?: file.name,
                                storageIdentifier =  file.name,
                                language = displayName?.langVisualCode ?: "es",
                                langCode = displayName?.langCode ?: "spa",
                                country = displayName?.country ?: "MX",
                                lang = displayName?.lang ?: "Español"
                            )
                        )
                    }
                }
            }
        }

        _downloadedBibles.value = downloadedList.distinctBy { it.storageIdentifier }
    }

    // Función helper mejorada
    private fun getDisplayNameForIdentifier(
        identifier: String,
        metadata: List<BibleVersionInfo> = allBiblesJson.value
    ): String? {
        return when {
            identifier == defaultAssetDbIdentifier -> "Biblia TLA (Por defecto)"
            metadata.isNotEmpty() -> metadata.find { it.database_name == identifier }?.title
                ?: identifier.removeSuffix(".sqlite3")
            else -> identifier.removeSuffix(".sqlite3")
        }
    }

    private fun getBibleVersionForIdentifier(
        identifier: String,
        metadata: List<BibleVersionInfo> = allBiblesJson.value
    ): BibleVersionInfo? {
        return when {
            metadata.isNotEmpty() -> metadata.find { it.database_name == identifier }
            else -> null
        }
    }

    fun setSelectedBible(bibleIdentifier: BibleDisplayInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeDbPath = context.getDatabasePath(RoomModule.ACTIVE_DATABASE_FILENAME)

            if (bibleIdentifier.storageIdentifier == defaultAssetDbIdentifier) {
                // El usuario quiere activar la Biblia por defecto (de assets)
                sharedPreferenceBible.saveSelectedDatabaseFileName(null) // o podrías guardar defaultAssetDbIdentifier y RoomModule lo maneja
                // Copiar la de assets a la activa
                context.assets.open(defaultAssetDbIdentifier).use { inputStream ->
                    FileOutputStream(activeDbPath).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _selectedBibleStorageId.value = null // O defaultAssetDbIdentifier si lo guardas así
                Log.i("SettingsVM", "Biblia por defecto '$defaultAssetDbIdentifier' activada. REINICIO REQUERIDO.")

            } else {
                // El usuario quiere activar una Biblia descargada
                val downloadedDbPath = context.getDatabasePath(bibleIdentifier.storageIdentifier)
                if (downloadedDbPath.exists()) {
                    downloadedDbPath.copyTo(activeDbPath, overwrite = true)
                    sharedPreferenceBible.saveSelectedDatabaseFileName(bibleIdentifier.storageIdentifier)
                    setLanguage(bibleIdentifier)
                    _selectedBibleStorageId.value = bibleIdentifier.storageIdentifier
                    Log.i("SettingsVM", "Biblia descargada '$bibleIdentifier' activada. REINICIO REQUERIDO.")
                } else {
                    Log.e("SettingsVM", "La Biblia '$bibleIdentifier' no se encontró para activar.")
                    return@launch
                }
            }
            // Aquí es donde invocarías la navegación a la pantalla de reinicio
            withContext(Dispatchers.Main){
                onNavigateToRestartScreen?.invoke()
            }
        }
    }

    private fun setLanguage(bibleVersion: BibleDisplayInfo){
        val languages = AppLanguage.entries.toTypedArray()
        val lg = languages.first { it.code == bibleVersion.language }

        sharedPreferenceBible.saveAppLanguageCode(lg.code)

    }

    // Callback para la navegación a la pantalla de reinicio
    var onNavigateToRestartScreen: (() -> Unit)? = null

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            sharedPreferenceBible.saveAppLanguageCode(language.code)
            _appLanguage.value = language
            // La actualización visual la manejará MainActivity al recrearse.

            // Aquí es donde invocarías la navegación a la pantalla de reinicio
            withContext(Dispatchers.Main){
                onNavigateToRestartScreen?.invoke()
            }
        }

    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferenceBible.setNotificationsEnabled(enabled)
        // Lógica adicional del ViewModel si es necesaria, como programar/cancelar alarmas
        if (enabled) {
            notificationScheduler.scheduleDailyNotification(selectedTime.value) // Ejemplo
        } else {
            notificationScheduler.cancelDailyNotification() // Ejemplo
        }
    }

    fun setSelectedTime(time: LocalTime) {
        sharedPreferenceBible.setSelectedTime(time)
        // Si las notificaciones están habilitadas, reprogramar con la nueva hora
        if (notificationsEnabled.value) {
            notificationScheduler.scheduleDailyNotification(time) // Ejemplo
        }
    }

    /*fun setSelectedBible(bibleVersion: String) {
        _selectedBible.value = bibleVersion
    }*/

    fun setAppTheme(theme: AppThemeType) {
        viewModelScope.launch {
            sharedPreferenceBible.saveAppTheme(theme)
            _appTheme.value = theme
            // Nota: Cambiar el tema aquí solo actualiza el estado.
            // La MainActivity (o donde apliques el tema globalmente) necesita observar este cambio
            // para redibujar la UI con el nuevo tema. Ya tienes una lógica similar
            // en MainActivity con `sharedPreferenceBible.getAppTheme()` y `onThemeChange`.
        }
    }


    /**
     * Función para refrescar todos los datos de la Biblia
     * Se llama cuando el usuario hace pull-to-refresh
     */
    fun refreshBibleData() {
        viewModelScope.launch {
            try {
                Log.d("SettingsVM", "Iniciando refresh de datos de Biblia")

                // Reinicializar todos los datos
                initializeBibleData()

                Log.d("SettingsVM", "Refresh completado exitosamente")
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error durante el refresh", e)
                // Opcional: emitir un estado de error o mostrar mensaje al usuario
            }
        }
    }

    /**
     * Función alternativa más granular si quieres control más específico
     */
    fun refreshBibleDataDetailed() {
        viewModelScope.launch {
            try {
                Log.d("SettingsVM", "Iniciando refresh detallado")

                // 1. Limpiar estados actuales
                _activeBibleDetails.value = null
                _downloadedBibles.value = emptyList()

                // 2. Recargar metadatos
                withContext(Dispatchers.IO) {
                    val biblesList = biblesUsesCases.invoke()
                    allBiblesJson.value = biblesList
                }

                // 3. Recargar biblia activa
                withContext(Dispatchers.Main) {
                    val selectedId = sharedPreferenceBible.getSelectedDatabaseFileName()
                    _selectedBibleStorageId.value = selectedId

                    val activeBible = allBiblesJson.value.find {
                        it.database_name == selectedId && it.database_name.isNotBlank()
                    }
                    _activeBibleDetails.value = activeBible
                }

                // 4. Recargar lista de biblias descargadas
                withContext(Dispatchers.IO) {
                    loadDownloadedBiblesListWithMetadata(allBiblesJson.value)
                }

                Log.d("SettingsVM", "Refresh detallado completado")
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error en refresh detallado", e)
            }
        }
    }

    /**
     * Función para forzar recarga completa (útil para casos extremos)
     */
    fun forceCompleteRefresh() {
        viewModelScope.launch {
            try {
                Log.d("SettingsVM", "Forzando recarga completa")

                // Limpiar todos los estados
                allBiblesJson.value = emptyList()
                _selectedBibleStorageId.value = ""
                _activeBibleDetails.value = null
                _downloadedBibles.value = emptyList()

                // Esperar un poco para asegurar que los estados se limpien
                delay(100)

                // Recargar desde cero
                initializeBibleData()

            } catch (e: Exception) {
                Log.e("SettingsVM", "Error en recarga completa", e)
            }
        }
    }

}