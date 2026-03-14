package com.jbncode.bibliaapp.view

sealed class Routes(val route: String) {
    object HomeScreen: Routes("homeScreen")
    object BibleScreen: Routes("bibleScreen")
    object MoreScreen: Routes("moreScreen")
    object VersesScreen: Routes("versesScreen")
    object NotificationScreen: Routes("notificationScreen")
    object BibleSelectionScreen: Routes("bibleSelectionScreen")
    object AboutScreen: Routes("aboutScreen")
    object ConfigScreen: Routes("configScreen")
    object ShareScreen: Routes("shareScreen")
    object BibleDownloadScreen: Routes("bibleDownloadScreen")
    object RestartingScreen: Routes("restartingScreen")

}