package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jbncode.bibliaapp.NotificationScheduler
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleDbFactory
import com.jbncode.bibliaapp.data.networking.models.BibleDisplayInfo
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.utils.VersionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewmodel @Inject constructor(
    val sharedPreferenceBible: SharedPreferenceBible,
    private val notificationScheduler: NotificationScheduler,
    @ApplicationContext private val context: Context,
    private val biblesUsesCases: BibleUseCase
) : ViewModel() {

    val defaultAssetDbIdentifier = BibleDbFactory.DEFAULT_FILE

    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(sharedPreferenceBible.getAppLanguageCode()))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val notificationsEnabled: StateFlow<Boolean> = sharedPreferenceBible.notificationsEnabled
    val selectedTime: StateFlow<LocalTime> = sharedPreferenceBible.selectedTime

    private val _showForceUpdate = MutableStateFlow(false)
    val showForceUpdate: StateFlow<Boolean> = _showForceUpdate

    private val _activeBibleDetails = MutableStateFlow<BibleVersionInfo?>(null)
    val activeBibleDetails: StateFlow<BibleVersionInfo?> = _activeBibleDetails.asStateFlow()

    val selectedBibleDisplayName: StateFlow<String> = _activeBibleDetails
        .map { it?.title ?: "…" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "…")

    private val _downloadedBibles = MutableStateFlow<List<BibleDisplayInfo>>(emptyList())
    val downloadedBibles: StateFlow<List<BibleDisplayInfo>> = _downloadedBibles.asStateFlow()

    private val _appTheme = MutableStateFlow(sharedPreferenceBible.getAppTheme())
    val appTheme: StateFlow<AppThemeType> = _appTheme.asStateFlow()

    /** Solo se usa al cambiar el idioma de la app. Cambiar de Biblia ya NO reinicia. */
    var onNavigateToRestartScreen: (() -> Unit)? = null

    init { refreshBibleData() }

    fun refreshBibleData() {
        viewModelScope.launch {
            val catalog = withContext(Dispatchers.IO) {
                try { biblesUsesCases() } catch (e: Exception) { Log.e("SettingsVM", "catálogo", e); emptyList() }
            }
            val file = sharedPreferenceBible.getSelectedDatabaseFileName() ?: defaultAssetDbIdentifier
            _activeBibleDetails.value = catalog.firstOrNull { it.database_name == file }
            _downloadedBibles.value = withContext(Dispatchers.IO) { listDownloaded(catalog) }
        }
    }

    private fun BibleVersionInfo?.toDisplay(file: String) = BibleDisplayInfo(
        id = this?.id ?: -1,
        displayName = this?.title ?: file.substringBeforeLast('.'),
        storageIdentifier = file,
        language = this?.langVisualCode ?: "es",
        langCode = this?.langCode ?: "spa",
        country = this?.country ?: "MX",
        lang = this?.lang ?: "Español"
    )

    private fun listDownloaded(catalog: List<BibleVersionInfo>): List<BibleDisplayInfo> {
        val result = mutableListOf(catalog.firstOrNull { it.database_name == defaultAssetDbIdentifier }.toDisplay(defaultAssetDbIdentifier))
        val dir = context.getDatabasePath("x").parentFile ?: return result
        dir.listFiles()?.forEach { f ->
            val isBible = f.isFile && (f.name.endsWith(".sqlite3") || f.name.endsWith(".sqlite") || f.name.endsWith(".db_"))
            // idx_* son los índices de búsqueda; no son Biblias.
            if (isBible && !f.name.startsWith("idx_") && f.name != defaultAssetDbIdentifier) {
                result += catalog.firstOrNull { it.database_name == f.name }.toDisplay(f.name)
            }
        }
        return result.distinctBy { it.storageIdentifier }
    }

    /** Activar una Biblia = guardar cuál es. El lector observa el cambio; no hay copia ni reinicio. */
    fun setSelectedBible(bible: BibleDisplayInfo) {
        val isDefault = bible.storageIdentifier == defaultAssetDbIdentifier
        sharedPreferenceBible.saveSelectedDatabaseFileName(if (isDefault) null else bible.storageIdentifier)
        refreshBibleData()
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            sharedPreferenceBible.saveAppLanguageCode(language.code)
            _appLanguage.value = language
            onNavigateToRestartScreen?.invoke()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferenceBible.setNotificationsEnabled(enabled)
        if (enabled) notificationScheduler.scheduleDailyNotification(selectedTime.value)
        else notificationScheduler.cancelDailyNotification()
    }

    fun setSelectedTime(time: LocalTime) {
        sharedPreferenceBible.setSelectedTime(time)
        if (notificationsEnabled.value) notificationScheduler.scheduleDailyNotification(time)
    }

    fun setAppTheme(theme: AppThemeType) {
        sharedPreferenceBible.saveAppTheme(theme)
        _appTheme.value = theme
    }

    fun rescheduleIfEnabled() {
        if (sharedPreferenceBible.notificationsEnabled.value) {
            notificationScheduler.scheduleDailyNotification(sharedPreferenceBible.selectedTime.value)
        }
    }

    fun checkVersion(currentVersion: String) {
        val min = FirebaseRemoteConfig.getInstance().getString("min_version_required")
        if (VersionUtils.isUpdateRequired(currentVersion, min)) _showForceUpdate.value = true
    }
}
