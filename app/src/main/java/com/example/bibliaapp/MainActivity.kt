package com.example.bibliaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bibliaapp.ui.theme.BibliaAppTheme
import com.example.bibliaapp.view.Routes
import com.example.bibliaapp.view.components.NavigationBottomJBN
import com.example.bibliaapp.view.components.TopAppBarComponent
import com.example.bibliaapp.view.screens.BibleScreen
import com.example.bibliaapp.view.screens.HomeScreen
import com.example.bibliaapp.view.screens.MoreScreen
import com.example.bibliaapp.view.screens.VersesScreen
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import com.example.bibliaapp.view.viewmodel.HomeViewmodel
import com.example.bibliaapp.view.viewmodel.MoreViewmodel
import com.example.bibliaapp.view.viewmodel.VersesViewmodel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewmodel by viewModels()
    private val bibleViewModel : BibleViewmodel by viewModels()
    private val moreViewmodel: MoreViewmodel by viewModels()
    private val versesViewmodel: VersesViewmodel by viewModels()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navigation = rememberNavController()
            // Estado para rastrear la posición del scroll
            val scrollState = rememberScrollState()
            // Estado para rastrear si la TopAppBar y la NavigationBar están visibles
            var isTopBarVisible by remember { mutableStateOf(true) }
            var isBottomBarVisible by remember { mutableStateOf(true) }
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val navBackStackEntry = navigation.currentBackStackEntryAsState().value

            BibliaAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {

                        val canPop = navBackStackEntry?.destination?.route != Routes.HomeScreen.route
                                && navBackStackEntry?.destination?.route != Routes.BibleScreen.route
                                && navBackStackEntry?.destination?.route != Routes.MoreScreen.route
                        TopAppBarComponent(navigation, canPop, scrollBehavior)
                             },
                    bottomBar = {
//                        if (navBackStackEntry?.destination?.route == Routes.HomeScreen.route){
//                            NavigationBottomJBN(navigation = navigation, isBottomBarVisible = false)
//                        }
                        NavigationBottomJBN(navigation = navigation, isBottomBarVisible = isBottomBarVisible)
                    }

                )
                { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding),)
                    {
                        NavHost(navController = navigation, startDestination = Routes.BibleScreen.route){
                            composable(Routes.HomeScreen.route){
                                HomeScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    homeViewModel,
                                    navigation
                                )
                            }
                            composable(Routes.BibleScreen.route){
                                BibleScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    bibleViewModel,
                                    navigation,
                                    scrollBehavior
                                )
                            }
                            composable(Routes.MoreScreen.route){
                                MoreScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    moreViewmodel,
                                    versesViewmodel,
                                    navigation
                                )
                            }
                            composable(Routes.VersesScreen.route){
                                VersesScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    versesViewmodel,
                                    navigation,
                                    scrollBehavior
                                )
                            }
                        }
                    }
//                    HomeScreen(
//                        modifier = Modifier.padding(innerPadding),
//                        homeViewmodel = homeViewModel
//                    )

                }
            }
        }
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