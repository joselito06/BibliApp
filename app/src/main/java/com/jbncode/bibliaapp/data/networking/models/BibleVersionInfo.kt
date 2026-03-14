package com.jbncode.bibliaapp.data.networking.models

data class BibleVersionInfo(
    val id: Int,
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
    var isDownloaded: Boolean = false,
    var downloadProgress: Int = 0,
    var isDownloading: Boolean = false,
    var downloadDisabled: Boolean = false
)
