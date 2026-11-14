package com.jbncode.bibliaapp.ui.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.domain.bible.Book

/** Selector en dos pasos: testamento + libro, luego capítulo. Base de los filtros avanzados. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookChapterSheet(
    books: List<Book>,
    ntStart: Int,
    currentBook: String,
    onPick: (book: String, chapter: Int) -> Unit,
    onDismiss: () -> Unit,
    initialBookOsis: String? = null   // con un libro: abre directo en sus capítulos
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var chosen by remember(initialBookOsis, books) {
        mutableStateOf(books.firstOrNull { it.osis == initialBookOsis })
    }
    var nt by remember { mutableStateOf(books.firstOrNull { it.osis == currentBook }?.let { it.order >= ntStart } ?: false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            val b = chosen
            if (b == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !nt, onClick = { nt = false }, label = { Text(stringResource(R.string.reader_old_testament)) })
                    FilterChip(selected = nt, onClick = { nt = true }, label = { Text(stringResource(R.string.reader_new_testament)) })
                }
                FlowRow(Modifier.padding(top = 8.dp)) {
                    books.filter { (it.order >= ntStart) == nt }.forEach { bk ->
                        TextButton(onClick = { chosen = bk }) {
                            Text(bk.name, fontWeight = if (bk.osis == currentBook) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            } else {
                TextButton(onClick = { chosen = null }) { Text("← ${stringResource(R.string.onboarding_back)}") }
                Text(b.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                FlowRow {
                    (1..maxOf(b.chapters, 1)).forEach { c -> TextButton(onClick = { onPick(b.osis, c) }) { Text("$c") } }
                }
            }
        }
    }
}
