package com.jbncode.bibliaapp.ui.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.data.user.AnnotationsRepository
import com.jbncode.bibliaapp.data.user.UserItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnotationsViewModel @Inject constructor(private val repo: AnnotationsRepository) : ViewModel() {
    fun chapter(book: String, chapter: Int) = repo.chapter(book, chapter)
    fun toggleFavorite(book: String, chapter: Int, verses: Set<Int>) { viewModelScope.launch { repo.toggleFavorite(book, chapter, verses) } }
    fun setHighlight(book: String, chapter: Int, verses: Set<Int>, color: Int?) { viewModelScope.launch { repo.setHighlight(book, chapter, verses, color) } }
    suspend fun noteAt(book: String, chapter: Int, verse: Int) = repo.noteAt(book, chapter, verse)
    fun saveNote(book: String, chapter: Int, from: Int, to: Int, text: String, existing: UserItemEntity?) {
        viewModelScope.launch { repo.saveNote(book, chapter, from, to, text, existing) }
    }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
}
