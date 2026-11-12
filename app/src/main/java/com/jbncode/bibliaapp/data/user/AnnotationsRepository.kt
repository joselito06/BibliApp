package com.jbncode.bibliaapp.data.user

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationsRepository @Inject constructor(private val dao: UserItemDao) {

    fun chapter(book: String, chapter: Int) = dao.observeChapter(book, chapter)
    fun byType(type: String) = dao.observeByType(type)

    /** Si TODOS los versículos ya son favoritos los quita; si no, marca los que faltan. */
    suspend fun toggleFavorite(book: String, chapter: Int, verses: Collection<Int>) {
        val allFav = verses.all { dao.find(ItemType.FAVORITE, book, chapter, it) != null }
        verses.forEach { v ->
            if (allFav) dao.deleteAt(ItemType.FAVORITE, book, chapter, v)
            else if (dao.find(ItemType.FAVORITE, book, chapter, v) == null) dao.upsert(item(ItemType.FAVORITE, book, chapter, v))
        }
    }

    /** color = null quita el resaltado. */
    suspend fun setHighlight(book: String, chapter: Int, verses: Collection<Int>, color: Int?) {
        verses.forEach { v ->
            dao.deleteAt(ItemType.HIGHLIGHT, book, chapter, v)
            if (color != null) dao.upsert(item(ItemType.HIGHLIGHT, book, chapter, v).copy(color = color))
        }
    }

    suspend fun noteAt(book: String, chapter: Int, verse: Int) = dao.findNote(book, chapter, verse)

    suspend fun saveNote(book: String, chapter: Int, from: Int, to: Int, text: String, existing: UserItemEntity?) {
        if (text.isBlank()) { existing?.let { dao.delete(it.id) }; return }
        val now = System.currentTimeMillis()
        dao.upsert(
            UserItemEntity(
                id = existing?.id ?: 0, type = ItemType.NOTE, book = book, chapter = chapter,
                verse = from, verseEnd = to, text = text.trim(),
                createdAt = existing?.createdAt ?: now, updatedAt = now
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun item(type: String, book: String, chapter: Int, verse: Int): UserItemEntity {
        val now = System.currentTimeMillis()
        return UserItemEntity(type = type, book = book, chapter = chapter, verse = verse, createdAt = now, updatedAt = now)
    }
}
