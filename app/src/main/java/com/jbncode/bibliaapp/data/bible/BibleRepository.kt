package com.jbncode.bibliaapp.data.bible

import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.search.SearchIndex
import com.jbncode.bibliaapp.domain.bible.Book
import com.jbncode.bibliaapp.domain.bible.SearchHit
import com.jbncode.bibliaapp.domain.bible.Verse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BibleRepository @Inject constructor(
    private val factory: BibleDbFactory,
    private val prefs: SharedPreferenceBible,
    private val searchIndex: SearchIndex
) {
    /** Archivo de la Biblia activa. Al cambiar, todo lo que lo observe se actualiza solo. */
    val activeFile: Flow<String> = prefs.getSelectedDatabaseFileNameFlow()
        .map { it ?: BibleDbFactory.DEFAULT_FILE }
        .distinctUntilChanged()

    fun books(): Flow<List<Book>> = activeFile.mapLatest { file ->
        factory.open(file).second.readerDao().books()
            .map { Book(it.osis, it.human, it.chapters, it.number ?: 0) }
    }

    suspend fun chapter(book: String, chapter: Int): List<Verse> {
        val dao = factory.open(activeFile.first()).second.readerDao()
        return dao.chapter(book, chapter).mapNotNull { r ->
            val text = r.text?.trim()
            if (text.isNullOrEmpty() || r.verse == null) null
            else Verse(book, chapter, r.verse, text, r.heading?.takeIf { it.isNotBlank() })
        }
    }

    suspend fun chapterCount(book: String): Int =
        factory.open(activeFile.first()).second.readerDao().chapters(book).size

    suspend fun search(
        query: String,
        books: Set<String>? = null,
        exactPhrase: Boolean = false
    ): List<SearchHit> {
        val (file, _) = factory.open(activeFile.first())
        return searchIndex.search(file, query, books, exactPhrase)
    }
}
