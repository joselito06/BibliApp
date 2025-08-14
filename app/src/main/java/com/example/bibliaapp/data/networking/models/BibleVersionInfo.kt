package com.example.bibliaapp.data.networking.models

data class BibleVersionInfo(
    val langCode: String,
    val langVisualCode: String,
    val lang: String,
    val country: String,
    val title: String,
    val code: String,
    val description: String,
    val new_testament_number: Int,
    val database_zip_name: String,
    val database_name: String,
    val read_chapter: String,
    val read_verse: String,
    val read_verse_plural: String,
    val read_separator: String,
    val server_url: String,
    val isDownloaded: Boolean = false,
    val downloadDisabled: Boolean = false
)
