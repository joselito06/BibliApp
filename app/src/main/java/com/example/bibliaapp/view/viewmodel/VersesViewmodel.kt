package com.example.bibliaapp.view.viewmodel

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

@HiltViewModel
class VersesViewmodel @Inject constructor(private val versesUC : VersesDailyUseCase,
                                          private val shared: SharedPreferenceBible) :ViewModel() {

    private val _getAllVerses = MutableStateFlow<List<VersesDaily>>(listOf())
    val getAllVerses: StateFlow<List<VersesDaily>> = _getAllVerses

    private val _verseOfTheDay = MutableStateFlow<VersesDaily>(VersesDaily(verse_id = -1, verse_direction = "", verse_content = "", code = ""))
    val verseOfTheDay: StateFlow<VersesDaily> = _verseOfTheDay

    init {
        onGet()
    }

    fun onGet(){
        viewModelScope.launch {
            val selects = versesUC.invoke("VersosDiarios", filter = { it.code == "tla" })
            _getAllVerses.value = selects
            loadVerseOfTheDay()
        }

    }

    private fun loadVerseOfTheDay() {
        if (_getAllVerses.value.isNotEmpty()) _verseOfTheDay.value = shared.getVerseOfTheDay(_getAllVerses.value)
    }

}