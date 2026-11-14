package com.jbncode.bibliaapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Misma API que material3.Card, pero con un borde fino (outlineVariant) por defecto.
 * Así las tarjetas se separan del fondo en TODOS los temas (en los oscuros las sombras no se ven).
 * Para una tarjeta sin borde (p. ej. una destacada con color propio) pasa `border = null`.
 * Uso: cambia `import androidx.compose.material3.Card` por `import com.jbncode.bibliaapp.ui.components.Card`.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) = androidx.compose.material3.Card(modifier, shape, colors, elevation, border, content)

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) = androidx.compose.material3.Card(onClick, modifier, enabled, shape, colors, elevation, border, content = content)

