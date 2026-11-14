package com.jbncode.bibliaapp.core.share

import com.jbncode.bibliaapp.domain.bible.Verse

/** Un solo lugar para el formato de enlaces. */
object ShareLinks {
    private const val BASE = "https://joselito06.github.io/v"
    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.jbncode.bibliaapp"

    fun verse(version: String, book: String, chapter: Int, verse: Int) =
        "$BASE/${version.lowercase()}/$book/$chapter/$verse"
}

object VerseTextFormatter {
    /** "16-18, 20" a partir de números sueltos. */
    private fun ranges(nums: List<Int>): String {
        val out = mutableListOf<String>()
        var start = nums.first(); var prev = start
        for (n in nums.drop(1) + Int.MAX_VALUE) {
            if (n == prev + 1) prev = n
            else { out += if (start == prev) "$start" else "$start-$prev"; start = n; prev = n }
        }
        return out.joinToString(", ")
    }

    fun format(bookName: String, chapter: Int, verses: List<Verse>, versionCode: String, footer: String? = null): String {
        val sorted = verses.sortedBy { it.verse }
        val ref = "$bookName $chapter:${ranges(sorted.map { it.verse })} (${versionCode.uppercase()})"
        return buildString {
            append("📖 ").append(ref).append("\n\n")
            sorted.forEach { append(it.verse).append(". ").append(it.text.trim()).append('\n') }
            if (footer != null) append('\n').append(footer)
        }.trimEnd()
    }
}
