package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.example.bibliaapp.R
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.SettingsViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSelectionScreen(viewModel: SettingsViewmodel, mainViewmodel: MainViewmodel, navigation: NavHostController) {
    val view = LocalView.current

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
    val bibleVersions = listOf("Reina-Valera 1960", "Nueva Versión Internacional", "Biblia de Jerusalén", "Dios Habla Hoy")
    val selectedBible by viewModel.selectedBible.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = scrollBehavior.state
    val isExpanded = scrollState.collapsedFraction < 0.05f
    val topAppBarShape = if (isExpanded) {
        RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp) // Solo esquinas inferiores redondeadas
    } else {
        RectangleShape // Forma rectangular normal cuando está colapsada o en transición
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        titleText = "Versiones de la Biblia"
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
    )
    { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp), // Padding general para el contenido
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre elementos principales
        ) {

            // 1. CARD PRINCIPAL: BIBLIA SELECCIONADA
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min), // Para que la card se ajuste al contenido
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.bible_in_use_title), // "Biblia en Uso"
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Book, // O un icono más específico
                        contentDescription = stringResource(R.string.bible_in_use_title), // Descripción para el icono
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedBible,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. OPCIÓN PARA DESCARGAR MÁS BIBLIAS
            Button(
                onClick = {
                    // TODO: Lógica para navegar a la pantalla de descarga
                    // o mostrar un diálogo/modal de selección de nuevas Biblias.
                    // Ejemplo: navController.navigate("downloadBiblesScreen")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Descarga", // "Descargar"
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.download_more_bibles_button)) // "Descargar más Biblias"
            }

            // 3. LISTA DE BIBLIAS DISPONIBLES (Para cambiar la selección actual)
            Text(
                text = stringResource(R.string.change_to_other_available_bible), // "Cambiar a otra Biblia disponible"
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp) // Un poco de espacio antes de la lista
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                // Si la lista es muy larga y quieres que ocupe el resto del espacio:
                // .weight(1f) // Esto requeriría que el Column padre tenga una altura definida o que no esté en un contexto de scroll infinito.
                // Por ahora, sin weight para que se ajuste al contenido.
            ) {
                items(bibleVersions) { version ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (version == selectedBible),
                                onClick = { viewModel.setSelectedBible(version) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (version == selectedBible),
                            onClick = { viewModel.setSelectedBible(version) }
                        )
                        Text(
                            text = version,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    if (version != bibleVersions.last()) { // No añadir Divider después del último ítem
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
