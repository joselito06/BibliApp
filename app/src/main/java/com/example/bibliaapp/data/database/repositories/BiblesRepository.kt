package com.example.bibliaapp.data.database.repositories

import android.content.Context
import com.example.bibliaapp.data.networking.models.BibleVersionInfo
import com.example.bibliaapp.di.GsonModule.readJsonFromAssets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BiblesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun getAll(fileName: String, filter: ((BibleVersionInfo) -> Boolean)? = null) : List<BibleVersionInfo> {
        return withContext(Dispatchers.IO){
            val jsonString = readJsonFromAssets(context = context, "$fileName.json")
            val allBibles = jsonString?.let { parseJson(it) } ?: emptyList()

            // Aplicar el filtro si filterText no es nulo ni está vacío
            if (filter != null) {
                allBibles.filter(filter)
            } else {
                allBibles
            }
        }
    }

    private fun parseJson(jsonString: String): List<BibleVersionInfo> {
        val gson = Gson()
        val listUserType = object : TypeToken<List<BibleVersionInfo>>() {}.type
        return gson.fromJson(jsonString, listUserType)
    }
}