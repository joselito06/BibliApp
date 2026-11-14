package com.jbncode.bibliaapp.ui.feature.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.FavoriteGold
import com.jbncode.bibliaapp.ui.components.FavoriteStarButton
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.ui.components.pressScale
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Demo { READ, SELECT, FAV, HIGHLIGHT, NOTE, LISTEN, SEARCH, BIBLES, STREAK }

private data class Topic(val demo: Demo, val icon: ImageVector, val title: Int, val body: Int, val hint: Int?)

private val TOPICS = listOf(
    Topic(Demo.READ, Icons.Default.AutoStories, R.string.tut_read_title, R.string.tut_read_body, null),
    Topic(Demo.SELECT, Icons.Default.TouchApp, R.string.tut_select_title, R.string.tut_select_body, R.string.tut_try_select),
    Topic(Demo.FAV, Icons.Default.Star, R.string.tut_fav_title, R.string.tut_fav_body, R.string.tut_try_fav),
    Topic(Demo.HIGHLIGHT, Icons.Default.Brush, R.string.tut_highlight_title, R.string.tut_highlight_body, R.string.tut_try_highlight),
    Topic(Demo.NOTE, Icons.Default.EditNote, R.string.tut_note_title, R.string.tut_note_body, R.string.tut_try_note),
    Topic(Demo.LISTEN, Icons.AutoMirrored.Filled.VolumeUp, R.string.tut_listen_title, R.string.tut_listen_body, R.string.tut_try_listen),
    Topic(Demo.SEARCH, Icons.Default.Search, R.string.tut_search_title, R.string.tut_search_body, null),
    Topic(Demo.BIBLES, Icons.Default.AutoStories, R.string.tut_bibles_title, R.string.tut_bibles_body, R.string.tut_try_bibles),
    Topic(Demo.STREAK, Icons.Default.LocalFireDepartment, R.string.tut_streak_title, R.string.tut_streak_body, null)
)

private val COLORS = listOf(0xFFFFF176, 0xFFA5D6A7, 0xFF90CAF9, 0xFFF48FB1).map { Color(it) }

@Composable
fun TutorialsScreen(mainViewmodel: MainViewmodel, navigation: NavHostController) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    LaunchedEffect(Unit) { mainViewmodel.showTopAppBar(false); mainViewmodel.showBottomBar(false) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tutorials_title),
                icon = Icons.Default.AutoStories,
                onBack = { navigation.popBackStack() }
            )
        }
    ) { inner ->
        TutorialPager(Modifier.padding(inner), finishLabel = R.string.tutorials_done, onFinish = { navigation.popBackStack() })
    }
}

/**
 * Tutoriales interactivos. Se usa en la pantalla de Más y como último paso de la configuración inicial.
 * [onBackFromFirst]: si no es null, el botón "Atrás" de la primera página lo invoca (volver al paso previo).
 * [onSkip]: si no es null, muestra "Saltar".
 */
@Composable
fun TutorialPager(
    modifier: Modifier = Modifier,
    finishLabel: Int,
    onFinish: () -> Unit,
    onBackFromFirst: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null
) {
    val pager = rememberPagerState { TOPICS.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == TOPICS.lastIndex
    // El gesto "atrás" del sistema retrocede una página antes de salir.
    androidx.activity.compose.BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TOPICS.indices.forEach { i ->
                    val c by animateColorAsState(
                        if (i <= pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        tween(300), label = "seg"
                    )
                    Box(
                        Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c)
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }
                    )
                }
            }
            if (onSkip != null && !last) {
                androidx.compose.material3.TextButton(onClick = onSkip) { Text(stringResource(R.string.tutorials_skip)) }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            TopicPage(TOPICS[page], active = pager.currentPage == page)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    else onBackFromFirst?.invoke()
                },
                enabled = pager.currentPage > 0 || onBackFromFirst != null,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.onboarding_back))
            }
            Button(
                onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AnimatedContent(last, label = "tutNext", transitionSpec = { fadeIn() togetherWith fadeOut() }) { isLast ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (isLast) finishLabel else R.string.onboarding_continue), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicPage(topic: Topic, active: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Escenario con demostración en vivo.
        Surface(
            Modifier.fillMaxWidth().height(230.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), Color.Transparent))
                ).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (topic.demo) {
                    Demo.READ -> DemoRead(active)
                    Demo.SELECT -> DemoSelect()
                    Demo.FAV -> DemoFavorite()
                    Demo.HIGHLIGHT -> DemoHighlight()
                    Demo.NOTE -> DemoNote(active)
                    Demo.LISTEN -> DemoListen()
                    Demo.SEARCH -> DemoSearch(active)
                    Demo.BIBLES -> DemoBibles()
                    Demo.STREAK -> DemoStreak(active)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(topic.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                stringResource(topic.title),
                style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(topic.body), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        topic.hint?.let { hint ->
            Spacer(Modifier.height(16.dp))
            val pulse = rememberInfiniteTransition(label = "hint")
            val a by pulse.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.graphicsLayer { alpha = a }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(hint), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Demos ───────────────────────────────────────────────────────────────────

@Composable
private fun VerseText(
    modifier: Modifier = Modifier, bg: Color = Color.Transparent, selected: Boolean = false, flash: Float = 0f, onClick: (() -> Unit)? = null,
    suffix: String = ""
) {
    val sel by animateColorAsState(if (selected) MaterialTheme.colorScheme.tertiaryContainer else bg, tween(220), label = "vbg")
    val sc by animateFloatAsState(if (selected) 1.02f else 1f, spring(dampingRatio = 0.6f), label = "vsc")
    Column(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(14.dp))
            .background(sel)
            .background(FavoriteGold.copy(alpha = 0.35f * flash))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp)
    ) {
        Text(
            stringResource(R.string.tut_demo_verse) + suffix,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, lineHeight = 26.sp)
        )
        Text(stringResource(R.string.tut_demo_ref), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DemoRead(active: Boolean) {
    val t = rememberInfiniteTransition(label = "read")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "p")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Dos "páginas": la actual sale a la izquierda y entra la siguiente.
        val shift = if (active) p else 0f
        Page(Modifier.graphicsLayer { translationX = -shift * 520f; alpha = 1f - shift }, "1")
        Page(Modifier.graphicsLayer { translationX = (1f - shift) * 520f; alpha = shift }, "2")
        Icon(
            Icons.Default.TouchApp, null, Modifier.size(44.dp).align(Alignment.BottomCenter)
                .graphicsLayer { translationX = (0.5f - shift) * 300f; translationY = -10f }, tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Page(modifier: Modifier, number: String) {
    Column(
        modifier.fillMaxWidth(0.8f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(number, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary)
        repeat(3) { i ->
            Box(
                Modifier.padding(top = 8.dp).fillMaxWidth(if (i == 2) 0.6f else 1f).height(8.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun DemoSelect() {
    var selected by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        VerseText(selected = selected, onClick = { selected = !selected }, modifier = Modifier.pressScale(0.98f))
        Spacer(Modifier.height(14.dp))
        Column(Modifier.height(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(selected, enter = fadeIn() + scaleIn(initialScale = 0.8f) + slideInVertically { it }, exit = fadeOut()) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        listOf(Icons.Default.Star, Icons.Default.Brush, Icons.Default.EditNote, Icons.Default.CopyAll, Icons.Default.Share, Icons.Default.Close).forEach {
                            Icon(it, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoFavorite() {
    var fav by remember { mutableStateOf(false) }
    val flash = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(fav) { if (fav) { flash.snapTo(1f); flash.animateTo(0f, tween(900)) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerseText(flash = flash.value, suffix = if (fav) "  ★" else "")
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface) {
            FavoriteStarButton(isFavorite = fav, onClick = { fav = !fav }, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun DemoHighlight() {
    var color by remember { mutableStateOf<Color?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerseText(bg = color?.copy(alpha = 0.55f) ?: Color.Transparent)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            COLORS.forEach { c ->
                val s by animateFloatAsState(if (color == c) 1.25f else 1f, spring(dampingRatio = 0.5f), label = "dot")
                Box(
                    Modifier.size(38.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).background(c).clickable { color = c },
                    contentAlignment = Alignment.Center
                ) { if (color == c) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = Color.Black.copy(alpha = 0.7f)) }
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { color = null },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Close, null, Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun DemoNote(active: Boolean) {
    var open by remember { mutableStateOf(false) }
    val full = stringResource(R.string.tut_demo_note_text)
    var typed by remember { mutableIntStateOf(0) }
    LaunchedEffect(open, active) {
        if (open && active) { typed = 0; while (typed < full.length) { delay(45); typed++ } } else if (!open) typed = 0
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerseText(suffix = if (open) "  ✎" else "")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).pressScale(0.9f).clickable { open = !open }) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.onPrimary) }
            }
            AnimatedVisibility(open, enter = fadeIn() + scaleIn(initialScale = 0.7f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f))) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                    Text(full.take(typed) + if (typed < full.length) "▍" else "", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DemoListen() {
    var playing by remember { mutableStateOf(false) }
    val t = rememberInfiniteTransition(label = "eq")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerseText(bg = if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp).pressScale(0.9f).clickable { playing = !playing }) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(playing, label = "pp", transitionSpec = { fadeIn() togetherWith fadeOut() }) {
                        Icon(if (it) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Row(Modifier.height(36.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(420, 560, 380, 640, 480).forEach { d ->
                    val h by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(d, easing = LinearEasing), RepeatMode.Reverse), label = "bar")
                    Box(
                        Modifier.width(6.dp).fillMaxHeight(if (playing) h else 0.2f).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoSearch(active: Boolean) {
    val word = stringResource(R.string.tut_demo_search_word)
    var typed by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        while (active) {
            typed = 0
            while (typed < word.length) { delay(220); typed++ }
            delay(2400)
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(word.take(typed) + "▍", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.tut_chip_all, R.string.tut_chip_ot, R.string.tut_chip_nt).forEachIndexed { i, r ->
                Surface(shape = RoundedCornerShape(50), color = if (i == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Text(stringResource(r), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(typed >= word.length, enter = fadeIn() + slideInVertically { it / 2 }) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Text(stringResource(R.string.tut_demo_ref), Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private data class DemoBible(val code: String, val lang: String)

@Composable
private fun DemoBibles() {
    val list = listOf(DemoBible("RVR60", "Español"), DemoBible("KJV", "English"), DemoBible("LSG", "Français"))
    var sel by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEachIndexed { i, b ->
            val c by animateColorAsState(if (sel == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tween(250), label = "bc")
            Surface(Modifier.fillMaxWidth().pressScale(0.98f).clickable { sel = i }, shape = RoundedCornerShape(16.dp), color = c) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(b.code, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(b.lang, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    AnimatedVisibility(sel == i, enter = scaleIn()) { Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        AnimatedContent(list[sel].lang, label = "applang", transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() }) { lang ->
            Text("🌐 $lang", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DemoStreak(active: Boolean) {
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) { target = 0; if (active) { delay(250); target = 7 } }
    val n by animateIntAsState(target, tween(1400, easing = FastOutSlowInEasing), label = "n")
    val t = rememberInfiniteTransition(label = "flame")
    val s by t.animateFloat(0.92f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(64.dp).graphicsLayer { scaleX = s; scaleY = s }, tint = Color(0xFFFF7A1A))
        Text("$n", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.tut_demo_days), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(7) { i ->
                val filled = i < n
                val c by animateColorAsState(if (filled) Color(0xFFFF7A1A) else MaterialTheme.colorScheme.outlineVariant, tween(200), label = "d")
                Box(Modifier.size(22.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                    if (filled) Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.White)
                }
            }
        }
    }
}
