package com.jbncode.bibliaapp.view.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.SharedPreferenceBible
import com.jbncode.bibliaapp.data.bible.BibleRepository
import com.jbncode.bibliaapp.data.user.HomeStats
import com.jbncode.bibliaapp.data.user.StatsMigration
import com.jbncode.bibliaapp.data.user.StatsRepository
import com.jbncode.bibliaapp.domain.bible.Book
import com.jbncode.bibliaapp.domain.model.BookRecomendation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor(
    statsRepository: StatsRepository,
    bible: BibleRepository,
    private val migration: StatsMigration,
    private val shared: SharedPreferenceBible,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val stats: StateFlow<HomeStats> = statsRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())

    val recommendations: StateFlow<List<BookRecomendation>> = combine(stats, bible.books()) { s, books -> build(s, books) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { runCatching { migration.runIfNeeded() } }
    }

    fun OnGet() = Unit   // compatibilidad con HomeScreen anterior

    private data class Rec(val osis: String, val title: Int, val desc: Int, val reason: Int, val priority: Int)

    private val catalog = listOf(
        Rec("Psa", R.string.recommendation_psalms_title, R.string.recommendation_psalms_desc, R.string.recommendation_psalms_reason, 1),
        Rec("Pro", R.string.recommendation_proverbs_title, R.string.recommendation_proverbs_desc, R.string.recommendation_proverbs_reason, 1),
        Rec("Mat", R.string.recommendation_matthew_title, R.string.recommendation_matthew_desc, R.string.recommendation_matthew_reason, 2),
        Rec("Joh", R.string.recommendation_john_title, R.string.recommendation_john_desc, R.string.recommendation_john_reason, 2),
        Rec("Gen", R.string.recommendation_genesis_title, R.string.recommendation_genesis_desc, R.string.recommendation_genesis_reason, 2)
    )

    private fun build(s: HomeStats, books: List<Book>): List<BookRecomendation> {
        val current = shared.getCurrentBook() ?: "Gen"
        return catalog.filter { it.osis !in s.completedBooks && !(it.osis == "Gen" && current == "Gen") }
            .mapNotNull { r ->
                books.firstOrNull { it.osis == r.osis }?.let { b ->
                    BookRecomendation(b.osis, b.name, context.getString(r.title), context.getString(r.desc), context.getString(r.reason), r.priority)
                }
            }.sortedBy { it.priority }.take(3)
    }
}
