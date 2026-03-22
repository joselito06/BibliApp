package com.jbncode.bibliaapp.data.database.repositories

import android.content.Context
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.di.GsonModule.readJsonFromAssets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VersesDailyRepository @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun GetAll(fileName: String, filter: ((VersesDaily) -> Boolean)? = null) : List<VersesDaily> {
        return withContext(Dispatchers.IO){
            val jsonString = readJsonFromAssets(context = context, "$fileName.json")
            val allVerses = jsonString?.let { parseJson(it) } ?: emptyList()

            if (filter != null) {
                allVerses.filter(filter)
            } else {
                allVerses
            }
        }
    }

    private fun parseJson(jsonString: String): List<VersesDaily> {
        val gson = Gson()
        //val listUserType = object : TypeToken<List<VersesDaily>>() {}.type
        //return gson.fromJson(jsonString, listUserType)
        // ✅ Mismo fix que BiblesRepository
        val type = TypeToken.getParameterized(List::class.java, VersesDaily::class.java).type
        return gson.fromJson(jsonString, type)
    }

}