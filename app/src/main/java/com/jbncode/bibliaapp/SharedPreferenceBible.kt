package com.jbncode.bibliaapp

import android.content.Context
import android.content.SharedPreferences
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.core.content.edit
import com.jbncode.bibliaapp.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.channels.awaitClose
import java.util.Calendar

private const val SHARED_SELECTED_DB_FILENAME = "selected_db_filename"

class SharedPreferenceBible @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        // ... existing constants ...
        private const val KEY_ANNOUNCE_VERSE_NUMBERS = "announce_verse_numbers"
        private const val KEY_ANNOUNCE_CHAPTER_TITLES = "announce_chapter_titles"

        // Nuevas constantes para estadísticas
        private const val KEY_BOOKS_READ = "books_read"
        private const val KEY_CHAPTERS_READ = "chapters_read"
        private const val KEY_READING_STREAK = "reading_streak"
        private const val KEY_TOTAL_READING_TIME = "total_reading_time_minutes"
        private const val KEY_LAST_READING_DATE = "last_reading_date"
        private const val KEY_VERSES_READ = "verses_read"

        private const val KEY_DAILY_APP_OPENS = "daily_app_opens"
        private const val KEY_LAST_APP_OPEN_DATE = "last_app_open_date"
    }

    private val SHARED_APP_THEME = "app_theme"
    private val SHARED_NAME = "BibleInit"
    private val SHARED_LIBRO = "libro"
    private val SHARED_CAPITULO = "capitulo"
    private val SHARED_CURRENT_INDEX = "current_index"
    private val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private val PREF_SELECTED_HOUR = "selected_hour"
    private val PREF_SELECTED_MINUTE = "selected_minute"

    private val KEY_APP_LANGUAGE = "app_language_code"
    private  val SHARED_VERSE_DAILY_INDEX = "VersePrefs"

    private val storage = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
    private val storage2 = context.getSharedPreferences(SHARED_VERSE_DAILY_INDEX, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // --- Preferencias de Notificación y Hora ---
    private val _notificationsEnabled = MutableStateFlow(
        storage.getBoolean(PREF_NOTIFICATIONS_ENABLED, false) // Leer desde 'storage'
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _selectedTime = MutableStateFlow(
        LocalTime.of(
            storage.getInt(PREF_SELECTED_HOUR, 9), // Leer desde 'storage', Hora por defecto: 09
            storage.getInt(PREF_SELECTED_MINUTE, 0)  // Leer desde 'storage', Minuto por defecto: 00
        )
    )
    val selectedTime: StateFlow<LocalTime> = _selectedTime.asStateFlow()

    // App Language - NUEVO StateFlow para compatibilidad con BibleViewmodel
    private val _appLanguage = MutableStateFlow(
        AppLanguage.fromCode(getAppLanguageCode())
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun saveAppTheme(theme: AppThemeType) {
        storage.edit { putString(SHARED_APP_THEME, theme.name) }
    }

    fun getAppTheme(): AppThemeType {
        val themeName = storage.getString(SHARED_APP_THEME, AppThemeType.SYSTEM_DEFAULT.name)
        return try {
            AppThemeType.valueOf(themeName ?: AppThemeType.SYSTEM_DEFAULT.name)
        } catch (e: IllegalArgumentException) {
            AppThemeType.SYSTEM_DEFAULT // Fallback por si el valor guardado es inválido
        }
    }

    fun saveCapitulo(capitulo: Int) {
        storage.edit { putInt(SHARED_CAPITULO, capitulo) }
    }

    fun saveCurrentBook(book: String) {
        storage.edit { putString(SHARED_LIBRO, book) }
    }

    fun getCapitulo(): Int = storage.getInt(SHARED_CAPITULO, 1)

    fun getCurrentBook(): String? = storage.getString(SHARED_LIBRO, "Gen")

    fun getVerseOfTheDay(verses: List<VersesDaily>): VersesDaily {
        if (verses.isEmpty()) return VersesDaily(verse_id = -1, verse_direction = "", verse_content = "", code = "")

        val today = dateFormat.format(Date())  // Obtener la fecha de hoy en formato YYYY-MM-DD
        val lastSavedDate = storage2.getString("last_verse_date", null)
        var lastIndex = storage2.getInt("last_verse_index", -1)

        if (lastSavedDate == today && lastIndex != -1) {
            // 🔹 Si el día no ha cambiado, devolver el mismo versículo
            return verses[lastIndex]
        } else {
            // 🔹 Si ha cambiado el día, seleccionar el siguiente versículo
            val nextIndex = if (lastIndex + 1 < verses.size) lastIndex + 1 else 0  // Ciclar al inicio si se acaban
            storage2.edit()
                .putString("last_verse_date", today)  // Guardar la nueva fecha
                .putInt("last_verse_index", nextIndex)  // Guardar el nuevo índice
                .apply()

            return verses[nextIndex]
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        with(storage.edit()) { // Usar 'storage'
            putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled)
            apply()
        }
    }

    fun setSelectedTime(time: LocalTime) {
        _selectedTime.value = time
        with(storage.edit()) { // Usar 'storage'
            putInt(PREF_SELECTED_HOUR, time.hour)
            putInt(PREF_SELECTED_MINUTE, time.minute)
            apply()
        }
    }

    // --- Preferencias de Selección de Base de Datos ---
    fun saveSelectedDatabaseFileName(fileName: String?) { // fileName puede ser null para "ninguna descargada seleccionada"
        storage.edit { putString(SHARED_SELECTED_DB_FILENAME, fileName) }
    }

    fun getSelectedDatabaseFileName(): String? {
        // Devuelve el nombre del archivo de la BD descargada seleccionada,
        // o null si no hay una selección explícita (lo que implica la de assets por defecto inicialmente)
        return storage.getString(SHARED_SELECTED_DB_FILENAME, null)
    }

    fun saveAppLanguageCode(languageCode: String) {
        storage.edit { putString(KEY_APP_LANGUAGE, languageCode) }
        // ACTUALIZAR el StateFlow cuando se guarda un nuevo idioma
        _appLanguage.value = AppLanguage.fromCode(languageCode)
    }

    fun getAppLanguageCode(): String {
        // Devuelve el código del idioma, por ejemplo, "en" o "es".
        // Puedes usar el idioma del sistema como valor por defecto si lo deseas,
        // o un idioma fijo como inglés.
        return storage.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
    }

    fun saveAnnounceVerseNumbers(enabled: Boolean) {
        storage.edit { putBoolean(KEY_ANNOUNCE_VERSE_NUMBERS, enabled) }
    }

    fun getAnnounceVerseNumbers(): Boolean {
        return storage.getBoolean(KEY_ANNOUNCE_VERSE_NUMBERS, true) // Default true
    }

    fun saveAnnounceChapterTitles(enabled: Boolean) {
        storage.edit { putBoolean(KEY_ANNOUNCE_CHAPTER_TITLES, enabled) }
    }

    fun getAnnounceChapterTitles(): Boolean {
        return storage.getBoolean(KEY_ANNOUNCE_CHAPTER_TITLES, true) // Default true
    }

    // PROGRESO DE LECTURA
    fun markBookAsRead(bookOsis: String) {
        val booksRead = getBooksRead().toMutableSet()
        booksRead.add(bookOsis)
        storage.edit { putStringSet(KEY_BOOKS_READ, booksRead) }
    }

    fun getBooksRead(): Set<String> {
        return storage.getStringSet(KEY_BOOKS_READ, emptySet()) ?: emptySet()
    }

    fun markChapterAsRead(bookOsis: String, chapter: Int) {
        val chaptersRead = getChaptersRead().toMutableSet()
        chaptersRead.add("$bookOsis-$chapter")
        storage.edit { putStringSet(KEY_CHAPTERS_READ, chaptersRead) }

        // Actualizar racha de lectura
        updateReadingStreak()
    }

    fun getChaptersRead(): Set<String> {
        return storage.getStringSet(KEY_CHAPTERS_READ, emptySet()) ?: emptySet()
    }

    fun getChaptersReadForBook(bookOsis: String): Int {
        return getChaptersRead().count { it.startsWith("$bookOsis-") }
    }

    // ESTADÍSTICAS DE TIEMPO
    fun addReadingTime(minutes: Int) {
        val currentTime = getTotalReadingTime()
        storage.edit { putInt(KEY_TOTAL_READING_TIME, currentTime + minutes) }
    }

    fun getTotalReadingTime(): Int {
        return storage.getInt(KEY_TOTAL_READING_TIME, 0)
    }

    // RACHA DE LECTURA
    private fun updateReadingStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = storage.getString(KEY_LAST_READING_DATE, "")

        if (lastDate == today) {
            // Ya leimos hoy, no cambiar racha
            return
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val currentStreak = getReadingStreak()

        if (lastDate == yesterday) {
            // Leyó ayer, incrementar racha
            storage.edit {
                putInt(KEY_READING_STREAK, currentStreak + 1)
                putString(KEY_LAST_READING_DATE, today)
            }
        } else if (lastDate != today) {
            // No leyó ayer, reiniciar racha
            storage.edit {
                putInt(KEY_READING_STREAK, 1)
                putString(KEY_LAST_READING_DATE, today)
            }
        }
    }

    fun getReadingStreak(): Int {
        return storage.getInt(KEY_READING_STREAK, 0)
    }

    // VERSÍCULOS LEÍDOS
    fun incrementVersesRead(count: Int = 1) {
        val current = getVersesRead()
        storage.edit { putInt(KEY_VERSES_READ, current + count) }
    }

    fun getVersesRead(): Int {
        return storage.getInt(KEY_VERSES_READ, 0)
    }

    // NUEVO: Marcar que la app fue abierta hoy
    fun markDailyAppOpen() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = storage.getString(KEY_LAST_APP_OPEN_DATE, "")

        if (lastDate != today) {
            // Es el primer acceso del día
            storage.edit {
                putString(KEY_LAST_APP_OPEN_DATE, today)
                putInt(KEY_DAILY_APP_OPENS, getDailyAppOpens() + 1)
            }

            // Actualizar racha de lectura si es necesario
            updateReadingStreakOnAppOpen()
        }
    }

    private fun updateReadingStreakOnAppOpen() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastReadingDate = storage.getString(KEY_LAST_READING_DATE, "")

        if (lastReadingDate != today) {
            // Si no ha leído hoy, mantener racha pero no incrementar
            // La racha se incrementará cuando realmente lea algo
        }
    }

    fun getDailyAppOpens(): Int {
        return storage.getInt(KEY_DAILY_APP_OPENS, 0)
    }

    // Flow para observar cambios en el idioma (similar al del tema)
    fun getAppLanguageCodeFlow(): Flow<String> = callbackFlow {
        val listener = object : SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
                if (key == KEY_APP_LANGUAGE) { // Usando tu constante KEY_APP_LANGUAGE
                    trySend(getAppLanguageCode())
                }
            }
        }
        trySend(getAppLanguageCode()) // Emisión inicial
        storage.registerOnSharedPreferenceChangeListener(listener)
        // awaitClose debe ser la última expresión en el bloque callbackFlow
        awaitClose {
            storage.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.conflate()

    // --- Flujo para observar cambios en la selección de la BD (opcional pero útil) ---
    fun getSelectedDatabaseFileNameFlow(): Flow<String?> = callbackFlow<String?> {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SHARED_SELECTED_DB_FILENAME) {
                trySend(getSelectedDatabaseFileName())
            }
        }
        trySend(getSelectedDatabaseFileName()) // Emisión inicial
        storage.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { storage.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate() // conflate() es útil para no perder actualizaciones si el colector es lento

}