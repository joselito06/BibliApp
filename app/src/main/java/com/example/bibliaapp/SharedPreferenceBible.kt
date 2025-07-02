package com.example.bibliaapp

import android.content.Context
import com.example.bibliaapp.data.networking.models.VersesDaily
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SharedPreferenceBible @Inject constructor(@ApplicationContext context: Context) {
    private val SHARED_NAME = "BibleInit"
    private val SHARED_LIBRO = "libro"
    private val SHARED_CAPITULO = "capitulo"
    private val SHARED_CURRENT_INDEX = "current_index"

    private  val SHARED_VERSE_DAILY_INDEX = "VersePrefs"

    private val storage = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
    private val storage2 = context.getSharedPreferences(SHARED_VERSE_DAILY_INDEX, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun saveCapitulo(capitulo: Int) {
        storage.edit().putInt(SHARED_CAPITULO, capitulo).apply()
    }

    fun saveCurrentBook(book: String) {
        storage.edit().putString(SHARED_LIBRO, book).apply()
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
}