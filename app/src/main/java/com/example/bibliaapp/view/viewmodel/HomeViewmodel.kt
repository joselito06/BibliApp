package com.example.bibliaapp.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.domain.usescases.BookUseCase
import com.example.bibliaapp.domain.usescases.VerseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor(private val versesUC: VerseUseCase, private val booksUC: BookUseCase) : ViewModel() {

    fun OnGet(){
        viewModelScope.launch {
            //booksUC.GetAllBookAndVerses()
            //versesUC.getAllVerseWithHeader()
        }
    }




}