package com.jbncode.bibliaapp.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jbncode.bibliaapp.ConfigTopAppBar
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.screens.getThemeDisplayName

/** Barra superior de Inicio y Más (el lector y la búsqueda tienen su propio encabezado). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarComponent(
    navigation: NavHostController,
    canPop: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    optionTopAppBar: ConfigTopAppBar,
    currentTheme: AppThemeType,
    onThemeChange: (AppThemeType) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        navigationIcon = {
            if (canPop) {
                IconButton(onClick = { navigation.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo), contentDescription = null,
                    modifier = Modifier.padding(start = 12.dp).size(40.dp), contentScale = ContentScale.Fit
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            )
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.Palette, contentDescription = "Change Theme") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                AppThemeType.entries.forEach { themeType ->
                    DropdownMenuItem(
                        trailingIcon = { if (currentTheme == themeType) Icon(Icons.Default.Check, contentDescription = null) },
                        text = { Text(getThemeDisplayName(themeType)) },
                        onClick = { onThemeChange(themeType); menuExpanded = false }
                    )
                }
            }
            if (optionTopAppBar.Actions.isEmpty()) {
                IconButton(onClick = { navigation.navigate(Routes.ConfigScreen.route) }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            } else {
                optionTopAppBar.Actions.forEach { item ->
                    IconButton(onClick = item.OnClickIconButton) { Icon(item.ImageIconVector, contentDescription = item.Description) }
                }
            }
        }
    )
}

private class NavItem(val route: String, val label: Int, val icon: ImageVector)

/**
 * Barra inferior. La lectura en voz alta ya no depende de la pantalla (vive en un servicio),
 * así que cambiar de pestaña no la interrumpe y se quitó el diálogo de "detener lectura".
 */
@Composable
fun NavigationBottomJBN(navigation: NavHostController) {
    val route = navigation.currentBackStackEntryAsState().value?.destination?.route
    val items = listOf(
        NavItem(Routes.HomeScreen.route, R.string.navigarion_bar_home, Icons.Rounded.Home),
        NavItem(Routes.BibleScreen.route, R.string.navigarion_bar_bible, Icons.Rounded.Book),
        NavItem(Routes.SearchScreen.route, R.string.navigarion_bar_search, Icons.Rounded.Search),
        NavItem(Routes.MoreScreen.route, R.string.navigarion_bar_more, Icons.Rounded.Menu)
    )
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 12.dp
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            items.forEach { item ->
                val selected = route == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        // Una sola copia de cada pestaña en la pila (antes se apilaban al tocar varias veces).
                        navigation.navigate(item.route) {
                            popUpTo(Routes.BibleScreen.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(stringResource(item.label), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
