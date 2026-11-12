package com.jbncode.bibliaapp.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.data.user.AnnotationsRepository
import com.jbncode.bibliaapp.data.user.UserItemEntity
import com.jbncode.bibliaapp.domain.bible.Verse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryEntry(val item: UserItemEntity, val bookName: String, val text: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: AnnotationsRepository,
    private val bible: BibleRepository
) : ViewModel() {

    /** Une cada elemento guardado con el texto del versículo en la Biblia ACTIVA. */
    fun entries(type: String): Flow<List<LibraryEntry>> =
        combine(repo.byType(type), bible.books()) { items, books -> items to books }
            .mapLatest { (items, books) ->
                val chapters = HashMap<String, List<Verse>>()
                items.map { it ->
                    val verses = chapters.getOrPut("${it.book}|${it.chapter}") { bible.chapter(it.book, it.chapter) }
                    LibraryEntry(
                        item = it,
                        bookName = books.firstOrNull { b -> b.osis == it.book }?.name ?: it.book,
                        text = verses.filter { v -> v.verse in it.verse..it.verseEnd }.joinToString(" ") { v -> v.text }
                    )
                }
            }

    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
}
