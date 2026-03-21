package com.jbncode.bibliaapp.view.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jbncode.bibliaapp.ConfigTopAppBar
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.screens.getThemeDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarComponent(
    navigation: NavHostController,
    canPop: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    optionTopAppBar: ConfigTopAppBar,
    currentTheme: AppThemeType,
    onThemeChange: (AppThemeType) -> Unit
){
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier
            //.offset(y = if (isTopBarVisible) 0.dp else -56.dp) // Oculta la TopAppBar al hacer scroll
            .animateContentSize(),
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),

        navigationIcon = {
            if (canPop) {
                IconButton(onClick = { navigation.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(45.dp),
                    contentScale = ContentScale.Fit,
                    //tint = MaterialTheme.colorScheme.primary
                )
                /*Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 8.dp)
                        .size(40.dp) // Tamaño del contenedor
                        .clip(CircleShape) // Corta en círculo
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) // Fondo suave
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ){
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit,
                        //tint = MaterialTheme.colorScheme.primary
                    )
                }*/
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            //Text(optionTopAppBar.Title)
        },
        actions = {
            // Icono para cambiar el tema
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change Theme"
                )
            }
            // Menú desplegable para seleccionar el tema
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                AppThemeType.entries.forEach { themeType ->
                    DropdownMenuItem(
                        trailingIcon = {
                            if (currentTheme == themeType) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        /*text = { Text(themeType.name.replace("_", " ").lowercase().capitalize(
                            Locale.current)) },*/
                        text = { Text(getThemeDisplayName(themeType)) },
                        onClick = {
                            onThemeChange(themeType)
                            menuExpanded = false
                        }
                    )
                }
            }
            if (optionTopAppBar.Actions.isEmpty()){
                IconButton(onClick = { navigation.navigate(Routes.ConfigScreen.route) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            } else {
                for (item in optionTopAppBar.Actions){
                    IconButton(onClick = item.OnClickIconButton) {
                        Icon(
                            imageVector = item.ImageIconVector,
                            contentDescription = item.Description
                        )
                    }
                }
            }

        }

    )
}

@Composable
fun FloatingActionButtonComponent(){
    FloatingActionButton(
        onClick = {  },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(8.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Add")
    }
}

@Composable
fun NavigationBottomJBN(
    navigation: NavHostController,
    isBottomBarVisible: Boolean,
    isSpeaking: Boolean,
    onStopReading: () -> Unit
){

    val navBackStackEntry by navigation.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Estado para controlar el diálogo
    var showStopDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<String?>(null) }

    // Función auxiliar para manejar el clic
    val handleNavigation: (String) -> Unit = { route ->
        if (isSpeaking && currentRoute == Routes.BibleScreen.route) {
            pendingRoute = route
            showStopDialog = true
        } else {
            navigation.navigate(route)
        }
    }
    // Diálogo de advertencia
    if (showStopDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(stringResource(R.string.bible_navigation_stop_reading_title)) },
            text = { Text(stringResource(R.string.bible_navigation_stop_reading_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onStopReading() // Detenemos el TTS
                    showStopDialog = false
                    pendingRoute?.let { navigation.navigate(it) }
                }) {
                    Text(stringResource(R.string.bible_navigation_stop_reading_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showStopDialog = false }) {
                    Text(stringResource(R.string.bible_navigation_stop_reading_cancel))
                }
            }
        )
    }

    NavigationBar(
        Modifier
            //.offset(y = if (!isBottomBarVisible) 0.dp else 100.dp) // Oculta la BottomAppBar al hacer scroll
            .animateContentSize(),
        containerColor = MaterialTheme.colorScheme.onPrimary,
        //contentColor = MaterialTheme.colorScheme.primary
    ){
        NavigationBarItem(
            selected = currentRoute == Routes.HomeScreen.route,
            onClick = { handleNavigation(Routes.HomeScreen.route) },
            icon = { Icon( Icons.Filled.Home, null) },
            label = { Text(text = stringResource(R.string.navigarion_bar_home))}
        )
        NavigationBarItem(
            selected = currentRoute == Routes.BibleScreen.route,
            onClick = { navigation.navigate(Routes.BibleScreen.route)},
            icon = { Icon( Icons.Filled.Book, null) },
            label = { Text(text = stringResource(R.string.navigarion_bar_bible))}
        )
        NavigationBarItem(
            selected = false,
            onClick = { /*TODO*/ },
            icon = { Icon( Icons.Filled.Search, null) },
            label = { Text(text = stringResource(R.string.navigarion_bar_search))}
        )
        NavigationBarItem(
            selected = currentRoute == Routes.MoreScreen.route,
            onClick = { handleNavigation(Routes.MoreScreen.route)},
            icon = { Icon( Icons.Filled.Menu, null) },
            label = { Text(text = stringResource(R.string.navigarion_bar_more))}
        )
    }
}