package com.example.bibliaapp.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.data.networking.models.VersesDaily
import com.example.bibliaapp.domain.usescases.VersesDailyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VersesViewmodel @Inject constructor(private val versesUC : VersesDailyUseCase) :ViewModel() {

    private val _getAllVerses = MutableLiveData<List<VersesDaily>>()
    val getAllVerses: LiveData<List<VersesDaily>> = _getAllVerses

    fun onGet(){
        viewModelScope.launch {
            val selects = versesUC.invoke()
            _getAllVerses.postValue(selects)
        }
    }

}