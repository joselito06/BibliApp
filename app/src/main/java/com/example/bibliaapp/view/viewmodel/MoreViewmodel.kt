package com.example.bibliaapp.view.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bibliaapp.domain.usescases.VerseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoreViewmodel @Inject constructor(val ruseca: VerseUseCase) : ViewModel() {
}