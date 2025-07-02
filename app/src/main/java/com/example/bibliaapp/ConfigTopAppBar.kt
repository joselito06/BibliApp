package com.example.bibliaapp

import androidx.compose.ui.graphics.vector.ImageVector

data class ConfigTopAppBar(
    val Title: String,
    val Actions: List<ActionTopApp> = listOf()
)

data class ActionTopApp(
    val ImageIconVector: ImageVector,
    val Description: String,
    val OnClickIconButton: () -> Unit

)
