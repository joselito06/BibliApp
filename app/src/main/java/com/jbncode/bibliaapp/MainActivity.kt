package com.jbncode.bibliaapp

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jbncode.bibliaapp.core.deeplink.DeepLinks
import com.jbncode.bibliaapp.core.notifications.ReminderScheduler
import com.jbncode.bibliaapp.core.notifications.ReminderStore
import com.jbncode.bibliaapp.ui.feature.library.LibraryScreen
import com.jbncode.bibliaapp.ui.feature.onboarding.OnboardingScreen
import com.jbncode.bibliaapp.ui.feature.reader.PlaybackViewModel
import com.jbncode.bibliaapp.ui.feature.reader.ReaderScreen
import com.jbncode.bibliaapp.ui.feature.reader.ReaderViewModel
import com.jbncode.bibliaapp.ui.feature.search.SearchScreen
import com.jbncode.bibliaapp.ui.theme.BibliaAppTheme
import com.jbncode.bibliaapp.utils.AppOpenManager
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.NavigationBottomJBN
import com.jbncode.bibliaapp.view.components.TopAppBarComponent
import com.jbncode.bibliaapp.view.screens.AboutScreen
import com.jbncode.bibliaapp.view.screens.BibleDownloadScreen
import com.jbncode.bibliaapp.view.screens.BibleSelectionScreen
import com.jbncode.bibliaapp.view.screens.ConfigScreen
import com.jbncode.bibliaapp.view.screens.HomeScreen
import com.jbncode.bibliaapp.view.screens.MoreScreen
import com.jbncode.bibliaapp.view.screens.NotificationSettingsScreen
import com.jbncode.bibliaapp.view.screens.RestartingScreen
import com.jbncode.bibliaapp.view.screens.ShareAppScreen
import com.jbncode.bibliaapp.view.screens.VersesScreen
import com.jbncode.bibliaapp.view.viewmodel.HomeViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MoreViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedPreferenceBible: SharedPreferenceBible

    private val homeViewModel: HomeViewmodel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()
    private val playbackViewModel: PlaybackViewModel by viewModels()
    private val moreViewmodel: MoreViewmodel by viewModels()
    private val versesViewmodel: VersesViewmodel by viewModels()
    private val settingViewmodel: SettingsViewmodel by viewModels()
    private val mainViewModel: MainViewmodel by viewModels()

    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var reminderStore: ReminderStore

    private lateinit var appOpenManager: AppOpenManager

    // A dónde llevar al usuario cuando toca una notificación ("home" o "reader").
    private val pendingDestination = mutableStateOf<String?>(null)

    // Versículo que llegó por un enlace compartido.
    private val pendingLink = mutableStateOf<DeepLinks.VerseLink?>(null)

    companion object { const val EXTRA_DESTINATION = "dest" }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        DeepLinks.parse(intent.data)?.let { pendingLink.value = it; intent.data = null }
        intent.getStringExtra(EXTRA_DESTINATION)?.let {
            pendingDestination.value = it
            intent.removeExtra(EXTRA_DESTINATION)   // que no se repita al recrear la Activity
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeIntent(intent)

        InterstitialAdManager.load(this)

        appOpenManager = AppOpenManager(application as JetpackApp)
        appOpenManager.loadAd()

        enableEdgeToEdge()
        setContent {
            val navigation = rememberNavController()
            val versionApp = stringResource(id = R.string.app_version_name)
            val showUpdate by settingViewmodel.showForceUpdate.collectAsState()

            val context = LocalContext.current
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                settingViewmodel.checkVersion(versionApp)
                sharedPreferenceBible.markDailyAppOpen()
                reminderScheduler.rescheduleAll()
            }

            // Navegar SIEMPRE limpiando hasta la raíz: si se apila Inicio encima de otra
            // pestaña, el estado guardado de esa pestaña queda contaminado y la barra
            // inferior "redirige" mal después (el bug de la notificación).
            fun goClean(route: String) {
                navigation.navigate(route) {
                    popUpTo(navigation.graph.startDestinationId) { saveState = false }
                    launchSingleTop = true
                }
            }

            val destination by pendingDestination
            LaunchedEffect(destination) {
                destination?.let { d ->
                    if (sharedPreferenceBible.isOnboardingDone()) {
                        goClean(if (d == "home") Routes.HomeScreen.route else Routes.BibleScreen.route)
                    }
                    pendingDestination.value = null
                }
            }

            val link by pendingLink
            LaunchedEffect(link) {
                link?.let { l ->
                    if (sharedPreferenceBible.isOnboardingDone()) {
                        readerViewModel.openFromLink(l.book, l.chapter, l.verse)
                        goClean(Routes.BibleScreen.route)
                    }
                    pendingLink.value = null
                }
            }

            // El permiso de notificaciones lo pide el primer uso; aquí solo para quien ya lo terminó.
            LaunchedEffect(Unit) {
                if (sharedPreferenceBible.isOnboardingDone() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val currentLanguageCode by mainViewModel.appLanguageCode.collectAsState()
            LaunchedEffect(currentLanguageCode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getSystemService(LocaleManager::class.java).applicationLocales =
                        LocaleList.forLanguageTags(currentLanguageCode)
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentLanguageCode))
                }
            }

            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val navBackStackEntry = navigation.currentBackStackEntryAsState().value

            val isScrollingDown by mainViewModel.isTopAppBarAnimationVisible.collectAsState()
            val isTopAppBarVisible by mainViewModel.isTopAppBarVisible.collectAsState()
            val isBottomBarVisible by mainViewModel.isBottomBarVisible.collectAsState()

            val currentAppTheme by settingViewmodel.appTheme.collectAsState()
            val onThemeChange: (com.jbncode.bibliaapp.ui.theme.AppThemeType) -> Unit = { settingViewmodel.setAppTheme(it) }

            // Toda pantalla con encabezado propio dibuja bajo la barra de estado;
            // si no, la franja de la barra queda del color del fondo (se veía blanca).
            val currentRoute = navBackStackEntry?.destination?.route
            val fullBleed = currentRoute in setOf(
                Routes.BibleScreen.route, Routes.SearchScreen.route, Routes.LibraryScreen.route,
                Routes.VersesScreen.route, Routes.NotificationScreen.route, Routes.BibleSelectionScreen.route,
                Routes.AboutScreen.route, Routes.ConfigScreen.route, Routes.ShareScreen.route,
                Routes.BibleDownloadScreen.route, Routes.TutorialsScreen.route
            )

            if (showUpdate) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ForceUpdateDialog(context)
                }
            } else {
                BibliaAppTheme(selectedTheme = currentAppTheme) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = if (fullBleed) WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        else ScaffoldDefaults.contentWindowInsets,
                        topBar = {
                            val route = navBackStackEntry?.destination?.route
                            val canPop = route != Routes.HomeScreen.route &&
                                route != Routes.BibleScreen.route &&
                                route != Routes.MoreScreen.route

                            val optionTopApp = when (route) {
                                Routes.ConfigScreen.route -> ConfigTopAppBar(
                                    Title = "Configuración",
                                    Actions = listOf(
                                        ActionTopApp(ImageIconVector = Icons.Rounded.Build, Description = "Configuración", OnClickIconButton = {}),
                                        ActionTopApp(ImageIconVector = Icons.Rounded.Image, Description = "Usuario", OnClickIconButton = {})
                                    )
                                )
                                Routes.AboutScreen.route -> ConfigTopAppBar(Title = "Acerca de nosotros")
                                Routes.NotificationScreen.route -> ConfigTopAppBar(Title = "Notificaciones")
                                Routes.VersesScreen.route -> ConfigTopAppBar(Title = "Versiculos del día")
                                Routes.BibleSelectionScreen.route -> ConfigTopAppBar(Title = "Tus biblias")
                                else -> ConfigTopAppBar(Title = "Busca tu biblia")
                            }

                            if (isTopAppBarVisible) {
                                TopAppBarComponent(
                                    navigation, canPop, scrollBehavior, optionTopApp, currentAppTheme, onThemeChange
                                )
                            }
                        },
                        bottomBar = {
                            if (isBottomBarVisible) {
                                if (navBackStackEntry?.destination?.route == Routes.BibleScreen.route) {
                                    AnimatedVisibility(
                                        visible = !isScrollingDown,
                                        enter = fadeIn() + slideInVertically { it },
                                        exit = fadeOut() + slideOutVertically { it }
                                    ) { NavigationBottomJBN(navigation) }
                                } else {
                                    NavigationBottomJBN(navigation)
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            val startDestination = remember {
                                if (sharedPreferenceBible.isOnboardingDone()) Routes.BibleScreen.route
                                else Routes.OnboardingScreen.route
                            }
                            NavHost(
                                navController = navigation,
                                startDestination = startDestination,
                                enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 12 } },
                                exitTransition = { fadeOut(tween(160)) },
                                popEnterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { -it / 12 } },
                                popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 12 } }
                            ) {
                                composable(Routes.HomeScreen.route) {
                                    HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        homeViewmodel = homeViewModel,
                                        versesViewmodel = versesViewmodel,
                                        readerViewmodel = readerViewModel,
                                        mainViewmodel = mainViewModel,
                                        navigation = navigation,
                                        playbackViewmodel = playbackViewModel
                                    )
                                }
                                composable(Routes.BibleScreen.route) {
                                    ReaderScreen(mainViewModel, readerViewModel, playbackViewModel, navigation)
                                }
                                composable(Routes.SearchScreen.route) {
                                    SearchScreen(mainViewModel, readerViewModel, navigation)
                                }
                                composable(Routes.MoreScreen.route) {
                                    MoreScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        moreViewmodel = moreViewmodel,
                                        versesViewmodel = versesViewmodel,
                                        mainViewmodel = mainViewModel,
                                        navigation = navigation
                                    )
                                }
                                composable(Routes.VersesScreen.route) {
                                    VersesScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        versesViewmodel = versesViewmodel,
                                        mainViewmodel = mainViewModel,
                                        navigation = navigation,
                                        scrollBehavior = scrollBehavior
                                    )
                                }
                                composable(Routes.NotificationScreen.route) {
                                    NotificationSettingsScreen(settingViewmodel, mainViewModel, navigation)
                                }
                                composable(Routes.BibleSelectionScreen.route) {
                                    BibleSelectionScreen(settingViewmodel, mainViewModel, navigation)
                                }
                                composable(Routes.AboutScreen.route) {
                                    AboutScreen(mainViewModel, navigation)
                                }
                                composable(Routes.ConfigScreen.route) {
                                    ConfigScreen(mainViewModel, settingViewmodel, playbackViewModel, navigation)
                                }
                                composable(Routes.ShareScreen.route) {
                                    ShareAppScreen(mainViewModel, navigation)
                                }
                                composable(Routes.BibleDownloadScreen.route) {
                                    BibleDownloadScreen(navController = navigation, mainViewmodel = mainViewModel)
                                }
                                composable(Routes.RestartingScreen.route) {
                                    RestartingScreen()
                                }
                                composable(Routes.OnboardingScreen.route) {
                                    OnboardingScreen(mainViewModel, onFinished = { restart ->
                                        navigation.navigate(
                                            if (restart) Routes.RestartingScreen.route else Routes.BibleScreen.route
                                        ) {
                                            popUpTo(Routes.OnboardingScreen.route) { inclusive = true }
                                        }
                                    })
                                }
                                composable(Routes.TutorialsScreen.route) {
                                    com.jbncode.bibliaapp.ui.feature.tutorial.TutorialsScreen(mainViewModel, navigation)
                                }
                                composable(Routes.LibraryScreen.route) {
                                    LibraryScreen(mainViewModel, readerViewModel, navigation)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        reminderStore.markOpened()   // si el usuario abre la app, los avisos vuelven a su ritmo normal
        appOpenManager.showAdIfAvailable(this)
    }
}

@Composable
fun ForceUpdateDialog(context: Context) {
    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
    val minVersion = remoteConfig.getString("min_version_required")
    val versionIns = stringResource(id = R.string.app_version_name)
    val packageName = context.packageName

    AlertDialog(
        onDismissRequest = { },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.bible_force_update_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.bible_force_update_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text(
                        text = stringResource(R.string.bible_force_update_version_label, minVersion),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.bible_force_update_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val playStoreUri = Uri.parse("market://details?id=$packageName")
                    val browserUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                }
            ) {
                Text(stringResource(R.string.bible_force_update_button_google_play))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val subject = Uri.encode("Problema con actualización - BibliApp")
                    val body = Uri.encode("Hola, tengo problemas para actualizar a la versión $minVersion.\nMi versión actual es: $versionIns")
                    val uriString = "mailto:soporte.bibliaapp@gmail.com?subject=$subject&body=$body"
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse(uriString) }
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.bible_force_update_no_email_app),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Text(stringResource(R.string.bible_force_update_button_support))
            }
        }
    )
}
