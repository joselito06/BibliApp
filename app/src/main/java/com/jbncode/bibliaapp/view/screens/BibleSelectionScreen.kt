package com.jbncode.bibliaapp.view.screens

import android.app.Activity
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.data.networking.models.BibleDisplayInfo
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.viewmodel.BibleManagerViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel
import kotlinx.coroutines.delay
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.components.BannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSelectionScreen(
    viewModel: SettingsViewmodel,
    mainViewmodel: MainViewmodel,
    navigation: NavHostController,
    bibleManagerViewmodel: BibleManagerViewmodel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as? Activity

    // --- Define los colores que quieres para esta pantalla ---
    val desiredStatusBarColor = MaterialTheme.colorScheme.primaryContainer // Ejemplo: Negro semi-transparente
    val desiredNavBarColor = MaterialTheme.colorScheme.primaryContainer    // Ejemplo: Un azul oscuro opaco

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
    //val bibleVersions = listOf("Reina-Valera 1960", "Nueva Versión Internacional", "Biblia de Jerusalén", "Dios Habla Hoy")
    //val selectedBible by viewModel.selectedBible.collectAsState()
    // El nombre legible de la Biblia actualmente seleccionada/activa
    val currentSelectedBibleDisplayName by viewModel.selectedBibleDisplayName.collectAsState()
    val activeBibleInfo by viewModel.activeBibleDetails.collectAsState()
    //val selectedDbName by bibleManagerViewmodel.sharedPreferenceBible.getSelectedDatabaseFileNameFlow()
    //    .collectAsState(initial = viewModel.sharedPreferenceBible.getSelectedDatabaseFileName())

    // La lista de Biblias que el usuario ha descargado (incluyendo la de assets como opción)
    val downloadedBibles by viewModel.downloadedBibles.collectAsState()

    // Estados para Pull to Refresh
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = scrollBehavior.state
    val isExpanded = scrollState.collapsedFraction < 0.05f
    val topAppBarShape = if (isExpanded) {
        RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp) // Solo esquinas inferiores redondeadas
    } else {
        RectangleShape // Forma rectangular normal cuando está colapsada o en transición
    }

    // Función de refresh
    val onRefresh: suspend () -> Unit = {
        isRefreshing = true
        try {
            // Llamar a las funciones de recarga del ViewModel
            viewModel.refreshBibleData() // Necesitas implementar esta función

            // Opcional: agregar un pequeño delay para mejor UX
            kotlinx.coroutines.delay(500)
        } catch (e: Exception) {
            Log.e("BibleSelection", "Error during refresh", e)
        } finally {
            isRefreshing = false
        }
    }

    // Configurar el callback en el ViewModel para la navegación
    LaunchedEffect(viewModel, navigation) {
        viewModel.onNavigateToRestartScreen = {
            navigation.navigate(Routes.RestartingScreen.route) { // Usa tu constante de ruta
                popUpTo(navigation.graph.startDestinationId) { inclusive = true } // Limpia backstack
                launchSingleTop = true
            }
        }
    }

    // SOLUCIÓN SIMPLE: Timeout automático para el refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Timeout máximo de 3 segundos
            delay(1500)
            isRefreshing = false
        }
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
        InterstitialAdManager.load(context)
    }

    Scaffold(
        //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
                title = {
                    TransformableTopAppBarTitle2(
                        scrollBehavior = scrollBehavior,
                        icon = Icons.Default.CollectionsBookmark,
                        iconContentDescription = "VersionesDeLaBiblia",
                        titleText = stringResource(R.string.bible_selection_screen_title),
                    )

                },
                actions = {
                    IconButton(onClick = { /* Acción adicional */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BannerAd(stringResource(R.string.banner_ad_id))
        }

    )
    { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if(!isRefreshing){
                    isRefreshing = true
                    viewModel.refreshBibleData()
                }
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Un poco menos de espacio entre items
            ) {
                // SECCIÓN 1: BIBLIA EN USO (Simplificada)
                item {
                    CurrentBibleInUseCard(activeBibleInfo)
                }

                // SECCIÓN 2: ACCIÓN PARA IR A DESCARGAR (si no hay muchas descargadas o como un recordatorio)
                item {
                    ButtonToDownloadScreen {
                        if(activity != null){
                            InterstitialAdManager.show(activity){
                                navigation.navigate(Routes.BibleDownloadScreen.route) // Navega a la pantalla de descarga
                            }
                        }else{
                            navigation.navigate(Routes.BibleDownloadScreen.route) // Navega a la pantalla de descarga
                        }

                    }
                }

                // SECCIÓN 3: LISTA DE BIBLIAS DESCARGADAS PARA ACTIVAR
                if (downloadedBibles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.bible_selection_subtitle_active), // "Activar otra Biblia descargada"
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(downloadedBibles, key = { it.storageIdentifier }) { bibleInfo ->
                        DownloadedBibleItem(
                            bibleInfo = bibleInfo,
                            // La Biblia activa es aquella cuyo identificador guardado en SharedPreferences
                            // coincide con el identificador de esta biblia en la lista.
                            // O si el identificador guardado es null Y esta biblia es la de assets.
                            isSelected = (viewModel.sharedPreferenceBible.getSelectedDatabaseFileName() ?: viewModel.defaultAssetDbIdentifier) == bibleInfo.storageIdentifier,
                            onBibleSelected = {
                                if(activity != null){
                                    InterstitialAdManager.show(activity){
                                        Log.d("BibleSelection", "Intentando activar: ${bibleInfo.displayName}")
                                        viewModel.setSelectedBible(bibleInfo)
                                    }
                                }else{
                                    Log.d("BibleSelection", "Intentando activar: ${bibleInfo.displayName}")
                                    viewModel.setSelectedBible(bibleInfo)
                                }

                                // La navegación a RestartingScreen se maneja desde el ViewModel
                            }
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
private fun CurrentBibleInUseCard(selectedBibleName: BibleVersionInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        if (selectedBibleName == null || selectedBibleName.database_name.isBlank()) { // Estado de carga o error
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedBibleName == null) stringResource(R.string.bible_selection_loading_details) else stringResource(R.string.bible_selection_info_not_available),
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
                // Fila para Bandera, Título y Código
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bandera (Placeholder - necesitas una lógica para mostrar la bandera)
                    Text(
                        text = getFlagEmoji(selectedBibleName.country), // Función para obtener emoji de bandera
                        fontSize = 28.sp, // Tamaño grande para la bandera
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedBibleName.title,
                            style = MaterialTheme.typography.titleLarge, // Más prominente
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row {
                            Text(
                                text = "(${selectedBibleName.code})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = selectedBibleName.lang,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Icono de "Activa"
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdded, // O CheckCircle
                        contentDescription = "Biblia activa",
                        tint = MaterialTheme.colorScheme.primary, // Usar el color primario
                        modifier = Modifier.size(36.dp) // Un poco más grande
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.bible_selection_description_label), // "Descripción:"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedBibleName.description.ifEmpty { stringResource(R.string.bible_selection_description_not_available)},
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 4, // Limitar para que no sea demasiado larga
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ButtonToDownloadScreen(onNavigate: () -> Unit) {
    FilledTonalButton( // O Button, OutlinedButton según preferencia
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(R.string.bible_selection_download_bibles), fontWeight = FontWeight.Medium) // "Descubrir y descargar Biblias"
    }
}

@Composable
private fun DownloadedBibleItem(
    bibleInfo: BibleDisplayInfo,
    isSelected: Boolean,
    onBibleSelected: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onBibleSelected, enabled = !isSelected), // Deshabilitar click si ya está seleccionada
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                // Podrías tener un icono diferente si es la Biblia de assets vs una descargada
                imageVector = if (bibleInfo.storageIdentifier == "tla.sqlite3") Icons.Filled.Book else Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = bibleInfo.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary, // Siempre primario para el check
                    modifier = Modifier.size(24.dp)
                )
            }
            // Opcional: mostrar un RadioButtonUnchecked si no está seleccionada
            // else {
            //     Icon(
            //         imageVector = Icons.Filled.RadioButtonUnchecked,
            //         contentDescription = stringResource(R.string.not_selected_description),
            //         tint = MaterialTheme.colorScheme.outline,
            //         modifier = Modifier.size(24.dp)
            //     )
            // }
        }
    }
}

// Helper para obtener el emoji de la bandera (simplificado)
// Necesitarás una implementación más robusta o usar una librería si quieres muchas banderas
fun getFlagEmoji(countryCode: String): String {
    return when (countryCode.uppercase()) {
        // Códigos presentes en tu JSON
        "MEX" -> "🇲🇽" // México
        "NA"  -> "🇺🇸" // North America (Usado para biblias en inglés)
        "USA" -> "🇺🇸" // Estados Unidos

        // Códigos para los nuevos idiomas que estás agregando
        "FRA" -> "🇫🇷" // Francia (Francés)
        "ROU" -> "🇷🇴" // Rumania (Rumano)
        "BRA" -> "🇧🇷" // Brasil (Portugués - muy común en biblias)
        "PRT" -> "🇵🇹" // Portugal (Portugués)

        // Otros códigos comunes
        "ESP" -> "🇪🇸" // España
        "GBR", "ENG" -> "🇬🇧" // Reino Unido / Inglés
        "ITA" -> "🇮🇹" // Italia
        else -> "🏳️" // Bandera blanca por defecto
    }
}

// --- Recursos de String que necesitarías en strings.xml (algunos nuevos/modificados) ---
/*
<string name="select_active_bible_title">Seleccionar Biblia Activa</string>
<string name="currently_active_bible">Biblia activa actualmente:</string>
<string name="discover_and_download_bibles">Descubrir y descargar Biblias</string>
<string name="activate_another_downloaded_bible">Activar otra Biblia descargada</string>
<string name="no_bibles_downloaded_yet">Aún no has descargado otras versiones de la Biblia.</string>
<string name="back_button_desc">Atrás</string>
<string name="selected_description">Seleccionado</string>
<string name="no_bible_selected">Por defecto</string> // O el nombre de tu Biblia por defecto
*/
