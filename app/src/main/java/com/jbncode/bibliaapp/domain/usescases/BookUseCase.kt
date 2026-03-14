package com.jbncode.bibliaapp.domain.usescases

import com.jbncode.bibliaapp.data.database.repositories.BooksRepository
import com.jbncode.bibliaapp.data.database.repositories.HeaderRepository
import com.jbncode.bibliaapp.data.database.repositories.VerseRepository
import com.jbncode.bibliaapp.domain.model.Books
import com.jbncode.bibliaapp.domain.model.Header
import com.jbncode.bibliaapp.domain.model.Verses
import javax.inject.Inject

class BookUseCase @Inject constructor(
    private val booksRepo: BooksRepository,
    private val versesRepo: VerseRepository,
    private val headerRepo: HeaderRepository) {

    suspend operator fun invoke(): List<Books> {
        val list: List<Books> = booksRepo.GetAll()
        return list
    }
    suspend fun GetBookByLibro(book: String): List<Books>{
        val list: List<Books> = booksRepo.GetBookByLibro(book)
        return list
    }

    suspend fun GetAllBook(): List<Books>{
        val list: List<Books> = booksRepo.GetAll()
        return list
    }

    suspend fun GetAllBookAndVerses() : List<Books>{
        val books: List<Books> = booksRepo.GetAll()
        val verses: List<Verses> = versesRepo.GetAll()
        val headers: List<Header> = headerRepo.GetAll()

        val booksResponse = mutableListOf<Books>()
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
                    )
                    )
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
                )
                )
            }
        }

        for (book in books){
            booksResponse.add( Books(
                Number = book.Number,
                Osis = book.Osis,
                Human = book.Human,
                Chapters = book.Chapters,
                Verse = versesResponse.filter { v -> v.Book == book.Osis }.toMutableList()
            )
            )
        }

        return booksResponse
    }

    suspend fun GetAllBookAndVerses(book: String) : List<Books>{
        val books: List<Books> = booksRepo.GetBookByLibro(book)
        val verses: List<Verses> = versesRepo.GetVerseByLibro(book)
        val headers: List<Header> = headerRepo.GetHeaderByLibro(book)

        val booksResponse = mutableListOf<Books>()
        val versesResponse = mutableListOf<Verses>()
        var valid: Boolean = false

        for ( verse in verses){
            valid = true
            for (header in headers){
                if (header.BookId == verse.Book && header.ChapteId == verse.Chapter && header.VerseId == verse.Verse){
                    versesResponse.add(
                        Verses(
                            Number = verse.Number,
                            Book = verse.Book,
                            Chapter = verse.Chapter,
                            Verse = verse.Verse,
                            Contenido = verse.Contenido,
                            ChapterName = header.Titulo,
                            ChapterNum = if (verse.Verse == 1) verse.Chapter else null
                        )
                    )
                    valid = false
                    break
                }
            }
            if (valid){
                versesResponse.add(
                    Verses(
                        Number = verse.Number,
                        Book = verse.Book,
                        Chapter = verse.Chapter,
                        Verse = verse.Verse,
                        Contenido = verse.Contenido,
                        ChapterName = "",
                        ChapterNum = if (verse.Verse == 1) verse.Chapter else null
                    )
                )
            }
        }

        /*for (verse in verses) {
            var headerTitle: String? = null

            // Buscar si hay un header para este versículo
            for (header in headers) {
                if (header.BookId == verse.Book &&
                    header.ChapteId == verse.Chapter &&
                    header.VerseId == verse.Verse) {
                    headerTitle = header.Titulo
                    break
                }
            }

            // CLAVE: Poblar ChapterNum con el valor de Chapter
            versesResponse.add(
                Verses(
                    Number = verse.Number,
                    Book = verse.Book,
                    Chapter = verse.Chapter,
                    Verse = verse.Verse,
                    Contenido = verse.Contenido,
                    ChapterName = headerTitle ?: "",
                    ChapterNum = verse.Chapter // ← ESTO ES IMPORTANTE
                )
            )
        }*/

        for (book in books){
            booksResponse.add(
                Books(
                    Number = book.Number,
                    Osis = book.Osis,
                    Human = book.Human,
                    Chapters = book.Chapters,
                    Verse = versesResponse.filter { v -> v.Book == book.Osis }.toMutableList()
                )
            )
        }

        return booksResponse
    }
}