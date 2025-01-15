package com.example.bibliaapp.data.database.repositories

import android.content.Context
import com.example.bibliaapp.data.networking.models.VersesDaily
import com.example.bibliaapp.di.GsonModule.readJsonFromAssets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VersesDailyRepository @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun GetAll() : List<VersesDaily> {
        return withContext(Dispatchers.IO){
            val jsonString = readJsonFromAssets(context = context, "VersosDiarios.json")
            jsonString?.let { parseJson(it) } ?: emptyList()
        }
    }

    private fun parseJson(jsonString: String): List<VersesDaily> {
        val gson = Gson()
        val listUserType = object : TypeToken<List<VersesDaily>>() {}.type
        return gson.fromJson(jsonString, listUserType)
    }

}