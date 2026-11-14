package com.jbncode.bibliaapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

val FavoriteGold = Color(0xFFFFC107)

/** Estrella con rebote, giro, destello de partículas y vibración. Reutilizable (lector y tutoriales). */
@Composable
fun FavoriteStarButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Int = 24) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isFavorite) {
        if (first) { first = false; return@LaunchedEffect }
        if (isFavorite) {
            launch { burst.snapTo(0f); burst.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            launch { rotation.snapTo(-40f); rotation.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) }
            scale.snapTo(0.4f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            scale.animateTo(0.75f, tween(90))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val tint by animateColorAsState(if (isFavorite) FavoriteGold else LocalContentColor.current, tween(250), label = "favTint")
    IconButton(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        modifier = modifier.drawBehind {
            val p = burst.value
            if (p > 0f && p < 1f) {
                repeat(8) { i ->
                    val a = Math.toRadians(i * 45.0)
                    val r = (size / 2 + 2).dp.toPx() + p * 22.dp.toPx()
                    drawCircle(
                        FavoriteGold.copy(alpha = 1f - p),
                        radius = (4f * (1f - p) + 1f).dp.toPx(),
                        center = Offset(center.x + cos(a).toFloat() * r, center.y + sin(a).toFloat() * r)
                    )
                }
            }
        }
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, tint = tint,
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = rotation.value }
        )
    }
}
