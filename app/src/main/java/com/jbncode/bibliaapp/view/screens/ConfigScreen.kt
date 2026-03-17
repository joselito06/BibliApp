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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.BibleViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    mainViewmodel: MainViewmodel,
    settingsViewmodel: SettingsViewmodel,
    bibleViewmodel: BibleViewmodel,
    navigation: NavHostController
){
    val view = LocalView.current

    // --- Define los colores que quieres para esta pantalla ---
    val desiredStatusBarColor = MaterialTheme.colorScheme.primaryContainer // Ejemplo: Negro semi-transparente
    val desiredNavBarColor = MaterialTheme.colorScheme.primaryContainer    // Ejemplo: Un azul oscuro opaco

    val announceVerseNumbers by bibleViewmodel.announceVerseNumbers.collectAsState()
    val announceChapterTitles by bibleViewmodel.announceChapterTitles.collectAsState()

    if (!view.isInEditMode) {
        val window = view.context.findActivity()?.window
        DisposableEffect(key1 = window, key2 = desiredStatusBarColor, key3 = desiredNavBarColor) {
            window?.let { win ->
                val originalStatusBarColor = win.statusBarColor
                val originalNavBarColor = win.navigationBarColor
                val insetsController = WindowCompat.getInsetsController(win, view)
                val originalIsLightStatusBar = insetsController.isAppearanceLightStatusBars
                val originalIsLightNavBar = insetsController.isAppearanceLightNavigationBars

                // Establecer nuevos colores para las barras
                win.statusBarColor = desiredStatusBarColor.toArgb()
                win.navigationBarColor = desiredNavBarColor.toArgb()

                // Ajustar el color de los iconos de las barras para contraste
                // Si la barra es oscura, los iconos deben ser claros (isAppearanceLight... = false)
                // Si la barra es clara, los iconos deben ser oscuros (isAppearanceLight... = true)
                insetsController.isAppearanceLightStatusBars = !desiredStatusBarColor.isPerceivedAsDark()
                insetsController.isAppearanceLightNavigationBars = !desiredNavBarColor.isPerceivedAsDark()

                onDispose {
                    // Restaurar colores e iconos originales al salir de la pantalla
                    win.statusBarColor = originalStatusBarColor
                    win.navigationBarColor = originalNavBarColor
                    insetsController.isAppearanceLightStatusBars = originalIsLightStatusBar
                    insetsController.isAppearanceLightNavigationBars = originalIsLightNavBar
                }
            } ?: onDispose {} // Si window es null, onDispose no tiene nada que hacer
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = scrollBehavior.state
    val isExpanded = scrollState.collapsedFraction < 0.05f
    val topAppBarShape = if (isExpanded) {
        RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp) // Solo esquinas inferiores redondeadas
    } else {
        RectangleShape // Forma rectangular normal cuando está colapsada o en transición
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentAppTheme by settingsViewmodel.appTheme.collectAsState() // O el que prefieras por defecto
    val onThemeChange: (AppThemeType) -> Unit = { newTheme ->
        settingsViewmodel.setAppTheme(newTheme)
    }
    val currentAppLanguage by settingsViewmodel.appLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Configurar el callback en el ViewModel para la navegación
    LaunchedEffect(settingsViewmodel, navigation) {
        settingsViewmodel.onNavigateToRestartScreen = {
            navigation.navigate(Routes.RestartingScreen.route) { // Usa tu constante de ruta
                popUpTo(navigation.graph.startDestinationId) { inclusive = true } // Limpia backstack
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)

    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    TransformableTopAppBarTitle2(
                        scrollBehavior = scrollBehavior,
                        icon = Icons.Default.Settings,
                        iconContentDescription = "Config",
                        titleText = stringResource(R.string.config_screen_title)
                    )
                },
                modifier = Modifier.clip(topAppBarShape),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Color para título y TextField
                    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer, // Color de fondo al desplazar
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navigation.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                },

                actions = {

                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BannerAd(stringResource(R.string.banner_ad_id))
        }

        ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingSectionTitle(stringResource(R.string.appearance_section_title))
            SettingItem(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.theme_setting_title),
                subtitle = getThemeDisplayName(currentAppTheme),
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(
                icon = Icons.Filled.Language, // Necesitarás este icono, por ejemplo, Icons.Default.Language
                title = stringResource(R.string.language_setting_title),
                subtitle = stringResource(currentAppLanguage.displayNameResId),
                onClick = { showLanguageDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TTSSettingsSection(
                announceVerseNumbers = announceVerseNumbers,
                announceChapterTitles = announceChapterTitles,
                onAnnounceVerseNumbersChanged = { bibleViewmodel.setAnnounceVerseNumbers(it) },
                onAnnounceChapterTitlesChanged = { bibleViewmodel.setAnnounceChapterTitles(it) }
            )

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
                // No es necesario recrear la Activity aquí manualmente,
                // AppCompatDelegate lo hará al cambiar applicationLocales.
                showLanguageDialog = false
            }
        )
    }

}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Ajuste de padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp), // Tamaño estándar para iconos en listas
            tint = MaterialTheme.colorScheme.secondary // Color más suave para el icono
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge) // Estilo de texto principal
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        } else {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ir a $title",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getThemeDisplayName(theme: AppThemeType): String {
    return when (theme) {
        AppThemeType.LIGHT -> stringResource(R.string.theme_light)
        AppThemeType.DARK -> stringResource(R.string.theme_dark)
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
    val themes = AppThemeType.entries.toTypedArray()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.theme_selection_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                themes.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Altura estándar para items clickables
                            .selectable(
                                selected = (theme == tempSelectedTheme),
                                onClick = { tempSelectedTheme = theme },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == tempSelectedTheme),
                            onClick = null // null recommended for radio buttons in Row clickable
                        )
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
            TextButton(
                onClick = {
                    onThemeSelected(tempSelectedTheme)
                    onDismissRequest() // Cierra el diálogo después de confirmar
                }
            ) {
                Text(stringResource(R.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel_button))
            }
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
    val languages = AppLanguage.entries.toTypedArray()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.language_setting_title)) }, // Usar stringResource
        text = {
            Column(Modifier.selectableGroup()) {
                languages.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (lang == tempSelectedLanguage),
                                onClick = { tempSelectedLanguage = lang },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (lang == tempSelectedLanguage),
                            onClick = null
                        )
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
            TextButton(
                onClick = {
                    onLanguageSelected(tempSelectedLanguage)
                }
            ) {
                Text(stringResource(R.string.accept_button)) // Usar stringResource
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel_button)) // Usar stringResource
            }
        }
    )
}

// 4. COMPONENTE UI PARA CONFIGURACIONES TTS
@Composable
fun TTSSettingsSection(
    announceVerseNumbers: Boolean,
    announceChapterTitles: Boolean,
    onAnnounceVerseNumbersChanged: (Boolean) -> Unit,
    onAnnounceChapterTitlesChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tts_settings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnnounceVerseNumbersChanged(!announceVerseNumbers) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = announceVerseNumbers,
                    onCheckedChange = onAnnounceVerseNumbersChanged,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = stringResource(R.string.announce_verse_numbers),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnnounceChapterTitlesChanged(!announceChapterTitles) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = announceChapterTitles,
                    onCheckedChange = onAnnounceChapterTitlesChanged,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = stringResource(R.string.announce_chapter_titles),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


