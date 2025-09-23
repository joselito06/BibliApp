package com.example.bibliaapp.data.networking.models

data class BibleDisplayInfo(
    val id: Int,
    val displayName: String, // "Reina-Valera 1960"
    val storageIdentifier: String, // "RVR1960.sqlite3" o el identificador único que uses
    val language: String, // "es"
    val langCode: String, // spa
    val country: String, // MEX
    val lang: String, // Español

)
