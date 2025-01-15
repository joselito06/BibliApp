package com.example.bibliaapp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferenceBible @Inject constructor(@ApplicationContext context: Context) {
    private val SHARED_NAME = "BibleInit"
    private val SHARED_LIBRO = "libro"
    private val SHARED_CAPITULO = "capitulo"
    private val SHARED_CURRENT_INDEX = "current_index"

    private val storage = context.getSharedPreferences(SHARED_NAME, 0)

//    fun SaveLibro(libro: String){
//        storage.edit().putString(SHARED_LIBRO, libro).apply()
//    }
    fun SaveCapitulo(capitulo: Int){
        storage.edit().putInt(SHARED_CAPITULO, capitulo).apply()
    }

    fun SaveCurrentIndex(key: String,index: Int){
        storage.edit().putInt(key, index).apply()
    }

    fun SaveCurrentBook(book: String){
        storage.edit().putString(SHARED_LIBRO,book).apply()
    }

//    fun GetLibro():String{
//        return storage.getString(SHARED_LIBRO, "")!!
//    }

    fun GetCapitulo():Int{
        return storage.getInt(SHARED_CAPITULO, 0)
    }

    fun GetCurrentIndex(key:String?):Int{
        return storage.getInt(key, 0)
    }

    fun GetCurrentBook():String?{
        return storage.getString(SHARED_LIBRO, "")
    }
}