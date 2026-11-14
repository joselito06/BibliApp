package com.jbncode.bibliaapp.ui.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import com.jbncode.bibliaapp.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.ui.feature.reader.ReaderViewModel
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    mainViewmodel: MainViewmodel,
    reader: ReaderViewModel,
    navigation: NavHostController,
    vm: SearchViewModel = hiltViewModel()
) {
    SystemBarsColor(MaterialTheme.colorScheme.background)
    LaunchedEffect(Unit) { mainViewmodel.showTopAppBar(false); mainViewmodel.showBottomBar(true) }

    val ui by vm.ui.collectAsState()
    val books by reader.books.collectAsState()
    val info by reader.info.collectAsState()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(ui.query, ui.scope, ui.exact, books, info.ntStart) { delay(300); vm.run(books, info.ntStart) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp).padding(top = 12.dp)) {
            TextField(
                value = ui.query, onValueChange = vm::setQuery, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            Row(Modifier.padding(top = 10.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(ui.scope == SearchScope.ALL, { vm.setScope(SearchScope.ALL) }, { Text(stringResource(R.string.bibles_download_filter_all)) })
                FilterChip(ui.scope == SearchScope.OT, { vm.setScope(SearchScope.OT) }, { Text(stringResource(R.string.reader_old_testament)) })
                FilterChip(ui.scope == SearchScope.NT, { vm.setScope(SearchScope.NT) }, { Text(stringResource(R.string.reader_new_testament)) })
                FilterChip(ui.exact, { vm.toggleExact() }, { Text(stringResource(R.string.search_exact)) })
            }
        }

        if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        when {
            ui.query.trim().length < 2 -> Hint(stringResource(R.string.search_type_more))
            ui.done && ui.hits.isEmpty() && !ui.loading -> Hint(stringResource(R.string.search_empty, ui.query.trim()))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(stringResource(R.string.search_results, ui.hits.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(ui.hits, key = { "${it.ref.book}-${it.ref.chapter}-${it.ref.verse}" }) { h ->
                    val name = books.firstOrNull { it.osis == h.ref.book }?.name ?: h.ref.book
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            reader.open(h.ref.book, h.ref.chapter)
                            if (!navigation.popBackStack(Routes.BibleScreen.route, false)) navigation.navigate(Routes.BibleScreen.route)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("$name ${h.ref.chapter}:${h.ref.verse}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primary)
                            Text(highlight(h.snippet, primary), Modifier.padding(top = 4.dp), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                // Anuncio discreto al final de los resultados (solo con resultados abundantes).
                if (ui.hits.size >= 8) item(key = "ad") { com.jbncode.bibliaapp.view.components.InlineNativeAd() }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** El índice marca la coincidencia con [corchetes]; aquí se muestra en negrita y color. */
private fun highlight(snippet: String, color: Color): AnnotatedString = buildAnnotatedString {
    var on = false
    snippet.forEach { ch ->
        when (ch) {
            '[' -> on = true
            ']' -> on = false
            else -> if (on) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(ch) } else append(ch)
        }
    }
}
