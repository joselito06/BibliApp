package com.jbncode.bibliaapp.core.deeplink

import android.net.Uri

/** Enlaces del tipo https://joselito06.github.io/v/{versión}/{libro}/{capítulo}/{versículo} */
object DeepLinks {
    const val HOST = "joselito06.github.io"

    data class VerseLink(val version: String, val book: String, val chapter: Int, val verse: Int)

    fun parse(uri: Uri?): VerseLink? {
        if (uri == null || !uri.host.equals(HOST, ignoreCase = true)) return null
        val seg = uri.pathSegments
        if (seg.size < 4 || seg[0] != "v") return null
        return VerseLink(
            version = seg[1].lowercase(),
            book = seg[2],
            chapter = seg[3].toIntOrNull() ?: return null,
            verse = seg.getOrNull(4)?.toIntOrNull() ?: 1
        )
    }
}
