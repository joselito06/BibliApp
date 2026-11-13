package com.jbncode.bibliaapp.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MoreViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel

@Composable
fun MoreScreen(
    modifier: Modifier,
    moreViewmodel: MoreViewmodel,
    versesViewmodel: VersesViewmodel,
    mainViewmodel: MainViewmodel,
    navigation: NavHostController
) {
    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(true)
    }

    Scaffold(
        Modifier.fillMaxSize(),
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SectionLabel(stringResource(R.string.more_section_space))
            SectionCard {
                MoreItem(Icons.Rounded.EditNote, stringResource(R.string.library_title)) {
                    navigation.navigate(Routes.LibraryScreen.route)
                }
                HorizontalDivider(Modifier.padding(start = 68.dp))
                MoreItem(Icons.Default.AutoStories, stringResource(R.string.more_screen_daily_verse_title)) {
                    navigation.navigate(Routes.VersesScreen.route)
                }
            }

            SectionLabel(stringResource(R.string.more_section_bible))
            SectionCard {
                MoreItem(Icons.Default.CollectionsBookmark, stringResource(R.string.more_screen_other_bibles_title)) {
                    navigation.navigate(Routes.BibleSelectionScreen.route)
                }
                HorizontalDivider(Modifier.padding(start = 68.dp))
                MoreItem(Icons.Default.CloudDownload, stringResource(R.string.bible_selection_download_bibles)) {
                    navigation.navigate(Routes.BibleDownloadScreen.route)
                }
            }

            SectionLabel(stringResource(R.string.more_section_app))
            SectionCard {
                MoreItem(Icons.Default.Notifications, stringResource(R.string.notifications_section_title)) {
                    navigation.navigate(Routes.NotificationScreen.route)
                }
                HorizontalDivider(Modifier.padding(start = 68.dp))
                MoreItem(Icons.Rounded.Settings, stringResource(R.string.more_screen_settings_title)) {
                    navigation.navigate(Routes.ConfigScreen.route)
                }
                HorizontalDivider(Modifier.padding(start = 68.dp))
                MoreItem(Icons.Rounded.Share, stringResource(R.string.more_screen_share_app_title)) {
                    navigation.navigate(Routes.ShareScreen.route)
                }
                HorizontalDivider(Modifier.padding(start = 68.dp))
                MoreItem(Icons.Default.Info, stringResource(R.string.about_app_title)) {
                    navigation.navigate(Routes.AboutScreen.route)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column { content() }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
