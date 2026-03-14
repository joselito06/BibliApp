package com.jbncode.bibliaapp.domain.model

data class BookRecomendation(
    val bookOsis: String,
    val bookName: String,
    val title: String,
    val description: String,
    val reason: String, // Por qué se recomienda
    val priority: Int // 1 = alta, 2 = media, 3 = baja
)
