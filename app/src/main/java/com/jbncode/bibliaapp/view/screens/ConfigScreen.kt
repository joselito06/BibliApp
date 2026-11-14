package com.jbncode.bibliaapp.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.ui.feature.reader.PlaybackViewModel
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    mainViewmodel: MainViewmodel,
    settingsViewmodel: SettingsViewmodel,
    playbackViewmodel: PlaybackViewModel,
    navigation: NavHostController
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)

    val currentAppTheme by settingsViewmodel.appTheme.collectAsState()
    val currentAppLanguage by settingsViewmodel.appLanguage.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.config_screen_title),
                icon = Icons.Default.Settings,
                onBack = { navigation.popBackStack() }
            )
        },
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingSectionTitle(stringResource(R.string.appearance_section_title))
            SettingsCard {
                SettingItem(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.theme_setting_title),
                    subtitle = getThemeDisplayName(currentAppTheme),
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(Modifier.padding(start = 56.dp))
                SettingItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.language_setting_title),
                    subtitle = stringResource(currentAppLanguage.displayNameResId),
                    onClick = { showLanguageDialog = true }
                )
            }


            Spacer(Modifier.height(24.dp))
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentAppTheme,
            onDismissRequest = { showThemeDialog = false },
            onThemeSelected = { newTheme ->
                settingsViewmodel.setAppTheme(newTheme)
                showThemeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentAppLanguage,
            onDismissRequest = { showLanguageDialog = false },
            onLanguageSelected = { newLanguage ->
                settingsViewmodel.setAppLanguage(newLanguage)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun getThemeDisplayName(theme: AppThemeType): String {
    return when (theme) {
        AppThemeType.LIGHT -> stringResource(R.string.theme_bibliapp_light)
        AppThemeType.DARK -> stringResource(R.string.theme_bibliapp_dark)
        AppThemeType.CLASSIC_LIGHT -> stringResource(R.string.theme_classic_light)
        AppThemeType.CLASSIC_DARK -> stringResource(R.string.theme_classic_dark)
        AppThemeType.SYSTEM_DEFAULT -> stringResource(R.string.theme_system)
        AppThemeType.HIGH_CONTRAST_LIGHT -> stringResource(R.string.theme_high_contrast_light)
        AppThemeType.HIGH_CONTRAST_DARK -> stringResource(R.string.theme_high_contrast_dark)
        AppThemeType.MEDIUM_CONTRAST_LIGHT -> stringResource(R.string.theme_medium_contrast_light)
        AppThemeType.MEDIUM_CONTRAST_DARK -> stringResource(R.string.theme_medium_contrast_dark)
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppThemeType,
    onDismissRequest: () -> Unit,
    onThemeSelected: (AppThemeType) -> Unit
) {
    var tempSelectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.theme_selection_title)) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                AppThemeType.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(
                                selected = (theme == tempSelectedTheme),
                                onClick = { tempSelectedTheme = theme },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (theme == tempSelectedTheme), onClick = null)
                        Text(
                            text = getThemeDisplayName(theme),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeSelected(tempSelectedTheme) }) {
                Text(stringResource(R.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onDismissRequest: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var tempSelectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.language_setting_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(
                                selected = (lang == tempSelectedLanguage),
                                onClick = { tempSelectedLanguage = lang },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (lang == tempSelectedLanguage), onClick = null)
                        Text(
                            text = stringResource(lang.displayNameResId),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onLanguageSelected(tempSelectedLanguage) }) {
                Text(stringResource(R.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
