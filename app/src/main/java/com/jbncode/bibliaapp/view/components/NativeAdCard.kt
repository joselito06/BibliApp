package com.jbncode.bibliaapp.view.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Mantiene el nombre usado en Inicio; ahora usa el anuncio nativo con colores del tema. */
@Composable
fun NativeAdCard() = InlineNativeAd(Modifier.padding(vertical = 4.dp))
