package com.example.bibliaapp.view.viewmodel

import androidx.activity.result.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.domain.model.AppLanguage
import com.example.bibliaapp.notifications.NotificationScheduler
import com.example.bibliaapp.ui.theme.AppThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject


@HiltViewModel
class SettingsViewmodel @Inject constructor(
    private val sharedPreferenceBible: SharedPreferenceBible, // Inyectar
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    // --- Configuración de Idioma ---
    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(sharedPreferenceBible.getAppLanguageCode()))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Exponer los StateFlows directamente desde SharedPreferenceBible
    val notificationsEnabled: StateFlow<Boolean> = sharedPreferenceBible.notificationsEnabled
    val selectedTime: StateFlow<LocalTime> = sharedPreferenceBible.selectedTime

    private val _selectedBible = MutableStateFlow("Reina-Valera 1960") // Versión por defecto
    val selectedBible: StateFlow<String> = _selectedBible.asStateFlow()

    // --- NUEVO: Configuración del Tema ---
    private val _appTheme = MutableStateFlow(sharedPreferenceBible.getAppTheme())
    val appTheme: StateFlow<AppThemeType> = _appTheme.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            sharedPreferenceBible.saveAppLanguageCode(language.code)
            _appLanguage.value = language
            // La actualización visual la manejará MainActivity al recrearse.
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

    fun setSelectedBible(bibleVersion: String) {
        _selectedBible.value = bibleVersion
    }

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
}