package com.jbncode.bibliaapp.ui.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.user.ItemType
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.ui.feature.reader.ReaderViewModel
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    mainViewmodel: MainViewmodel,
    reader: ReaderViewModel,
    navigation: NavHostController,
    vm: LibraryViewModel = hiltViewModel()
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    LaunchedEffect(Unit) { mainViewmodel.showTopAppBar(false); mainViewmodel.showBottomBar(false) }

    val types = listOf(ItemType.NOTE, ItemType.FAVORITE, ItemType.HIGHLIGHT)
    val labels = listOf(R.string.library_notes, R.string.library_favorites, R.string.library_highlights)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val entries by remember(tab) { vm.entries(types[tab]) }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.library_title),
                icon = Icons.Default.Bookmark,
                onBack = { navigation.popBackStack() }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                labels.forEachIndexed { i, res -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(res)) }) }
            }
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entries, key = { it.item.id }) { e ->
                        EntryCard(e, onOpen = {
                            reader.open(e.item.book, e.item.chapter)
                            if (!navigation.popBackStack(Routes.BibleScreen.route, false)) navigation.navigate(Routes.BibleScreen.route)
                        }, onDelete = { vm.delete(e.item.id) })
                    }
                    // Anuncio discreto al final de la lista (solo si hay varias entradas).
                    if (entries.size >= 5) item(key = "ad") { com.jbncode.bibliaapp.view.components.InlineNativeAd() }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(e: LibraryEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    val item = e.item
    val ref = "${e.bookName} ${item.chapter}:${item.verse}" + if (item.verseEnd > item.verse) "-${item.verseEnd}" else ""
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.color?.let { c -> Box(Modifier.size(12.dp).clip(CircleShape).background(Color(c))) ; androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp)) }
                Text(ref, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.note_delete)) }
            }
            Text(e.text, Modifier.padding(end = 12.dp), maxLines = 4, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Serif, style = MaterialTheme.typography.bodyLarge)
            if (!item.text.isNullOrBlank()) {
                Text(
                    item.text, Modifier.padding(top = 8.dp, end = 12.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer).padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
