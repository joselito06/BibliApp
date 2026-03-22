package com.jbncode.bibliaapp.view.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import androidx.core.net.toUri

// --- FUNCIONES AUXILIARES DEFINIDAS AQUÍ A NIVEL DE ARCHIVO ---
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Color.isPerceivedAsDark(): Boolean {
    return this.luminance() < 0.5f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppScreen(mainViewmodel: MainViewmodel, navigation: NavHostController) {

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

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = scrollBehavior.state
    val isExpanded = scrollState.collapsedFraction < 0.05f
    val topAppBarShape = if (isExpanded) {
        RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp) // Solo esquinas inferiores redondeadas
    } else {
        RectangleShape // Forma rectangular normal cuando está colapsada o en transición
    }

    val shareMessage = stringResource(R.string.share_app_message_template)+ "https://play.google.com/store/apps/details?id=com.jbncode.bibliaapp"
    .trimIndent()

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
                        icon = Icons.Default.Share,
                        iconContentDescription = "Compartir",
                        titleText = stringResource(R.string.share_screen_title)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Hero card ────────────────────────────────────────────────────
            ShareHeroCard()

            // ── Botón principal compartir ─────────────────────────────────────
            Button(
                onClick = { shareText(context, shareMessage, context.getString(R.string.share_app_chooser_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.share_app_button_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Opciones rápidas de compartir ─────────────────────────────────
            ShareOptionsRow(context = context, shareMessage = shareMessage)

            // ── Calificar en Play Store ───────────────────────────────────────
            RateAppCard(context = context)

            // ── Frase de cierre ───────────────────────────────────────────────
            ShareClosingCard()

            Spacer(Modifier.height(8.dp))
        }

    }

}

// ── Hero card con gradiente y logo ───────────────────────────────────────────
@Composable
private fun ShareHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icono con círculo de fondo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.share_app_card_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.share_app_card_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ── Fila de botones rápidos (WhatsApp, más opciones) ─────────────────────────
@Composable
private fun ShareOptionsRow(context: Context, shareMessage: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShareOptionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Whatsapp,
            label = "WhatsApp",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // WhatsApp no está instalado, usar compartir general
                    shareText(context, shareMessage, context.getString(R.string.share_app_chooser_title))
                }
            }
        )

        ShareOptionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Share,
            label = stringResource(R.string.share_app_options_title),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = {
                shareText(context, shareMessage, context.getString(R.string.share_app_chooser_title))
            }
        )
    }
}

@Composable
private fun ShareOptionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ── Tarjeta para calificar en Play Store ─────────────────────────────────────
@Composable
private fun RateAppCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.share_app_rate_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.share_app_rate_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }

            FilledTonalButton(
                onClick = {
                    val packageName = context.packageName
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                data = "market://details?id=$packageName".toUri()
                            }
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                data =
                                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text(stringResource(R.string.share_app_rate_button), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Tarjeta de cierre con frase ───────────────────────────────────────────────
@Composable
private fun ShareClosingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.share_app_closing_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ── Funciones auxiliares ──────────────────────────────────────────────────────
fun shareText(context: Context, text: String, textIntent: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, textIntent))
}
