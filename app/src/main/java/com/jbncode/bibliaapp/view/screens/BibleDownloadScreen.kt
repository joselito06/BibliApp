package com.jbncode.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import com.jbncode.bibliaapp.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.utils.RewardedDownloadAdManager
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.BibleManagerViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel

@Composable
fun BibleDownloadScreen(
    viewModel: BibleManagerViewmodel = hiltViewModel(),
    navController: NavHostController,
    mainViewmodel: MainViewmodel? = null
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    val context = LocalContext.current
    val bibles by viewModel.filteredBibleList.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguageFilter.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val selectedDbName by viewModel.sharedPreferenceBible.getSelectedDatabaseFileNameFlow()
        .collectAsState(initial = viewModel.sharedPreferenceBible.getSelectedDatabaseFileName())

    var languageFilterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewmodel?.showTopAppBar(false)
        mainViewmodel?.showBottomBar(false)
        RewardedDownloadAdManager.preloadAd(context)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            if (ev.msg == BibleManagerViewmodel.DownloadMsg.STARTED) return@collect   // el progreso ya se ve en la tarjeta
            val title = ev.bible?.title.orEmpty()
            val id = when (ev.msg) {
                BibleManagerViewmodel.DownloadMsg.STARTED -> R.string.dl_msg_started
                BibleManagerViewmodel.DownloadMsg.DONE -> R.string.dl_msg_done
                BibleManagerViewmodel.DownloadMsg.NO_INTERNET -> R.string.dl_msg_no_internet
                BibleManagerViewmodel.DownloadMsg.TIMEOUT -> R.string.dl_msg_timeout
                BibleManagerViewmodel.DownloadMsg.SERVER -> R.string.dl_msg_server
                BibleManagerViewmodel.DownloadMsg.STORAGE -> R.string.dl_msg_storage
                BibleManagerViewmodel.DownloadMsg.INVALID -> R.string.dl_msg_invalid
                BibleManagerViewmodel.DownloadMsg.FAILED -> R.string.dl_msg_failed
                BibleManagerViewmodel.DownloadMsg.IN_PROGRESS -> R.string.dl_msg_in_progress
            }
            val canRetry = ev.bible != null && ev.msg !in listOf(
                BibleManagerViewmodel.DownloadMsg.STARTED, BibleManagerViewmodel.DownloadMsg.DONE, BibleManagerViewmodel.DownloadMsg.IN_PROGRESS
            )
            snackbar.currentSnackbarData?.dismiss()
            scope.launch {
            val res = snackbar.showSnackbar(
                message = context.getString(id, title),
                actionLabel = if (canRetry) context.getString(R.string.dl_retry) else null,
                duration = if (canRetry) androidx.compose.material3.SnackbarDuration.Long else androidx.compose.material3.SnackbarDuration.Short
            )
            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) ev.bible?.let { viewModel.startDownload(it) }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.bible_selection_download_bibles),
                icon = Icons.Filled.CloudDownload,
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.bibles_download_filter_label), style = MaterialTheme.typography.titleSmall)
                Box {
                    FilledTonalButton(
                        onClick = { languageFilterExpanded = true },
                        modifier = Modifier.defaultMinSize(minWidth = 120.dp)
                    ) {
                        Text(selectedLanguage ?: stringResource(R.string.bibles_download_filter_all))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = languageFilterExpanded, onDismissRequest = { languageFilterExpanded = false }) {
                        availableLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(if (language == "Todos") stringResource(R.string.bibles_download_filter_all) else language) },
                                onClick = {
                                    viewModel.setLanguageFilter(if (language == "Todos") null else language)
                                    languageFilterExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (bibles.isEmpty() && selectedLanguage != "Todos" && selectedLanguage != null) {
                Text(
                    stringResource(R.string.bibles_download_empty_list, selectedLanguage ?: ""),
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bibles, key = { it.id }) { bible ->
                        val isDefaultAssetBible = bible.database_name == "tla.sqlite3"
                        val isActive = (selectedDbName == bible.database_name) ||
                            (selectedDbName == null && isDefaultAssetBible)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            BibleListItem(
                                bibleInfo = bible,
                                isCurrentlyActive = isActive,
                                onDownloadClick = {
                                    RewardedDownloadAdManager.showAdThenDownload(context) { viewModel.startDownload(bible) }
                                },
                                onActivateClick = { viewModel.activateDownloadedDatabase(bible.database_name) },
                                onSetAsDefaultClick = if (isActive && !isDefaultAssetBible) {
                                    { viewModel.activateDefaultDatabase() }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BibleListItem(
    bibleInfo: BibleVersionInfo,
    isCurrentlyActive: Boolean,
    onDownloadClick: () -> Unit,
    onActivateClick: () -> Unit,
    onSetAsDefaultClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Book,
            contentDescription = "Icono de Biblia",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bibleInfo.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.bibles_download_code_label,bibleInfo.code.uppercase()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Podrías añadir el idioma aquí si quieres que sea visible en el ítem,
            // aunque ya está siendo usado para filtrar.
            // Text(
            // text = "Idioma: ${bibleInfo.lang}",
            // style = MaterialTheme.typography.caption,
            // color = MaterialTheme.colorScheme.outline
            // )
            if (bibleInfo.description.isNotBlank()) {
                Text(
                    text = bibleInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (bibleInfo.isDownloading) {
                LinearProgressIndicator(
                    progress = { bibleInfo.downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                Text(
                    stringResource(R.string.bibles_download_status_downloading,bibleInfo.downloadProgress),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        when {
            bibleInfo.isDownloading -> {
                Button(
                    onClick = { /* Considerar acción de cancelar */ },
                    enabled = false,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                ) {
                    Text(stringResource(R.string.bibles_download_button_downloading))
                }
            }
            bibleInfo.isDownloaded -> {
                if (isCurrentlyActive) {
                    if (onSetAsDefaultClick != null && bibleInfo.database_name != "tla.sqlite3" /* Ajusta a tu BD de assets */) {
                        OutlinedButton(
                            onClick = onSetAsDefaultClick,
                            modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                        ) {
                            Text(stringResource(R.string.bibles_download_button_use_default))
                        }
                    } else {
                        Text(
                            stringResource(R.string.bibles_download_active_label),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onActivateClick,
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text(stringResource(R.string.bibles_download_button_activate))
                    }
                }
            }
            else -> { // No descargada
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                ) {
                    Text(stringResource(R.string.bibles_download_button_download))
                }
            }
        }
    }
}