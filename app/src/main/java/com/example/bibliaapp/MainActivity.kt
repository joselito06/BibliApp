package com.example.bibliaapp

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bibliaapp.di.RoomModule
import com.example.bibliaapp.ui.theme.AppThemeType
import com.example.bibliaapp.ui.theme.BibliaAppTheme
import com.example.bibliaapp.view.Routes
import com.example.bibliaapp.view.components.NavigationBottomJBN
import com.example.bibliaapp.view.components.TopAppBarComponent
import com.example.bibliaapp.view.screens.AboutScreen
import com.example.bibliaapp.view.screens.BibleDownloadScreen
import com.example.bibliaapp.view.screens.BibleScreen
import com.example.bibliaapp.view.screens.BibleSelectionScreen
import com.example.bibliaapp.view.screens.ConfigScreen
import com.example.bibliaapp.view.screens.HomeScreen
import com.example.bibliaapp.view.screens.MoreScreen
import com.example.bibliaapp.view.screens.NotificationSettingsScreen
import com.example.bibliaapp.view.screens.RestartingScreen
import com.example.bibliaapp.view.screens.ShareAppScreen
import com.example.bibliaapp.view.screens.VersesScreen
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import com.example.bibliaapp.view.viewmodel.HomeViewmodel
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.MoreViewmodel
import com.example.bibliaapp.view.viewmodel.SettingsViewmodel
import com.example.bibliaapp.view.viewmodel.VersesViewmodel
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



    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoomModule.cleanupTempFiles(this)
        RoomModule.forceCloseAllConnections(this)
        enableEdgeToEdge()
        setContent {

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

            LaunchedEffect(Unit) {
                // Cuando habilitas notificaciones o al inicio de la pantalla:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU es API 33
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        // Permiso ya concedido
                    } else {
                        // Solicitar permiso
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
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
                                    NavigationBottomJBN(navigation = navigation, isBottomBarVisible = isBottomBarVisible)
                                }
                            }else{
                                NavigationBottomJBN(navigation = navigation, isBottomBarVisible = isBottomBarVisible)
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
        scheduleDailyVerseNotification(this)
    }

    fun scheduleDailyVerseNotification(context: Context) {
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
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BibliaAppTheme {
        Greeting("Android")
    }
}