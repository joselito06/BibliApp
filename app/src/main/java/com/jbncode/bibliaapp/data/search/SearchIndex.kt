package com.jbncode.bibliaapp.data.search

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jbncode.bibliaapp.domain.bible.BibleRef
import com.jbncode.bibliaapp.domain.bible.SearchHit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Índice de búsqueda FTS4 por versión, guardado aparte ("idx_<archivo>").
 * No toca los .sqlite3 descargados; se reconstruye solo si el archivo cambia.
 */
@Singleton
class SearchIndex @Inject constructor(@ApplicationContext private val context: Context) {

    private fun idxFile(bible: String) = context.getDatabasePath("idx_$bible")

    suspend fun ensure(bible: String) = withContext(Dispatchers.IO) {
        val src = context.getDatabasePath(bible)
        val idx = idxFile(bible)
        val stamp = "${src.length()}-${src.lastModified()}"
        if (idx.exists() && storedStamp(idx) == stamp) return@withContext
        idx.delete()
        SQLiteDatabase.openOrCreateDatabase(idx, null).use { db ->
            db.execSQL(
                "CREATE VIRTUAL TABLE fts USING fts4(content, book, chapter, verse, " +
                    "notindexed=book, notindexed=chapter, notindexed=verse, tokenize=unicode61)"
            )
            db.execSQL("CREATE TABLE meta(stamp TEXT)")
            db.execSQL("ATTACH DATABASE ? AS src", arrayOf(src.path))
            db.execSQL(
                "INSERT INTO fts(content, book, chapter, verse) " +
                    "SELECT contenido, book, chapter, verse FROM src.verses WHERE contenido IS NOT NULL"
            )
            db.execSQL("DETACH DATABASE src")
            db.execSQL("INSERT INTO meta VALUES(?)", arrayOf(stamp))
        }
    }

    suspend fun search(
        bible: String,
        query: String,
        books: Set<String>? = null,   // filtro por libro/testamento
        exactPhrase: Boolean = false,
        limit: Int = 100
    ): List<SearchHit> = withContext(Dispatchers.IO) {
        val tokens = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@withContext emptyList()
        ensure(bible)

        val match = if (exactPhrase) "\"${tokens.joinToString(" ")}\"" else tokens.joinToString(" ") { "$it*" }
        val where = StringBuilder("fts MATCH ?")
        val args = mutableListOf(match)
        if (!books.isNullOrEmpty()) {
            where.append(" AND book IN (${books.joinToString(",") { "?" }})")
            args += books
        }
        val sql = "SELECT book, chapter, verse, snippet(fts, '[', ']', '…', 0, 14) " +
            "FROM fts WHERE $where LIMIT $limit"

        val hits = mutableListOf<SearchHit>()
        SQLiteDatabase.openDatabase(idxFile(bible).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(sql, args.toTypedArray()).use { c ->
                while (c.moveToNext()) {
                    hits += SearchHit(
                        BibleRef(c.getString(0), c.getString(1).toInt(), c.getString(2).toInt()),
                        c.getString(3)
                    )
                }
            }
        }
        hits
    }

    private fun storedStamp(idx: java.io.File): String? = try {
        SQLiteDatabase.openDatabase(idx.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT stamp FROM meta LIMIT 1", null).use { if (it.moveToFirst()) it.getString(0) else null }
        }
    } catch (e: Exception) { null }
}
