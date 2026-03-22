package com.jbncode.bibliaapp

import android.Manifest
import android.app.LocaleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jbncode.bibliaapp.di.RoomModule
import com.jbncode.bibliaapp.ui.theme.AppThemeType
import com.jbncode.bibliaapp.ui.theme.BibliaAppTheme
import com.jbncode.bibliaapp.utils.AppOpenManager
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.NavigationBottomJBN
import com.jbncode.bibliaapp.view.components.TopAppBarComponent
import com.jbncode.bibliaapp.view.screens.AboutScreen
import com.jbncode.bibliaapp.view.screens.BibleDownloadScreen
import com.jbncode.bibliaapp.view.screens.BibleScreen
import com.jbncode.bibliaapp.view.screens.BibleSelectionScreen
import com.jbncode.bibliaapp.view.screens.ConfigScreen
import com.jbncode.bibliaapp.view.screens.HomeScreen
import com.jbncode.bibliaapp.view.screens.MoreScreen
import com.jbncode.bibliaapp.view.screens.NotificationSettingsScreen
import com.jbncode.bibliaapp.view.screens.RestartingScreen
import com.jbncode.bibliaapp.view.screens.ShareAppScreen
import com.jbncode.bibliaapp.view.screens.VersesScreen
import com.jbncode.bibliaapp.view.viewmodel.BibleViewmodel
import com.jbncode.bibliaapp.view.viewmodel.HomeViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MoreViewmodel
import com.jbncode.bibliaapp.view.viewmodel.SettingsViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedPreferenceBible: SharedPreferenceBible // Inyección

    private val homeViewModel: HomeViewmodel by viewModels()
    private val bibleViewModel : BibleViewmodel by viewModels()
    private val moreViewmodel: MoreViewmodel by viewModels()
    private val versesViewmodel: VersesViewmodel by viewModels()
    private val settingViewmodel: SettingsViewmodel by viewModels()
    private val mainViewModel : MainViewmodel by viewModels()
    private var appStartTime: Long = 0

    private lateinit var appOpenManager: AppOpenManager

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                // ✅ Aquí conectamos con las funciones de tu ViewModel
                "STOP_TTS" -> bibleViewModel.stopReading()
                "PAUSE_TTS" -> bibleViewModel.pauseReading()
                "PLAY_TTS" -> bibleViewModel.resumeReading()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Intersticial
        InterstitialAdManager.load(this)

        // Inicializar y mostrar App Open
        appOpenManager = AppOpenManager(application as JetpackApp)
        appOpenManager.loadAd()

        val filter = IntentFilter().apply {
            addAction("STOP_TTS")
            addAction("PAUSE_TTS")
            addAction("PLAY_TTS")
        }

        // Registrar el receptor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        // ✅ NUEVO: Marcar inicio de sesión
        appStartTime = System.currentTimeMillis()

        RoomModule.cleanupTempFiles(this)
        RoomModule.forceCloseAllConnections(this)
        enableEdgeToEdge()
        setContent {
            val versionApp = stringResource(id = R.string.app_version_name)
            val showUpdate by settingViewmodel.showForceUpdate.collectAsState()

            //Notificaciones----------------------------------
            val context = LocalContext.current
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permiso concedido
                } else {
                    // Permiso denegado. Informar al usuario.
                }
            }

            // NUEVO: Trackear apertura de app
            LaunchedEffect(Unit) {
                // Actualizar racha de lectura al abrir la app
                settingViewmodel.checkVersion(versionApp)
                sharedPreferenceBible.markDailyAppOpen()
                settingViewmodel.rescheduleIfEnabled()
            }

            LaunchedEffect(Unit) {
                // Cuando habilitas notificaciones o al inicio de la pantalla:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU es API 33
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!granted) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // ✅ Programar notificación si está habilitada
                // Esto asegura que si el usuario ya tenía notificaciones ON,
                // se reprograme al abrir la app (por si el dispositivo reinició)
                if (sharedPreferenceBible.notificationsEnabled.value) {
                    // Necesitamos inyectar NotificationScheduler en MainActivity
                    // o llamarlo desde SettingsViewmodel
                }
                //------------------------------------------------
            }

            val currentLanguageCode by mainViewModel.appLanguageCode.collectAsState()

            LaunchedEffect(currentLanguageCode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getSystemService(LocaleManager::class.java)
                        .applicationLocales = LocaleList.forLanguageTags(currentLanguageCode)
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentLanguageCode))
                }
            }

            val navigation = rememberNavController()
            // Estado para rastrear la posición del scroll
            val scrollState = rememberScrollState()
            // Estado para rastrear si la TopAppBar y la NavigationBar están visibles
            var isTopBarVisible by remember { mutableStateOf(true) }
            //var isBottomBarVisible by remember { mutableStateOf(true) }
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val navBackStackEntry = navigation.currentBackStackEntryAsState().value
            val versesDaily by versesViewmodel.verseOfTheDay.collectAsState()

            val isScrollingDown by mainViewModel.isTopAppBarAnimationVisible.collectAsState()
            val isTopAppBarVisible by mainViewModel.isTopAppBarVisible.collectAsState()
            val isBottomBarVisible by mainViewModel.isBottomBarVisible.collectAsState()
            val isFloatingActionButtonVisible by mainViewModel.isFloatingActionButtonVisible.collectAsState()
            val fabAction by mainViewModel.fabAction.collectAsState()
            val fabIcon by mainViewModel.fabIcon.collectAsState()

            //var currentAppTheme by remember { mutableStateOf(sharedPreferenceBible.getAppTheme()) }
            val currentAppTheme by settingViewmodel.appTheme.collectAsState() // O el que prefieras por defecto
            val onThemeChange: (AppThemeType) -> Unit = { newTheme ->
                settingViewmodel.setAppTheme(newTheme)
            }
            /*val onThemeChange: (AppThemeType) -> Unit = { newTheme ->
                currentAppTheme = newTheme
                sharedPreferenceBible.saveAppTheme(newTheme)
            }*/

            val isSpeaking by bibleViewModel.isSpeaking.collectAsState()

            if (showUpdate) {
                // Bloqueamos la app mostrando SOLO el diálogo
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ForceUpdateDialog(context)
                }
            } else {
                BibliaAppTheme(
                    selectedTheme = currentAppTheme,
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            val canPop = navBackStackEntry?.destination?.route != Routes.HomeScreen.route
                                    && navBackStackEntry?.destination?.route != Routes.BibleScreen.route
                                    && navBackStackEntry?.destination?.route != Routes.MoreScreen.route

                            val optionTopApp = if (navBackStackEntry?.destination?.route == Routes.ConfigScreen.route)
                                ConfigTopAppBar(
                                    Title = "Configuración",
                                    Actions = listOf(
                                        ActionTopApp(
                                            ImageIconVector = Icons.Rounded.Build,
                                            Description = "Configuración",
                                            OnClickIconButton = {}
                                        ),
                                        ActionTopApp(
                                            ImageIconVector = Icons.Rounded.Image,
                                            Description = "Usuario",
                                            OnClickIconButton = {}
                                        )
                                    )
                                )
                            else if (navBackStackEntry?.destination?.route == Routes.AboutScreen.route)
                                ConfigTopAppBar(Title = "Acerca de nosotros" )
                            else if (navBackStackEntry?.destination?.route == Routes.NotificationScreen.route)
                                ConfigTopAppBar(Title = "Notificaciones" )
                            else if (navBackStackEntry?.destination?.route == Routes.VersesScreen.route)
                                ConfigTopAppBar(Title = "Versiculos del día" )
                            else if (navBackStackEntry?.destination?.route == Routes.BibleSelectionScreen.route)
                                ConfigTopAppBar(Title = "Tus biblias" )
                            else ConfigTopAppBar(Title = "Busca tu biblia")

                            val titleTopAppBar = if (navBackStackEntry?.destination?.route == Routes.ConfigScreen.route) "Configuración"
                            else if (navBackStackEntry?.destination?.route == Routes.AboutScreen.route) "Acerca de nosotros"
                            else if (navBackStackEntry?.destination?.route == Routes.NotificationScreen.route) "Notificaciones"
                            else if (navBackStackEntry?.destination?.route == Routes.VersesScreen.route) "Versiculos del día"
                            else if (navBackStackEntry?.destination?.route == Routes.BibleSelectionScreen.route) "Tus biblias"
                            else "Busca tu biblia"


                            if(isTopAppBarVisible){
                                if (false){
                                    AnimatedVisibility(
                                        visible = !isScrollingDown,
                                        enter = fadeIn() + slideInVertically { -it },
                                        exit = fadeOut() + slideOutVertically { -it }
                                    ){
                                        TopAppBarComponent(
                                            navigation,
                                            canPop,
                                            scrollBehavior,
                                            optionTopApp,
                                            currentAppTheme,
                                            onThemeChange
                                        )
                                    }
                                }else{
                                    TopAppBarComponent(
                                        navigation,
                                        canPop,
                                        scrollBehavior,
                                        optionTopApp,
                                        currentAppTheme,
                                        onThemeChange
                                    )
                                }


                            }

                        },
                        bottomBar = {
                            if (isBottomBarVisible){
                                if (navBackStackEntry?.destination?.route == Routes.BibleScreen.route){
                                    AnimatedVisibility(
                                        visible = !isScrollingDown,
                                        enter = fadeIn() + slideInVertically { it },
                                        exit = fadeOut() + slideOutVertically { it }
                                    ){
                                        NavigationBottomJBN(
                                            navigation = navigation,
                                            isBottomBarVisible = isBottomBarVisible,
                                            isSpeaking = isSpeaking,
                                            onStopReading = {
                                                bibleViewModel.stopReading()
                                                bibleViewModel.stopReadingService()
                                            }
                                        )
                                    }
                                    //BannerAd(stringResource(R.string.banner_ad_id))
                                }else{
                                    NavigationBottomJBN(
                                        navigation = navigation,
                                        isBottomBarVisible = isBottomBarVisible,
                                        isSpeaking = isSpeaking,
                                        onStopReading = {
                                            bibleViewModel.stopReading()
                                            bibleViewModel.stopReadingService()
                                        }
                                    )
                                }

                            }
//                        if (navBackStackEntry?.destination?.route == Routes.HomeScreen.route){
//                            NavigationBottomJBN(navigation = navigation, isBottomBarVisible = false)
//                        }

                        },
                        floatingActionButtonPosition = FabPosition.End,
                        floatingActionButton = {
                            if (isFloatingActionButtonVisible){

                            }
                        }

                    )
                    { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding))
                        {
                            NavHost(navController = navigation, startDestination = Routes.BibleScreen.route){
                                composable(Routes.HomeScreen.route){
                                    HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        homeViewModel,
                                        versesViewmodel,
                                        bibleViewModel,
                                        mainViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.BibleScreen.route){
                                    BibleScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        bibleViewModel,
                                        mainViewModel,
                                        navigation,
                                        scrollBehavior
                                    )
                                }
                                composable(Routes.MoreScreen.route){
                                    MoreScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        moreViewmodel,
                                        versesViewmodel,
                                        mainViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.VersesScreen.route){
                                    VersesScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        versesViewmodel,
                                        mainViewModel,
                                        navigation,
                                        scrollBehavior
                                    )
                                }
                                composable(Routes.NotificationScreen.route){
                                    NotificationSettingsScreen(
                                        settingViewmodel,
                                        mainViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.BibleSelectionScreen.route){
                                    BibleSelectionScreen(
                                        settingViewmodel,
                                        mainViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.AboutScreen.route){
                                    AboutScreen(mainViewModel, navigation)
                                }
                                composable(Routes.ConfigScreen.route){
                                    ConfigScreen(
                                        mainViewModel,
                                        settingViewmodel,
                                        bibleViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.ShareScreen.route){
                                    ShareAppScreen(
                                        mainViewModel,
                                        navigation
                                    )
                                }
                                composable(Routes.BibleDownloadScreen.route) {
                                    BibleDownloadScreen(
                                        navController = navigation,
                                    )
                                }
                                composable(Routes.RestartingScreen.route){
                                    RestartingScreen()
                                }
                            }
                        }

                    }
                }
            }

        }
        //scheduleDailyVerseNotification(this)
    }

    override fun onPause() {
        super.onPause()

        // ✅ NUEVO: Guardar tiempo de sesión cuando se pausa la app
        val sessionTime = (System.currentTimeMillis() - appStartTime) / 60000
        if (sessionTime > 0) {
            sharedPreferenceBible.addReadingTime(sessionTime.toInt())
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ NUEVO: Resetear contador al resumir
        appStartTime = System.currentTimeMillis()
        // Mostrar anuncio al volver a abrir la app si está listo
        appOpenManager.showAdIfAvailable(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ PASO C: Muy importante desregistrar para evitar fugas de memoria
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ya estaba desregistrado
        }
    }

    /*fun scheduleDailyVerseNotification(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<VerseNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(getInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "verse_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun getInitialDelay(): Long {
        val now = Calendar.getInstance()
        val nextExecution = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)  // Notificar a las 8 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(nextExecution)) nextExecution.add(Calendar.DAY_OF_MONTH, 1) // Si ya pasó, programar para el día siguiente
        return nextExecution.timeInMillis - now.timeInMillis
    }*/

}

@Composable
fun ForceUpdateDialog(context: Context) {
    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
    // Obtenemos la versión requerida desde Firebase Remote Config
    val minVersion = remoteConfig.getString("min_version_required")
    val versionIns = stringResource(id = R.string.app_version_name)
    val packageName = context.packageName

    AlertDialog(
        onDismissRequest = { /* No permitir cerrar */ },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
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

                // Badge de versión requerida
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
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

                    val intent = Intent(Intent.ACTION_VIEW, playStoreUri)
                    try {
                        // Intenta abrir la App de Play Store
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Si no tiene Play Store instalado, abre el navegador
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
                    /*val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:soporte.angelito@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Problema con actualización - Angelito RD")
                        putExtra(Intent.EXTRA_TEXT, "Hola, tengo problemas para actualizar a la versión $minVersion. Mi versión actual es ${R.string.app_version_name}.")
                    }*/
                    // Codificamos el texto para que Gmail lo reconozca correctamente
                    val subject = Uri.encode("Problema con actualización - BibliApp")
                    val body = Uri.encode("Hola, tengo problemas para actualizar a la versión $minVersion.\nMi versión actual es: ${versionIns}")

                    // Construimos la URI completa con el esquema mailto
                    val uriString = "mailto:soporte.bibliaapp@gmail.com?subject=$subject&body=$body"

                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(uriString)
                    }
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        // En caso de que no haya ninguna app de correo instalada
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
