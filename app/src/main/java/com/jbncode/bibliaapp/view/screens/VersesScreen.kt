package com.jbncode.bibliaapp.view.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersesScreen(
    modifier: Modifier,
    versesViewmodel: VersesViewmodel,
    mainViewmodel: MainViewmodel,
    navigation: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)

    val context = LocalContext.current
    val list: List<VersesDaily> by versesViewmodel.getAllVerses.collectAsState()
    val today by versesViewmodel.verseOfTheDay.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
        versesViewmodel.onGet()   // recarga por si cambió la Biblia activa
    }
    LaunchedEffect(isSearchActive) { if (isSearchActive) focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.verses_screen_title),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onBack = { navigation.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchText = ""
                    }) {
                        Icon(if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { innerPadding ->
        val cleaned = list.filterNot { it.verse_content.isNullOrBlank() }
        val filtered = if (searchText.isBlank()) cleaned else cleaned.filter { item ->
            item.verse_direction?.contains(searchText, ignoreCase = true) == true ||
                item.verse_content?.contains(searchText, ignoreCase = true) == true
        }

        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (isSearchActive) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(stringResource(R.string.verses_screen_search_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(percent = 50),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (searchText.isBlank() && !today.verse_content.isNullOrBlank()) {
                item(key = "today") { TodayCard(today, context) }
            }
            items(filtered, key = { it.verse_id }) { item ->
                VerseCard(item, context)
            }
            }
        }
    }
}

@Composable
private fun TodayCard(verse: VersesDaily, context: Context) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                Text(
                    stringResource(R.string.verses_today_label),
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                verse.verse_content.orEmpty(),
                Modifier.padding(top = 12.dp),
                fontFamily = FontFamily.Serif, lineHeight = 26.sp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            verse.verse_direction?.let {
                Text(
                    it, Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Actions(verse, context, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun VerseCard(item: VersesDaily, context: Context) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            item.verse_direction?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item.verse_content?.let {
                Text(
                    it, Modifier.padding(top = 6.dp),
                    fontFamily = FontFamily.Serif, lineHeight = 24.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Actions(item, context, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Actions(item: VersesDaily, context: Context, tint: androidx.compose.ui.graphics.Color) {
    val copied = stringResource(R.string.bible_copy_toast)
    val copyLabel = stringResource(R.string.bible_copy_label)
    val shareTitle = stringResource(R.string.bible_share_title)
    Row(Modifier.padding(top = 4.dp)) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(copyLabel, verseText(item)))
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }) { Icon(Icons.Filled.CopyAll, contentDescription = copyLabel, Modifier.size(20.dp), tint = tint) }
        IconButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, verseText(item) + "\n\n" + context.getString(R.string.share_open_in_app) + " " + com.jbncode.bibliaapp.core.share.ShareLinks.PLAY_STORE)
            }
            context.startActivity(Intent.createChooser(send, shareTitle))
        }) { Icon(Icons.Filled.Share, contentDescription = shareTitle, Modifier.size(20.dp), tint = tint) }
    }
}

private fun verseText(item: VersesDaily): String =
    "📖 ${item.verse_content.orEmpty().trim()}\n\n— ${item.verse_direction.orEmpty()}".trim().trimEnd('—').trim()
