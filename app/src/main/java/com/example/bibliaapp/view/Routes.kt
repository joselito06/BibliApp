package com.example.bibliaapp.view

sealed class Routes(val route: String) {
    object HomeScreen: Routes("homeScreen")
    object BibleScreen: Routes("bibleScreen")
    object MoreScreen: Routes("moreScreen")
    object VersesScreen: Routes("versesScreen")
}