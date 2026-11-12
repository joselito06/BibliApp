package com.jbncode.bibliaapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jbncode.bibliaapp.view.screens.findActivity
import com.jbncode.bibliaapp.view.screens.isPerceivedAsDark

/** Reemplaza el bloque de 25 líneas que estaba copiado en cada pantalla. */
@Composable
fun SystemBarsColor(color: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findActivity()?.window
    DisposableEffect(window, color) {
        if (window == null) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val oldStatus = window.statusBarColor
        val oldNav = window.navigationBarColor
        val oldLightStatus = controller.isAppearanceLightStatusBars
        val oldLightNav = controller.isAppearanceLightNavigationBars
        window.statusBarColor = color.toArgb()
        window.navigationBarColor = color.toArgb()
        controller.isAppearanceLightStatusBars = !color.isPerceivedAsDark()
        controller.isAppearanceLightNavigationBars = !color.isPerceivedAsDark()
        onDispose {
            window.statusBarColor = oldStatus
            window.navigationBarColor = oldNav
            controller.isAppearanceLightStatusBars = oldLightStatus
            controller.isAppearanceLightNavigationBars = oldLightNav
        }
    }
}
