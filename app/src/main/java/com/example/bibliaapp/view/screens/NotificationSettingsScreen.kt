package com.example.bibliaapp.view.screens


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.lerp as lerpTextStyle
import androidx.navigation.NavHostController
import com.example.bibliaapp.R
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.SettingsViewmodel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(viewModel: SettingsViewmodel, mainViewmodel: MainViewmodel, navigation: NavHostController) {
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
    val context = LocalContext
    val selectedTime by viewModel.selectedTime.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = false
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
                        icon = Icons.Default.Notifications,
                        iconContentDescription = "Notificaciones",
                        titleText = stringResource(R.string.notifications_section_title)
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
    ){ innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                        Text(text =
                            if (notificationsEnabled) stringResource(R.string.notifications_section_title)+ " " + stringResource(R.string.notifications_status_enabled)
                            else stringResource(R.string.notifications_section_title)+ " " + stringResource(R.string.notifications_status_disabled))
                    }

                }
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    modifier = Modifier.alpha(if (notificationsEnabled) 1f else 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        TimePicker(state = timePickerState)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setSelectedTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                                },
                                enabled = notificationsEnabled
                            ) {
                                Text(stringResource(R.string.accept_button))
                            }
                            Button(
                                onClick = {  },
                                enabled = notificationsEnabled
                            ) {
                                Text(stringResource(R.string.cancel_button))
                            }
                        }
                    }

                }

            }

        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedTopBarTitle(
    scrollBehavior: TopAppBarScrollBehavior,
    icon: ImageVector,
    iconContentDescription: String?,
    titleText: String,
    modifier: Modifier = Modifier
) {
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    // Estilos de texto para los estados expandido y colapsado
    // LargeTopAppBar usa aproximadamente headlineMedium expandido y titleLarge colapsado.
    val expandedTextStyle = MaterialTheme.typography.headlineMedium
    val collapsedTextStyle = MaterialTheme.typography.titleLarge

    // Umbral para cambiar el layout. Puedes ajustarlo.
    val layoutSwitchThreshold = 0.5f

    Crossfade(
        targetState = collapsedFraction > layoutSwitchThreshold,
        animationSpec = tween(durationMillis = 200), // Duración del fundido
        modifier = modifier
    ) { isLayoutCollapsed ->
        if (isLayoutCollapsed) {
            // Estado COLAPSADO: Icono a la izquierda del texto, alineado al inicio
            Row(
                modifier = Modifier.fillMaxWidth(), // Ocupa el ancho para alineación al inicio
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start // Alineación estándar de título colapsado
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp) // Tamaño estándar para icono en TopAppBar colapsada
                )
                Text(
                    text = titleText,
                    style = collapsedTextStyle
                )
            }
        } else {
            // Estado EXPANDIDO: Icono encima del texto, centrado
            Column(
                modifier = Modifier.fillMaxSize(), // Ocupa todo el espacio disponible en el slot del título
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier
                        .padding(bottom = 4.dp) // Espacio entre icono y texto
                        .size(36.dp) // Icono más grande para el estado expandido
                )
                Text(
                    text = titleText,
                    style = expandedTextStyle
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransformableTopAppBarTitle1(
    scrollBehavior: TopAppBarScrollBehavior,
    icon: ImageVector,
    iconContentDescription: String?,
    titleText: String,
    modifier: Modifier = Modifier
) {
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    // Define styles and sizes for expanded and collapsed states
    val expandedTextStyle = MaterialTheme.typography.headlineMedium
    val collapsedTextStyle = MaterialTheme.typography.titleLarge // Standard for collapsed LargeTopAppBar
    val expandedIconSizeDp = 36.dp
    val collapsedIconSizeDp = 24.dp // Standard for icons in a collapsed TopAppBar

    // Interpolate icon size and text style
    val currentIconSize =
        lerp(expandedIconSizeDp, collapsedIconSizeDp, collapsedFraction)
    // TextStyle lerp will smoothly animate font size, weight, letter spacing, etc.
    val currentTextStyle = lerpTextStyle(expandedTextStyle, collapsedTextStyle, collapsedFraction)

    Layout(
        content = {
            // Icon with animated size
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                modifier = Modifier.size(currentIconSize)
            )
            // Text with animated style
            Text(
                text = titleText,
                style = currentTextStyle,
                textAlign = TextAlign.Center,
                maxLines = 1, // Keep title to one line usually
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.fillMaxWidth() // Title area usually fills width
    ) { measurables, constraints ->
        // Measure icon and text with their currently interpolated sizes/styles
        val iconPlaceable = measurables[0].measure(constraints)
        val textPlaceable = measurables[1].measure(constraints)

        val availableWidth = constraints.maxWidth
        // constraints.maxHeight should be the current animated height of the title slot
        val availableHeight = constraints.maxHeight

        // Define target layout properties for EXPANDED state (icon above text, centered)
        val expandedIconX = (availableWidth - iconPlaceable.width) / 2
        // Try to center the (Icon + Padding + Text) block vertically
        val expandedContentHeight = iconPlaceable.height + 4.dp.toPx() + textPlaceable.height
        val expandedIconY = (availableHeight - expandedContentHeight) / 2
        val expandedTextX = (availableWidth - textPlaceable.width) / 2
        val expandedTextY = expandedIconY + iconPlaceable.height + 4.dp.toPx()

        // Define target layout properties for COLLAPSED state (icon left of text, start aligned, vertically centered)
        // Standard TopAppBar often has some start padding for its title content.
        // We'll place relative to our Layout's bounds. The LargeTopAppBar might add its own padding.
        val collapsedIconX = 0 // Align to start of our Layout area
        val collapsedIconY = (availableHeight - iconPlaceable.height) / 2
        val collapsedTextX = iconPlaceable.width + 8.dp.toPx() // 8.dp padding between icon and text
        val collapsedTextY = (availableHeight - textPlaceable.height) / 2

        // Interpolate current positions based on collapsedFraction
        val currentIconX = lerp(expandedIconX.toFloat(), collapsedIconX.toFloat(), collapsedFraction).toInt()
        val currentIconY = lerp(expandedIconY.toFloat(), collapsedIconY.toFloat(), collapsedFraction).toInt()
        val currentTextX = lerp(expandedTextX.toFloat(), collapsedTextX.toFloat(), collapsedFraction).toInt()
        val currentTextY = lerp(expandedTextY.toFloat(), collapsedTextY.toFloat(), collapsedFraction).toInt()

        // The overall layout dimensions will be based on available space
        layout(availableWidth, availableHeight) {
            iconPlaceable.placeRelative(currentIconX, currentIconY)
            textPlaceable.placeRelative(currentTextX, currentTextY)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformableTopAppBarTitle2(
    scrollBehavior: TopAppBarScrollBehavior,
    icon: ImageVector,
    iconContentDescription: String?,
    titleText: String,
    modifier: Modifier = Modifier
) {
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    val expandedTextStyle = MaterialTheme.typography.headlineMedium
    val collapsedTextStyle = MaterialTheme.typography.titleLarge
    val expandedIconSizeDp = 42.dp
    val collapsedIconSizeDp = 32.dp

    val currentIconSize = lerp(expandedIconSizeDp, collapsedIconSizeDp, collapsedFraction)
    val currentTextStyle = lerpTextStyle(expandedTextStyle, collapsedTextStyle, collapsedFraction)

    // Icon alpha: Fades out as it collapses.
    // Multiplicar por 1.5f (o 2f) y coerceAtMost(1f) hace que el alpha llegue a 0
    // antes de que collapsedFraction llegue a 1.0, para que desaparezca limpiamente.
    val iconAlpha = lerp(1f, 0f, (collapsedFraction * 1.5f).coerceAtMost(1f))

    Layout(
        content = {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                modifier = Modifier
                    .size(currentIconSize)
                    .graphicsLayer { alpha = iconAlpha } // Aplicar alpha para desvanecer

            )
            Text(
                text = titleText,
                style = currentTextStyle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val iconPlaceable = measurables[0].measure(constraints)
        val textPlaceable = measurables[1].measure(constraints)

        val availableWidth = constraints.maxWidth
        val availableHeight = constraints.maxHeight

        // EXPANDED state (icon above text, centered)
        val expandedIconX = (availableWidth - iconPlaceable.width) / 2
        val expandedContentHeight = iconPlaceable.height + 4.dp.toPx() + textPlaceable.height
        val expandedIconY = (availableHeight - expandedContentHeight) / 2
        val expandedTextX = (availableWidth - textPlaceable.width) / 2
        val expandedTextY = expandedIconY + iconPlaceable.height + 4.dp.toPx()

        // COLLAPSED state (Icon is faded out, text is at the start, vertically centered)
        // La posición del icono en colapsado no importa tanto visualmente ya que tendrá alpha 0,
        // pero su trayectoria puede influir en la interpolación.
        // Lo colocamos donde estaría si estuviera al lado del texto, para una trayectoria suave.
        val collapsedIconVisualX = 0
        val collapsedIconVisualY = (availableHeight - iconPlaceable.height) / 2

        // El texto colapsado se alinea al inicio del área del título.
        val collapsedTextX = 0 // Alineado al inicio del Layout
        val collapsedTextY = (availableHeight - textPlaceable.height) / 2

        // Interpolar posiciones
        val currentIconX = lerp(expandedIconX.toFloat(), collapsedIconVisualX.toFloat(), collapsedFraction).toInt()
        val currentIconY = lerp(expandedIconY.toFloat(), collapsedIconVisualY.toFloat(), collapsedFraction).toInt()
        val currentTextX = lerp(expandedTextX.toFloat(), collapsedTextX.toFloat(), collapsedFraction).toInt()
        val currentTextY = lerp(expandedTextY.toFloat(), collapsedTextY.toFloat(), collapsedFraction).toInt()

        layout(availableWidth, availableHeight) {
            // El icono se coloca, Modifier.graphicsLayer se encarga de su alpha.
            iconPlaceable.placeRelative(currentIconX, currentIconY)
            textPlaceable.placeRelative(currentTextX, currentTextY)
        }
    }
}


