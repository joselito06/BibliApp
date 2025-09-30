package com.example.bibliaapp.view.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.R
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.domain.model.BookRecomendation
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.ReadingStats
import com.example.bibliaapp.domain.usescases.BookUseCase
import com.example.bibliaapp.domain.usescases.VerseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val versesUC: VerseUseCase,
    private val booksUC: BookUseCase,
    private val shared: SharedPreferenceBible,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _readingStats = MutableStateFlow(
        ReadingStats(0, 0, 0, 0, 0, 0)
    )
    val readingStats: StateFlow<ReadingStats> = _readingStats.asStateFlow()

    private val _recommendations = MutableStateFlow<List<BookRecomendation>>(emptyList())
    val recommendations: StateFlow<List<BookRecomendation>> = _recommendations.asStateFlow()

    private val _allBooks = MutableStateFlow<List<Books>>(emptyList())

    init {
        loadData()
    }
    fun OnGet(){
        viewModelScope.launch {
            //booksUC.GetAllBookAndVerses()
            //versesUC.getAllVerseWithHeader()
        }
    }


    private fun loadData() {
        viewModelScope.launch {
            try {
                // Cargar todos los libros
                val books = booksUC.GetAllBook()
                _allBooks.value = books

                // Calcular estadísticas
                calculateReadingStats(books)

                // Generar recomendaciones
                generateRecommendations(books)

            } catch (e: Exception) {
                Log.e("HomeViewmodel", "Error cargando datos: ${e.message}")
            }
        }
    }

    private fun calculateReadingStats(books: List<Books>) {
        val booksRead = shared.getBooksRead()
        val chaptersRead = shared.getChaptersRead()
        val versesRead = shared.getVersesRead()
        val readingStreak = shared.getReadingStreak()
        val totalTime = shared.getTotalReadingTime()

        _readingStats.value = ReadingStats(
            booksRead = booksRead.size,
            totalBooks = books.size,
            chaptersRead = chaptersRead.size,
            versesRead = versesRead,
            readingStreak = readingStreak,
            totalReadingTimeMinutes = totalTime
        )
    }

    private fun generateRecommendations(books: List<Books>) {
        val booksRead = shared.getBooksRead()
        val currentBook = shared.getCurrentBook() ?: "Gen"

        val recommendations = mutableListOf<BookRecomendation>()

        // Recomendaciones basadas en lógica
        books.forEach { book ->
            when {
                // No leído aún
                !booksRead.contains(book.Osis) -> {
                    when (book.Osis) {
                        "Psa" -> recommendations.add(
                            BookRecomendation(
                                bookOsis = book.Osis,
                                bookName = book.Human,
                                title = context.getString(R.string.recommendation_psalms_title),
                                description = context.getString(R.string.recommendation_psalms_desc),
                                reason = context.getString(R.string.recommendation_psalms_reason),
                                priority = 1
                            )
                        )
                        "Pro" -> recommendations.add(
                            BookRecomendation(
                                bookOsis = book.Osis,
                                bookName = book.Human,
                                title = context.getString(R.string.recommendation_proverbs_title),
                                description = context.getString(R.string.recommendation_proverbs_desc),
                                reason = context.getString(R.string.recommendation_proverbs_reason),
                                priority = 1
                            )
                        )
                        "Mat" -> recommendations.add(
                            BookRecomendation(
                                bookOsis = book.Osis,
                                bookName = book.Human,
                                title = context.getString(R.string.recommendation_matthew_title),
                                description = context.getString(R.string.recommendation_matthew_desc),
                                reason = context.getString(R.string.recommendation_matthew_reason),
                                priority = 2
                            )
                        )
                        "Gen" -> if (currentBook != "Gen") {
                            recommendations.add(
                                BookRecomendation(
                                    bookOsis = book.Osis,
                                    bookName = book.Human,
                                    title = context.getString(R.string.recommendation_genesis_title),
                                    description = context.getString(R.string.recommendation_genesis_desc),
                                    reason = context.getString(R.string.recommendation_genesis_reason),
                                    priority = 2
                                )
                            )
                        }
                        "Joh" -> recommendations.add(
                            BookRecomendation(
                                bookOsis = book.Osis,
                                bookName = book.Human,
                                title = context.getString(R.string.recommendation_john_title),
                                description = context.getString(R.string.recommendation_john_desc),
                                reason = context.getString(R.string.recommendation_john_reason),
                                priority = 2
                            )
                        )
                    }
                }
            }
        }

        // Ordenar por prioridad y tomar los primeros 3
        _recommendations.value = recommendations
            .sortedBy { it.priority }
            .take(3)
    }

    // NUEVO: Función para trackear interacciones del usuario
    fun trackUserInteraction(action: String) {
        when (action) {
            "verse_shared" -> {
                // Incrementar contador de versos compartidos
                //shared.incrementVersesShared()
            }
            "tts_used" -> {
                // Trackear uso de TTS
                //shared.incrementTTSUsage()
            }
            "recommendation_clicked" -> {
                // Trackear clicks en recomendaciones
                //shared.incrementRecommendationClicks()
            }
        }

        // Agregar pequeño tiempo de engagement
        shared.addReadingTime(1)
    }

    // FUNCIÓN PARA MARCAR CAPÍTULO COMO LEÍDO
    fun markChapterAsRead(bookOsis: String, chapter: Int) {
        shared.markChapterAsRead(bookOsis, chapter)
        // Recalcular estadísticas
        calculateReadingStats(_allBooks.value)
    }

    // FUNCIÓN PARA AGREGAR TIEMPO DE LECTURA
    fun addReadingTime(minutes: Int) {
        shared.addReadingTime(minutes)
        calculateReadingStats(_allBooks.value)
    }

    // FUNCIÓN PARA MARCAR LIBRO COMO COMPLETADO
    fun markBookAsCompleted(bookOsis: String) {
        shared.markBookAsRead(bookOsis)
        calculateReadingStats(_allBooks.value)
        // Regenerar recomendaciones
        generateRecommendations(_allBooks.value)
    }

}