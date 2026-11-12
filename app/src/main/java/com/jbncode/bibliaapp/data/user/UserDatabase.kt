package com.jbncode.bibliaapp.data.user

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

object ItemType {
    const val NOTE = "NOTE"
    const val FAVORITE = "FAVORITE"
    const val HIGHLIGHT = "HIGHLIGHT"
}

/** Notas, favoritos y resaltados. Se guardan por REFERENCIA (no por versión ni texto). */
@Entity(tableName = "user_items", indices = [Index("type"), Index("book", "chapter", "verse")])
data class UserItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val verseEnd: Int = verse,
    val text: String? = null,
    val color: Int? = null,
    val tags: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Un registro por sesión de lectura. book="LEGACY"/chapter=0 son datos migrados del sistema anterior. */
@Entity(tableName = "reading_log", indices = [Index("day")])
data class ReadingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val book: String,
    val chapter: Int,
    val seconds: Int,
    val versesRead: Int
)

data class BookProgressRow(val book: String, val chapters: Int)

@Dao
interface UserItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: UserItemEntity): Long
    @Query("DELETE FROM user_items WHERE id = :id") suspend fun delete(id: Long)

    @Query("DELETE FROM user_items WHERE type = :type AND book = :book AND chapter = :chapter AND verse = :verse")
    suspend fun deleteAt(type: String, book: String, chapter: Int, verse: Int)

    @Query("SELECT * FROM user_items WHERE type = :type AND book = :book AND chapter = :chapter AND verse = :verse LIMIT 1")
    suspend fun find(type: String, book: String, chapter: Int, verse: Int): UserItemEntity?

    @Query("SELECT * FROM user_items WHERE type = 'NOTE' AND book = :book AND chapter = :chapter AND :v BETWEEN verse AND verseEnd LIMIT 1")
    suspend fun findNote(book: String, chapter: Int, v: Int): UserItemEntity?

    @Query("SELECT * FROM user_items WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<UserItemEntity>>

    @Query("SELECT * FROM user_items WHERE book = :book AND chapter = :chapter")
    fun observeChapter(book: String, chapter: Int): Flow<List<UserItemEntity>>

    @Query("SELECT * FROM user_items WHERE type = 'NOTE' AND text LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun searchNotes(q: String): Flow<List<UserItemEntity>>
}

@Dao
interface ReadingLogDao {
    @Insert suspend fun insert(entry: ReadingLogEntity)
    @Insert suspend fun insertAll(entries: List<ReadingLogEntity>)
    @Query("SELECT DISTINCT day FROM reading_log ORDER BY day DESC") fun observeDays(): Flow<List<String>>
    @Query("SELECT COALESCE(SUM(seconds),0) FROM reading_log") fun observeTotalSeconds(): Flow<Int>
    @Query("SELECT COALESCE(SUM(versesRead),0) FROM reading_log") fun observeTotalVerses(): Flow<Int>
    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT book, chapter FROM reading_log WHERE chapter > 0)")
    fun observeChaptersRead(): Flow<Int>
    @Query("SELECT book AS book, COUNT(DISTINCT chapter) AS chapters FROM reading_log WHERE chapter > 0 GROUP BY book")
    fun observeBookProgress(): Flow<List<BookProgressRow>>
    @Query("SELECT COALESCE(SUM(seconds),0) FROM reading_log WHERE day = :day") fun observeSecondsOn(day: String): Flow<Int>
}

@Database(entities = [UserItemEntity::class, ReadingLogEntity::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userItemDao(): UserItemDao
    abstract fun readingLogDao(): ReadingLogDao
}
