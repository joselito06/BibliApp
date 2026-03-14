package com.jbncode.bibliaapp.view.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel

@Composable
@Preview()
fun VersesScreenPreview(){
    //VersesScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersesScreen(
    modifier: Modifier,
    versesViewmodel: VersesViewmodel,
    mainViewmodel: MainViewmodel,
    navigation : NavHostController,
    scrollBehavior: TopAppBarScrollBehavior
){
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
    versesViewmodel.onGet()
    val list: List<VersesDaily> by versesViewmodel.getAllVerses.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val listState = rememberLazyListState() // Se mantiene por si se necesita para la LazyColu
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val scrollState = scrollBehavior.state
    // Consideramos "expandido" si la fracción colapsada es muy pequeña (cercana a 0)
    val isExpanded = scrollState.collapsedFraction < 0.05f // Umbral pequeño

    val topAppBarShape = if (isExpanded) {
        RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) // Solo esquinas inferiores redondeadas
    } else {
        RectangleShape // Forma rectangular normal cuando está colapsada o en transición
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center // Centra el contenido del Box
                    ){
                        AnimatedContent(
                            targetState = isSearchActive,
                            label = "SearchTitleAnimation"
                        ) { searchActive ->
                            if (searchActive){
                                TextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    placeholder = { Text(stringResource(R.string.verses_screen_search_placeholder)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .padding(end = 16.dp, bottom = 8.dp),
                                    singleLine = true,
                                    leadingIcon = { // <--- AÑADIR ESTO
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Buscar"
                                        )
                                    },
                                    shape = RoundedCornerShape(percent = 50), // Forma de pastilla
                                    colors = TextFieldDefaults.colors(
                                        // Fondo sutil para el TextField
                                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        // Indicadores transparentes ya que la forma define el campo
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        // Colores para texto, cursor, placeholder e icono
                                        cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f), // Placeholder más sutil
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        focusedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    textStyle = MaterialTheme.typography.titleLarge.copy( // Para que coincida con el tamaño del título
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }else{
                                Row(verticalAlignment = Alignment.CenterVertically) { // Row para icono y texto
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook, // Icono de ejemplo
                                        contentDescription = "Libro", // Descripción para accesibilidad
                                        modifier = Modifier.padding(end = 8.dp) // Espacio entre icono y texto
                                    )
                                    Text(stringResource(R.string.verses_screen_title))
                                }
                            }

                        }
                    }

                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchText = "" // Limpiar búsqueda al cerrar
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (isSearchActive) "Cerrar búsqueda" else "Activar búsqueda"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },

    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            val lists = list.dropLastWhile { it.verse_content?.isEmpty() == true && it.verse_direction?.isEmpty() == true }
            var filteredList: List<VersesDaily> = emptyList()
            filteredList = if (searchText.isBlank()) {
                list
            } else {
                lists.filter { item ->
                    item.verse_direction?.contains(searchText, ignoreCase = true) == true ||
                            item.verse_content?.contains(searchText, ignoreCase = true) == true
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                    //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    itemsIndexed(filteredList){ index, item ->
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)){
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                elevation = CardDefaults.elevatedCardElevation(5.dp)) {
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                ) {
                                    item.verse_direction?.let {
                                        Text(
                                            text = it,
                                            fontStyle = FontStyle.Normal,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.padding(10.dp))
                                    item.verse_content?.let { Text(text = it) }
                                    Row {
                                        IconButton(onClick = { /*TODO*/ }) {
                                            Icon(imageVector = Icons.Filled.Share, contentDescription = "Compartir")
                                        }
                                        IconButton(onClick = { /*TODO*/ }) {
                                            Icon(imageVector = Icons.Filled.CopyAll, contentDescription = "Copiar")
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

}