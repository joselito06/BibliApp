package com.example.bibliaapp.view.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime


class SettingsViewmodel : ViewModel() {
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> get() = _notificationsEnabled

    private val _selectedTime = MutableStateFlow(LocalTime.of(8, 0))
    val selectedTime: StateFlow<LocalTime> get() = _selectedTime

    private val _selectedBible = MutableStateFlow("Reina-Valera 1960") // Versión por defecto
    val selectedBible: StateFlow<String> = _selectedBible.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setSelectedTime(time: LocalTime) {
        _selectedTime.value = time
    }

    fun setSelectedBible(bibleVersion: String) {
        _selectedBible.value = bibleVersion
    }
}