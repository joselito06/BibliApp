package com.jbncode.bibliaapp.data.bible

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import com.jbncode.bibliaapp.data.database.entities.BooksEntity
import com.jbncode.bibliaapp.data.database.entities.HeaderEntity
import com.jbncode.bibliaapp.data.database.entities.VersesEntity

data class VerseRow(
    @ColumnInfo(name = "book") val book: String?,
    @ColumnInfo(name = "chapter") val chapter: Int?,
    @ColumnInfo(name = "verse") val verse: Int?,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "heading") val heading: String?
)

@Dao
interface ReaderDao {
    @Query("SELECT * FROM books ORDER BY number")
    suspend fun books(): List<BooksEntity>

    @Query("SELECT chapter FROM verses WHERE book = :book GROUP BY chapter ORDER BY chapter")
    suspend fun chapters(book: String): List<Int>

    // El título se resuelve en SQL: elimina el doble for O(n*m) de BookUseCase.
    @Query(
        """SELECT v.book AS book, v.chapter AS chapter, v.verse AS verse, v.contenido AS text,
           (SELECT h.titulo FROM header h
             WHERE h.bookid = v.book AND h.chapterid = v.chapter AND h.verseid = v.verse
             LIMIT 1) AS heading
           FROM verses v WHERE v.book = :book AND v.chapter = :chapter ORDER BY v.verse"""
    )
    suspend fun chapter(book: String, chapter: Int): List<VerseRow>

    @Query(
        """SELECT v.book AS book, v.chapter AS chapter, v.verse AS verse, v.contenido AS text,
           NULL AS heading FROM verses v
           WHERE v.book = :book AND v.chapter = :chapter AND v.verse BETWEEN :from AND :to
           ORDER BY v.verse"""
    )
    suspend fun range(book: String, chapter: Int, from: Int, to: Int): List<VerseRow>
}

// Mismo esquema que ApplicationDbContext: abre los .sqlite3 ya descargados sin migrar.
// NUNCA usar fallbackToDestructiveMigration aquí: borraría la Biblia del usuario.
@Database(
    entities = [BooksEntity::class, HeaderEntity::class, VersesEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
