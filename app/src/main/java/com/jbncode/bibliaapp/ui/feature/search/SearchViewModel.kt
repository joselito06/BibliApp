package com.jbncode.bibliaapp.ui.feature.search

import androidx.lifecycle.ViewModel
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.domain.bible.Book
import com.jbncode.bibliaapp.domain.bible.SearchHit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class SearchScope { ALL, OT, NT }

data class SearchUi(
    val query: String = "",
    val scope: SearchScope = SearchScope.ALL,
    val exact: Boolean = false,
    val loading: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
    val done: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(private val repo: BibleRepository) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui.asStateFlow()

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun setScope(s: SearchScope) = _ui.update { it.copy(scope = s) }
    fun toggleExact() = _ui.update { it.copy(exact = !it.exact) }

    /** La pantalla la llama con retraso (debounce); si cambia algo antes, la corrutina se cancela. */
    suspend fun run(books: List<Book>, ntStart: Int) {
        val s = _ui.value
        if (s.query.trim().length < 2) { _ui.update { it.copy(hits = emptyList(), loading = false, done = false) }; return }
        _ui.update { it.copy(loading = true) }
        val allowed = when (s.scope) {
            SearchScope.ALL -> null
            SearchScope.OT -> books.filter { it.order < ntStart }.map { it.osis }.toSet()
            SearchScope.NT -> books.filter { it.order >= ntStart }.map { it.osis }.toSet()
        }
        val hits = runCatching { repo.search(s.query, allowed, s.exact) }.getOrDefault(emptyList())
        _ui.update { it.copy(hits = hits, loading = false, done = true) }
    }
}
