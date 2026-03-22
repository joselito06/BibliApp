package com.jbncode.bibliaapp.data.database.repositories

import android.content.Context
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.di.GsonModule.readJsonFromAssets
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
        //val listUserType = object : TypeToken<List<BibleVersionInfo>>() {}.type
        //return gson.fromJson(jsonString, listUserType)
        // ✅ TypeToken.getParameterized NO usa clases anónimas,
        //    sobrevive a R8/ProGuard sin reglas adicionales
        val type = TypeToken.getParameterized(List::class.java, BibleVersionInfo::class.java).type
        return gson.fromJson(jsonString, type)
    }
}