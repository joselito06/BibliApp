package com.example.bibliaapp.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bibliaapp.R
import com.example.bibliaapp.view.Routes
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.MoreViewmodel
import com.example.bibliaapp.view.viewmodel.VersesViewmodel

@Composable
fun MoreScreen(
    modifier: Modifier,
    moreViewmodel: MoreViewmodel,
    versesViewmodel: VersesViewmodel,
    mainViewmodel: MainViewmodel,
    navigation : NavHostController){

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(true)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.NotificationScreen.route) },
            headlineContent = { Text(stringResource(R.string.notifications_section_title)) },
            leadingContent = {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Localized description",
                )
            }
        )

        Divider()

        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.VersesScreen.route)  },
            headlineContent = { Text(stringResource(R.string.more_screen_daily_verse_title)) },
            leadingContent = {
                Icon(
                    Icons.Rounded.Bookmark,
                    contentDescription = "Localized description",
                )
            }
        )

        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.BibleSelectionScreen.route) },
            headlineContent = { Text(stringResource(R.string.more_screen_other_bibles_title)) },
            leadingContent = {
                Icon(
                    Icons.Rounded.CollectionsBookmark,
                    contentDescription = "Localized description",
                )
            }
        )

        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.AboutScreen.route) },
            headlineContent = { Text(stringResource(R.string.about_app_title)) },
            leadingContent = {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "Localized description",
                )
            }
        )

        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.ShareScreen.route) },
            headlineContent = { Text(stringResource(R.string.more_screen_share_app_title)) },
            leadingContent = {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Localized description",
                )
            }
        )

        ListItem(
            modifier = Modifier.clickable { navigation.navigate(Routes.ConfigScreen.route) },
            headlineContent = { Text(stringResource(R.string.more_screen_settings_title)) },
            leadingContent = {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Localized description",
                )
            }
        )

    }
}

@Preview()
@Composable
fun MoreScreenPreview(){
    //MoreScreen()
}