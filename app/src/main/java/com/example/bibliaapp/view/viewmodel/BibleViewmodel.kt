package com.example.bibliaapp.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.usescases.BookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BibleViewmodel @Inject constructor(private val booksUC: BookUseCase, private val shared: SharedPreferenceBible) : ViewModel() {
    val validChapterItem = MutableLiveData<Boolean>()
    val numChapterItem = MutableLiveData<Int>()
    val sharedComp = MutableLiveData<SharedPreferenceBible>()

    private val _getAllBook = MutableLiveData<List<Books>>()
    val getAllBook: LiveData<List<Books>> = _getAllBook

    private val _getAllBiblia = MutableLiveData<List<Books>>()
    val getAllBiblia : LiveData<List<Books>> = _getAllBiblia

    private var _Capitulo = MutableLiveData(1)
    var capitulo : LiveData<Int> = _Capitulo

    private var _Libro = MutableLiveData(shared.GetCurrentBook()!!)
    var libro : LiveData<String> = _Libro

    private var _LibroIndex = MutableLiveData(0)
    var libroIndex : LiveData<Int> = _LibroIndex

    private var _numLibro = MutableLiveData(1)
    var numLibro : LiveData<Int> = _numLibro

    fun onGet(){
        viewModelScope.launch {
            val booksSelect = booksUC.invoke();
            _getAllBook.postValue(booksSelect)
            _Libro.postValue(booksSelect.first().Osis)
            var list = booksUC.GetAllBookAndVerses(_Libro.value!!)
            _getAllBiblia.postValue(list)
            validChapterItem.postValue(true)
            numChapterItem.postValue(0)
            sharedComp.postValue(shared)
        }
    }

    /*fun onGetLibro(){
        viewModelScope.launch {
            val list = libroUsercase.invoke()
            _getAllLibro.postValue(list)
        }
    }*/

    fun getByCapitulo(){
        viewModelScope.launch {
            //val list = booksUC.getBibliaByLibroAndCapitulo(1,capitulo.value!!.toInt())
            //_getAllBiblia.postValue(list)
        }
    }

    fun getBookIndex(){
        _getAllBook.value!!
    }

    fun plusBook(){
        viewModelScope.launch {
            if (_LibroIndex.value!!.toInt() < _getAllBook.value!!.size - 1){
                _LibroIndex.value = _LibroIndex.value!!.toInt() + 1
                _Libro.value = _getAllBook.value!![_LibroIndex.value!!.toInt()].Osis
                //_Libro.postValue(_getAllBook.value!![_LibroIndex.value!!.toInt()].Osis)
            }
            try {
                _getAllBiblia.postValue(booksUC.GetAllBookAndVerses(_getAllBook.value!![_LibroIndex.value!!.toInt()].Osis))
            }
            catch (e: Exception){

            }

        }

    }

    fun lessBook(){
        viewModelScope.launch {
            if (_LibroIndex.value!!.toInt() >= 1){
                _LibroIndex.value = _LibroIndex.value!!.toInt() - 1
                _Libro.value = _getAllBook.value!![_LibroIndex.value!!.toInt()].Osis
                //_Libro.postValue(_getAllBook.value!![_LibroIndex.value!!.toInt()].Osis)
            }
            try{
                _getAllBiblia.postValue(booksUC.GetAllBookAndVerses(_getAllBook.value!![_LibroIndex.value!!.toInt()].Osis))
            }catch (e: Exception){}

        }
    }

    fun aumentarCapitulo(){
        if (_Capitulo.value!!.toInt() < _getAllBiblia.value!!.first().Chapters - 1){
            _Capitulo.value = _Capitulo.value!!.toInt() + 1
            //getByCapitulo()
        }

    }

    fun decrementCapitulo(){
        if(_Capitulo.value!!.toInt() >= 2){
            _Capitulo.value = _Capitulo.value!!.toInt() - 1
            //getByCapitulo()
        }

    }
}