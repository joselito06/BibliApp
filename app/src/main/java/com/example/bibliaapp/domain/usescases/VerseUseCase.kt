package com.example.bibliaapp.domain.usescases

import com.example.bibliaapp.data.database.repositories.HeaderRepository
import com.example.bibliaapp.data.database.repositories.VerseRepository
import com.example.bibliaapp.domain.model.Header
import com.example.bibliaapp.domain.model.Verses
import javax.inject.Inject

class VerseUseCase @Inject constructor(private val versesRepo: VerseRepository, private val headerRepo: HeaderRepository) {

    suspend operator fun invoke(): List<Verses> {
        val list: List<Verses> = versesRepo.GetAll()
        return list
    }

    suspend fun getAllVerseWithHeader(): List<Verses>{
        val verses: List<Verses> = versesRepo.GetAll()
        val headers: List<Header> = headerRepo.GetAll()

        val versesResponse = mutableListOf<Verses>()
        var valid: Boolean = false

        for ( verse in verses){
            valid = true
            for (header in headers){
                if (header.BookId == verse.Book && header.ChapteId == verse.Chapter && header.VerseId == verse.Verse){
                    versesResponse.add( Verses(
                        Number = verse.Number,
                        Book = verse.Book,
                        Chapter = verse.Chapter,
                        Verse = verse.Verse,
                        Contenido = verse.Contenido,
                        ChapterName = header.Titulo,
                        ChapterNum = null
                    ))
                    valid = false
                    break
                }
            }
            if (valid == true){
                versesResponse.add( Verses(
                    Number = verse.Number,
                    Book = verse.Book,
                    Chapter = verse.Chapter,
                    Verse = verse.Verse,
                    Contenido = verse.Contenido,
                    ChapterName = "",
                    ChapterNum = null
                ))
            }
        }
        return versesResponse
    }
}