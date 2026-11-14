package com.jbncode.bibliaapp.ui.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.DatabaseDownloader
import com.jbncode.bibliaapp.NotificationScheduler
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.core.locale.BibleLanguage
import com.jbncode.bibliaapp.core.locale.LocaleDetector
import com.jbncode.bibliaapp.core.locale.RegionBibleMap
import com.jbncode.bibliaapp.data.bible.BibleDbFactory
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.data.bible.DbValidator
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.domain.usescases.BibleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class OnboardingUi(
    val step: Int = 0,                       // 0 idioma · 1 Biblia · 2 avisos · 3 guía
    val language: AppLanguage = AppLanguage.ENGLISH,
    val detected: AppLanguage = AppLanguage.ENGLISH,
    val catalogLoaded: Boolean = false,
    val options: List<BibleVersionInfo> = emptyList(),
    val selectedCode: String? = null,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val error: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferenceBible,
    private val bibles: BibleUseCase,
    private val downloader: DatabaseDownloader,
    private val scheduler: NotificationScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val deviceLocale = LocaleDetector.detect(context)
    private var catalog: List<BibleVersionInfo> = emptyList()
    private var pendingRestart = false

    private val _ui = MutableStateFlow(
        OnboardingUi(
            language = deviceLocale.language,   // el radio arranca en el idioma detectado del teléfono
            detected = deviceLocale.language
        )
    )
    val ui: StateFlow<OnboardingUi> = _ui.asStateFlow()

    init {
        // Primera vez: la app arranca ya en el idioma del teléfono (el usuario puede cambiarlo en el primer paso).
        if (prefs.getAppLanguageCode() != deviceLocale.language.code) prefs.saveAppLanguageCode(deviceLocale.language.code)
        viewModelScope.launch {
            catalog = try { bibles() } catch (e: Exception) { emptyList() }
            _ui.update { it.withOptions(it.language, loaded = true) }
        }
    }

    private fun OnboardingUi.withOptions(lang: AppLanguage, loaded: Boolean = catalogLoaded): OnboardingUi {
        val opts = RegionBibleMap.recommended(lang.code, deviceLocale.country).mapNotNull { code ->
            catalog.firstOrNull { it.code.equals(code, ignoreCase = true) && !it.downloadDisabled }
        }
        return copy(language = lang, catalogLoaded = loaded, options = opts, selectedCode = opts.firstOrNull()?.code, error = false)
    }

    fun selectLanguage(l: AppLanguage) = _ui.update { it.withOptions(l) }
    fun selectBible(code: String) = _ui.update { it.copy(selectedCode = code, error = false) }
    fun next() = _ui.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    fun back() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = false) }

    /** Guardar el idioma aplica el locale (la actividad se recrea; el estado vive aquí). */
    fun confirmLanguage() {
        prefs.saveAppLanguageCode(_ui.value.language.code)
        next()
    }

    fun confirmBible() {
        val s = _ui.value
        val bible = s.options.firstOrNull { it.code == s.selectedCode }
        if (bible == null) { useIncluded(); return }
        if (bible.database_name == BibleDbFactory.DEFAULT_FILE) { syncLanguage(bible); useIncluded(); return }

        val target = context.getDatabasePath(bible.database_name)
        if (target.exists() && DbValidator.isValid(target)) { applyBible(bible); return }

        _ui.update { it.copy(downloading = true, progress = 0, error = false) }
        val part = File(target.parentFile, bible.database_name + ".part")
        viewModelScope.launch {
            target.parentFile?.mkdirs(); part.delete()
            downloader.downloadDatabase(
                "${bible.server_url}${bible.database_name}", part,
                object : DatabaseDownloader.DownloadProgressListener {
                    override fun onProgress(progressPercentage: Int) = _ui.update { it.copy(progress = progressPercentage) }
                    override fun onCompleted(file: File) {
                        viewModelScope.launch(Dispatchers.IO) { finishDownload(bible, part, target) }
                    }
                    override fun onError(exception: Exception) { part.delete(); fail() }
                }
            )
        }
    }

    // Descarga a ".part", valida y solo entonces renombra: nunca queda una Biblia a medias.
    private fun finishDownload(bible: BibleVersionInfo, part: File, target: File) {
        if (!DbValidator.isValid(part)) { part.delete(); fail(); return }
        target.delete()
        if (!part.renameTo(target)) { part.delete(); fail(); return }
        applyBible(bible)
    }

    private fun applyBible(bible: BibleVersionInfo) {
        prefs.saveSelectedDatabaseFileName(bible.database_name)
        syncLanguage(bible)
        pendingRestart = true   // el lector actual aún lee "biblia_activa"; se reinicia al terminar
        _ui.update { it.copy(downloading = false, progress = 100, step = 2) }
    }

    /** El idioma de la app pasa a ser el de la Biblia elegida. */
    private fun syncLanguage(bible: BibleVersionInfo) {
        BibleLanguage.sync(prefs, bible.langVisualCode)?.let { l -> _ui.update { it.copy(language = l) } }
    }

    private fun fail() = _ui.update { it.copy(downloading = false, error = true) }

    fun useIncluded() {
        prefs.saveSelectedDatabaseFileName(null)
        _ui.update { it.copy(error = false, step = 2) }
    }

    fun onNotificationsResult(granted: Boolean) {
        if (granted) {
            prefs.setNotificationsEnabled(true)
            scheduler.scheduleDailyNotification(prefs.selectedTime.value)
        }
        next()
    }

    /** @return true si hay que reiniciar para activar la Biblia descargada. */
    fun finish(): Boolean {
        prefs.markOnboardingDone()
        prefs.markFirstLaunchDone()
        return pendingRestart
    }
}
