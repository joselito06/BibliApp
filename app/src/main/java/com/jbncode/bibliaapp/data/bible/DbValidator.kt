package com.jbncode.bibliaapp.data.bible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

object DbValidator {
    /** Una Biblia es válida si abre, pasa quick_check y tiene las tablas books y verses. */
    fun isValid(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            SQLiteDatabase.openDatabase(
                file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { db ->
                val ok = db.rawQuery("PRAGMA quick_check(1)", null).use { it.moveToFirst() && it.getString(0) == "ok" }
                ok && db.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('books','verses')", null
                ).use { it.moveToFirst() && it.getInt(0) == 2 }
            }
        } catch (e: Exception) { false }
    }
}

/** Borra la copia "biblia_activa" del sistema anterior (liberaba decenas de MB por usuario). */
object LegacyCleanup {
    fun run(context: Context) {
        Thread {
            listOf("biblia_activa.sqlite3", "temp_biblia.sqlite3").forEach { context.deleteDatabase(it) }
        }.start()
    }
}
