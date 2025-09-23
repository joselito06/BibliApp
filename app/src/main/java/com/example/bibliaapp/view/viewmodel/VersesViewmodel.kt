package com.example.bibliaapp.view.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.data.networking.models.VersesDaily
import com.example.bibliaapp.domain.usescases.VersesDailyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val defaultAssetDbIdentifier = "tla.sqlite3"

@HiltViewModel
class VersesViewmodel @Inject constructor(
    private val versesUC : VersesDailyUseCase,
    private val shared: SharedPreferenceBible
) :ViewModel() {

    private val _getAllVerses = MutableStateFlow<List<VersesDaily>>(listOf())
    val getAllVerses: StateFlow<List<VersesDaily>> = _getAllVerses

    private val _verseOfTheDay = MutableStateFlow<VersesDaily>(VersesDaily(verse_id = -1, verse_direction = "", verse_content = "", code = ""))
    val verseOfTheDay: StateFlow<VersesDaily> = _verseOfTheDay

    // Este es el identificador de la base de datos por defecto en assets (ej: "tla.sqlite3")
    // Necesitamos el CÓDIGO de esta Biblia (ej: "tla").
    // Asumamos que el código es el nombre del archivo sin la extensión ".sqlite3".
    // Si tienes una forma más robusta de obtener este código (desde metadatos), úsala.
    private val DEFAULT_BIBLE_CODE_FOR_VERSES = defaultAssetDbIdentifier.removeSuffix(".sqlite3") // ej. "tla"


    init {
        onGet()
    }

    fun onGet(){
        viewModelScope.launch {
            // 1. Obtener el nombre del archivo de la BD seleccionada (ej: "nvi.sqlite3", "rvr1960.sqlite3")
            // Si es null, usamos la de assets (cuyo identificador es defaultAssetDbIdentifier)
            val selectedDbFileName = shared.getSelectedDatabaseFileName() ?: defaultAssetDbIdentifier

            // 2. Convertir el nombre del archivo de la BD al CÓDIGO de la Biblia que usan tus VersesDaily
            // Asumimos que el código es el nombre del archivo sin la extensión ".sqlite3"
            // Ej: "nvi.sqlite3" -> "nvi"; "tla.sqlite3" -> "tla"
            // ¡¡¡ASEGÚRATE DE QUE ESTA CONVENCIÓN SEA CORRECTA PARA TUS DATOS DE VersesDaily!!!
            val targetBibleCode = selectedDbFileName.removeSuffix(".sqlite3").lowercase() // ej. "nvi", "tla"

            Log.i("VersesViewmodel", "Cargando versos diarios para el código de Biblia: '$targetBibleCode'")

            // 3. Usar este targetBibleCode en tu filtro
            try {
                val allDailyVerses = versesUC.invoke("VersosDiarios") // Obtener todos primero

                // Filtrar por el código de la Biblia activa
                val filteredVerses = allDailyVerses.filter { it.code.equals(targetBibleCode, ignoreCase = true) }

                if (filteredVerses.isEmpty() && targetBibleCode != DEFAULT_BIBLE_CODE_FOR_VERSES) {
                    // Si no hay versos para la Biblia seleccionada Y NO es la Biblia por defecto,
                    // intenta cargar los de la Biblia por defecto como fallback.
                    Log.w("VersesViewmodel", "No se encontraron versos para '$targetBibleCode'. Intentando fallback a '$DEFAULT_BIBLE_CODE_FOR_VERSES'.")
                    _getAllVerses.value = allDailyVerses.filter { it.code.equals(DEFAULT_BIBLE_CODE_FOR_VERSES, ignoreCase = true) }
                } else {
                    _getAllVerses.value = filteredVerses
                }

                if (_getAllVerses.value.isEmpty()) {
                    Log.w("VersesViewmodel", "La lista de versos diarios está vacía después del filtro (código: '$targetBibleCode') y fallback.")
                }

                loadVerseOfTheDay()

            } catch (e: Exception) {
                Log.e("VersesViewmodel", "Error al obtener versos diarios: ${e.message}", e)
                _getAllVerses.value = emptyList() // En caso de error, lista vacía
                _verseOfTheDay.value = VersesDaily(verse_id = -1, verse_direction = "", verse_content = "", code = "")
            }
            
            /*val selects = versesUC.invoke("VersosDiarios", filter = { it.code == "tla" })
            _getAllVerses.value = selects
            loadVerseOfTheDay()*/
        }

    }

    private fun loadVerseOfTheDay() {
        if (_getAllVerses.value.isNotEmpty()) _verseOfTheDay.value = shared.getVerseOfTheDay(_getAllVerses.value)
    }

}