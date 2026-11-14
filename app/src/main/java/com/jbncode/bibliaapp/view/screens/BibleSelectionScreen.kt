package com.jbncode.bibliaapp.view.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CollectionsBookmark
import com.jbncode.bibliaapp.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.BibleDisplayInfo
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.BibleManagerViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSelectionScreen(
    viewModel: SettingsViewmodel,
    mainViewmodel: MainViewmodel,
    navigation: NavHostController,
    bibleManagerViewmodel: BibleManagerViewmodel = hiltViewModel()
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    val context = LocalContext.current
    val activity = context as? Activity

    val activeBibleInfo by viewModel.activeBibleDetails.collectAsState()
    val downloadedBibles by viewModel.downloadedBibles.collectAsState()

    // Archivo activo observado como Flow: al activar otra Biblia, la marca se mueve al instante.
    val selectedFile by viewModel.sharedPreferenceBible.getSelectedDatabaseFileNameFlow()
        .collectAsState(initial = viewModel.sharedPreferenceBible.getSelectedDatabaseFileName())
    val activeIdentifier = selectedFile ?: viewModel.defaultAssetDbIdentifier

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) { delay(1500); isRefreshing = false }
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
        viewModel.refreshBibleData()
        InterstitialAdManager.load(context)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.bible_selection_screen_title),
                icon = Icons.Default.CollectionsBookmark,
                onBack = { navigation.popBackStack() }
            )
        },
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    viewModel.refreshBibleData()
                }
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { CurrentBibleInUseCard(activeBibleInfo) }

                item {
                    FilledTonalButton(
                        onClick = {
                            val go = { navigation.navigate(Routes.BibleDownloadScreen.route) }
                            if (activity != null) InterstitialAdManager.show(activity) { go() } else go()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.bible_selection_download_bibles), fontWeight = FontWeight.Medium)
                    }
                }

                if (downloadedBibles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.bible_selection_subtitle_active),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    items(downloadedBibles, key = { it.storageIdentifier }) { bibleInfo ->
                        DownloadedBibleItem(
                            bibleInfo = bibleInfo,
                            isSelected = activeIdentifier == bibleInfo.storageIdentifier,
                            onBibleSelected = { viewModel.setSelectedBible(bibleInfo) }
                        )
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.bible_selection_empty_downloads),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentBibleInUseCard(bible: BibleVersionInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (bible == null || bible.database_name.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.bible_selection_loading_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getFlagEmoji(bible.country),
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bible.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${bible.code.uppercase()} · ${bible.lang}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdded,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (bible.description.isNotEmpty()) {
                    HorizontalDivider(
                        Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Text(
                        text = bible.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        maxLines = 4, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedBibleItem(
    bibleInfo: BibleDisplayInfo,
    isSelected: Boolean,
    onBibleSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBibleSelected, enabled = !isSelected),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Text(
                text = getFlagEmoji(bibleInfo.country),
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = bibleInfo.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bibleInfo.lang,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper para obtener el emoji de la bandera (simplificado)
fun getFlagEmoji(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "MEX" -> "🇲🇽"
        "NA" -> "🇺🇸"
        "USA" -> "🇺🇸"
        "FRA" -> "🇫🇷"
        "ROU" -> "🇷🇴"
        "BRA" -> "🇧🇷"
        "PRT" -> "🇵🇹"
        "ESP" -> "🇪🇸"
        "GBR", "ENG" -> "🇬🇧"
        "ITA" -> "🇮🇹"
        else -> "🏳️"
    }
}
