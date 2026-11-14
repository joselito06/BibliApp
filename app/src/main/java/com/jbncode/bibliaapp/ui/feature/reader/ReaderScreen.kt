package com.jbncode.bibliaapp.ui.feature.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jbncode.bibliaapp.ui.components.appearOnce
import com.jbncode.bibliaapp.ui.components.pressScale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jbncode.bibliaapp.R
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.ui.components.ScreenHeader
import com.jbncode.bibliaapp.view.Routes
import java.util.Locale
import com.jbncode.bibliaapp.core.share.ShareLinks
import com.jbncode.bibliaapp.core.share.VerseTextFormatter
import androidx.hilt.navigation.compose.hiltViewModel
import com.jbncode.bibliaapp.data.user.ItemType
import com.jbncode.bibliaapp.data.user.UserItemEntity
import com.jbncode.bibliaapp.domain.bible.Book
import com.jbncode.bibliaapp.domain.bible.Verse
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    mainViewmodel: MainViewmodel,
    reader: ReaderViewModel,
    playback: PlaybackViewModel,
    navigation: NavHostController,
    annotations: AnnotationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val books by reader.books.collectAsState()
    val position by reader.position.collectAsState()
    val activeFile by reader.activeFile.collectAsState()
    val info by reader.info.collectAsState()
    val speaking by playback.speakingRef.collectAsState()
    val linkVerse by reader.pendingVerse.collectAsState()
    val isSpeaking by playback.isSpeaking.collectAsState()
    val isPaused by playback.isPaused.collectAsState()
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerBook by remember { mutableStateOf<String?>(null) }   // null = lista de libros
    var noteTarget by remember { mutableStateOf<NoteTarget?>(null) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    val book = books.firstOrNull { it.osis == position.book }
    val chapterItems by remember(position.book, position.chapter) { annotations.chapter(position.book, position.chapter) }
        .collectAsState(initial = emptyList())
    val favoriteVerses = remember(chapterItems) { chapterItems.filter { it.type == ItemType.FAVORITE }.map { it.verse }.toSet() }
    val allSelectedFavorite = selected.isNotEmpty() && selected.all { it in favoriteVerses }
    val ttsState by playback.state.collectAsState()
    val rate by playback.speechRate.collectAsState()
    val chapterVerses by produceState(emptyList(), activeFile, position.book, position.chapter) {
        value = runCatching { reader.loadChapter(position.book, position.chapter) }.getOrDefault(emptyList())
    }
    // Progreso dentro del capítulo que se está leyendo.
    val progress = if (ttsState.chapter == position.chapter && ttsState.verse > 0 && chapterVerses.isNotEmpty())
        (chapterVerses.indexOfFirst { it.verse == ttsState.verse } + 1).toFloat() / chapterVerses.size else 0f

    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    LaunchedEffect(Unit) { mainViewmodel.showTopAppBar(false); mainViewmodel.showBottomBar(true) }
    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
    LaunchedEffect(linkVerse) { linkVerse?.let { selected = setOf(it) } }

    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> reader.onResumed()
                Lifecycle.Event.ON_PAUSE -> reader.onPaused()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); reader.onPaused(); mainViewmodel.showTopAppBarAnimation(false) }
    }

    // Al desplazar hacia abajo se oculta la barra inferior de navegación.
    val scrollWatcher = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (abs(available.y) > 8f) mainViewmodel.showTopAppBarAnimation(available.y < 0)
                return Offset.Zero
            }
        }
    }

    if (book == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val chapters = maxOf(book.chapters, 1)

    Box(Modifier.fillMaxSize().nestedScroll(scrollWatcher)) {
        Column(Modifier.fillMaxSize()) {
            ReaderHeader(
                bookName = book.name, versionTitle = info.title, chapter = position.chapter,
                onPickBook = { pickerBook = null; showPicker = true },
                onPickChapter = { pickerBook = position.book; showPicker = true },
                onSearch = { navigation.navigate(Routes.SearchScreen.route) },
                onVersions = { navigation.navigate(Routes.BibleSelectionScreen.route) }
            )

            key(book.osis, activeFile) {
                val pagerState = rememberPagerState(
                    initialPage = (position.chapter - 1).coerceIn(0, chapters - 1),
                    pageCount = { chapters }
                )
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { reader.setChapter(it + 1); selected = emptySet() }
                }
                LaunchedEffect(position.chapter) {
                    val target = position.chapter - 1
                    if (target in 0 until chapters && pagerState.currentPage != target) pagerState.animateScrollToPage(target)
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), key = { it }) { page ->
                    ChapterPage(
                        reader, annotations, book, page + 1, activeFile, selected, speaking,
                        linkVerse = if (page + 1 == position.chapter) linkVerse else null,
                        onLinkShown = reader::consumePendingVerse
                    ) { n ->
                        selected = if (n in selected) selected - n else selected + n
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.85f) + slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { it },
                exit = fadeOut() + scaleOut(targetScale = 0.9f) + slideOutVertically { it }
            ) {
                AnnotationBar(
                    allFavorite = allSelectedFavorite,
                    onFavorite = { annotations.toggleFavorite(book.osis, position.chapter, selected) },
                    onHighlight = { c -> annotations.setHighlight(book.osis, position.chapter, selected, c); selected = emptySet() },
                    onNote = {
                        scope.launch {
                            val first = selected.min()
                            noteTarget = NoteTarget(first, selected.max(), annotations.noteAt(book.osis, position.chapter, first))
                        }
                    },
                    onCopy = { scope.launch { copyOrShare(context, reader, book, position.chapter, selected, info.code, share = false) } },
                    onShare = { scope.launch { copyOrShare(context, reader, book, position.chapter, selected, info.code, share = true) } },
                    onClear = { selected = emptySet() }
                )
            }
            val speakingNow = isSpeaking || isPaused
            if (speakingNow) {
                PlayerCard(
                    title = "${ttsState.bookName.ifBlank { book.name }} ${ttsState.chapter.takeIf { it > 0 } ?: position.chapter}",
                    verse = ttsState.verse, progress = progress, voiceTag = ttsState.languageTag, rate = rate, paused = isPaused,
                    onToggle = { if (isPaused) playback.resume() else playback.pause() },
                    onPrev = { playback.skip(-1) }, onNext = { playback.skip(1) }, onStop = playback::stop,
                    onSettings = { showVoiceSheet = true }
                )
            }
            ChapterBar(
                chapter = position.chapter, showPlay = !speakingNow,
                onPrev = reader::previousChapter, onNext = reader::nextChapter,
                onPick = { pickerBook = position.book; showPicker = true },
                onPlay = {
                    val from = selected.minOrNull() ?: playback.resumeVerse(position.book, position.chapter)
                    if (selected.isEmpty() && from > 1) {
                        Toast.makeText(context, context.getString(R.string.reader_resume_from, from), Toast.LENGTH_SHORT).show()
                    }
                    playback.play(position.book, position.chapter, from)
                }
            )
        }
    }

    if (showVoiceSheet) VoiceSettingsSheet(playback) { showVoiceSheet = false }

    noteTarget?.let { t ->
        NoteDialog(
            title = "${book.name} ${position.chapter}:${t.from}" + if (t.to > t.from) "-${t.to}" else "",
            existing = t.existing,
            onSave = { text -> annotations.saveNote(book.osis, position.chapter, t.from, t.to, text, t.existing); noteTarget = null; selected = emptySet() },
            onDelete = { t.existing?.let { annotations.delete(it.id) }; noteTarget = null },
            onDismiss = { noteTarget = null }
        )
    }

    if (showPicker) {
        BookChapterSheet(
            books, info.ntStart, position.book,
            onPick = { b, c -> reader.open(b, c); showPicker = false },
            onDismiss = { showPicker = false },
            initialBookOsis = pickerBook
        )
    }
}

private suspend fun copyOrShare(
    context: Context, reader: ReaderViewModel, book: Book, chapter: Int,
    selected: Set<Int>, versionCode: String, share: Boolean
) {
    val verses = reader.loadChapter(book.osis, chapter).filter { it.verse in selected }
    if (verses.isEmpty()) return
    val footer = if (share) {
        context.getString(R.string.share_open_in_app) + " " + ShareLinks.PLAY_STORE
    } else null
    val text = VerseTextFormatter.format(book.name, chapter, verses, versionCode, footer)
    if (share) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.bible_share_title)))
    } else {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.bible_copy_label), text))
        Toast.makeText(context, context.getString(R.string.bible_copy_toast), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ChapterPage(
    reader: ReaderViewModel, annotations: AnnotationsViewModel, book: Book, chapter: Int, activeFile: String,
    selected: Set<Int>, speaking: Pair<Int, Int>?, linkVerse: Int? = null, onLinkShown: () -> Unit = {},
    onToggle: (Int) -> Unit
) {
    val verses by produceState<List<Verse>?>(null, activeFile, book.osis, chapter) {
        value = try { reader.loadChapter(book.osis, chapter) } catch (_: Exception) { emptyList() }
    }
    val list = verses
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val items by remember(book.osis, chapter) { annotations.chapter(book.osis, chapter) }.collectAsState(initial = emptyList())
    val favorites = remember(items) { items.filter { it.type == ItemType.FAVORITE }.map { it.verse }.toSet() }
    val highlights = remember(items) { items.filter { it.type == ItemType.HIGHLIGHT && it.color != null }.associate { it.verse to it.color!! } }
    val noted = remember(items) { items.filter { it.type == ItemType.NOTE }.flatMap { it.verse..it.verseEnd }.toSet() }
    val state = rememberLazyListState()
    val speakingVerse = speaking?.takeIf { it.first == chapter }?.second
    // Un anuncio discreto al final de cada tercer capítulo (nunca interrumpe la lectura).
    val showAd = remember(book.osis, chapter) { com.jbncode.bibliaapp.utils.AdPacing.shouldShowInline("${book.osis}:$chapter") }
    LaunchedEffect(linkVerse, list.size) {
        if (linkVerse != null && list.isNotEmpty()) {
            val idx = list.indexOfFirst { it.verse == linkVerse }
            if (idx >= 0) try { state.scrollToItem(idx + 1) } catch (_: Exception) { }
            onLinkShown()
        }
    }
    LaunchedEffect(speakingVerse) {
        if (speakingVerse != null) {
            val idx = list.indexOfFirst { it.verse == speakingVerse }
            if (idx >= 0) try { state.animateScrollToItem(idx + 1) } catch (_: Exception) { }
        }
    }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 200.dp)) {
        item {
            Text(
                chapter.toString(), Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary
            )
        }
        itemsIndexed(list, key = { _, v -> v.verse }) { _, v ->
            VerseRow(v, v.verse in selected, v.verse == speakingVerse, highlights[v.verse], v.verse in favorites, v.verse in noted) { onToggle(v.verse) }
        }
        if (showAd) item(key = "inline-ad") {
            com.jbncode.bibliaapp.view.components.InlineNativeAd(Modifier.padding(top = 28.dp))
        }
    }
}

@Composable
private fun VerseRow(v: Verse, selected: Boolean, speaking: Boolean, highlight: Int?, favorite: Boolean, hasNote: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val bgTarget = when {
        selected -> MaterialTheme.colorScheme.tertiaryContainer
        speaking -> primary.copy(alpha = 0.14f)
        highlight != null -> Color(highlight).copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val bg by animateColorAsState(bgTarget, tween(220), label = "verseBg")
    val rowScale by animateFloatAsState(if (selected) 1.012f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "verseScale")
    // Destello dorado cuando el versículo pasa a ser favorito.
    val flash = remember { Animatable(0f) }
    var firstFav by remember { mutableStateOf(true) }
    LaunchedEffect(favorite) {
        if (firstFav) { firstFav = false; return@LaunchedEffect }
        if (favorite) { flash.snapTo(1f); flash.animateTo(0f, tween(900)) }
    }
    Column(Modifier.fillMaxWidth()) {
        v.heading?.let {
            Text(it, Modifier.padding(top = 16.dp, bottom = 6.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = primary)
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 12.sp, baselineShift = BaselineShift.Superscript, color = primary, fontWeight = FontWeight.Bold)) { append("${v.verse} ") }
                append(v.text)
                if (favorite) withStyle(SpanStyle(color = primary)) { append("  ★") }
                if (hasNote) withStyle(SpanStyle(color = primary)) { append("  ✎") }
            },
            style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 30.sp),
            color = if (speaking) primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (speaking) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth()
                .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .drawBehind { if (flash.value > 0f) drawRect(FAVORITE_GOLD.copy(alpha = 0.35f * flash.value)) }
                .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private data class NoteTarget(val from: Int, val to: Int, val existing: UserItemEntity?)

private val HIGHLIGHT_COLORS = listOf(0xFFFFF176, 0xFFA5D6A7, 0xFF90CAF9, 0xFFF48FB1).map { it.toInt() }

@Composable
private fun AnnotationBar(
    allFavorite: Boolean,
    onFavorite: () -> Unit, onHighlight: (Int?) -> Unit, onNote: () -> Unit,
    onCopy: () -> Unit, onShare: () -> Unit, onClear: () -> Unit
) {
    var showColors by remember { mutableStateOf(false) }
    val bg = MaterialTheme.colorScheme.inverseSurface
    val fg = MaterialTheme.colorScheme.inverseOnSurface
    Column(Modifier.padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = showColors,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.CenterHorizontally) + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally)
        ) {
            Surface(Modifier.padding(bottom = 8.dp), shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HIGHLIGHT_COLORS.forEach { c ->
                        Box(Modifier.size(32.dp).pressScale(0.85f).clip(CircleShape).background(Color(c)).clickable { onHighlight(c); showColors = false })
                    }
                    IconButton(onClick = { onHighlight(null); showColors = false }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.action_remove_highlight))
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FavoriteButton(allFavorite, onFavorite, Modifier.appearOnce(0, 30))
                IconButton(onClick = { showColors = !showColors }, Modifier.appearOnce(1, 30)) { Icon(Icons.Rounded.Brush, stringResource(R.string.action_highlight)) }
                IconButton(onClick = onNote, Modifier.appearOnce(2, 30)) { Icon(Icons.Rounded.EditNote, stringResource(R.string.action_note)) }
                IconButton(onClick = onCopy, Modifier.appearOnce(3, 30)) { Icon(Icons.Rounded.CopyAll, stringResource(R.string.bible_action_copy)) }
                IconButton(onClick = onShare, Modifier.appearOnce(4, 30)) { Icon(Icons.Rounded.Share, stringResource(R.string.bible_action_share)) }
                IconButton(onClick = onClear, Modifier.appearOnce(5, 30)) { Icon(Icons.Rounded.Close, stringResource(R.string.reader_clear_selection)) }
            }
        }
    }
}

private val FAVORITE_GOLD = Color(0xFFFFC107)

/** Estrella con rebote, giro, destello de partículas y vibración al marcar como favorito. */
@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
    val tint by animateColorAsState(if (isFavorite) FAVORITE_GOLD else LocalContentColor.current, tween(250), label = "favTint")
    IconButton(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        modifier = modifier.drawBehind {
            val p = burst.value
            if (p > 0f && p < 1f) {
                repeat(8) { i ->
                    val a = Math.toRadians(i * 45.0)
                    val r = 14.dp.toPx() + p * 22.dp.toPx()
                    drawCircle(
                        FAVORITE_GOLD.copy(alpha = 1f - p),
                        radius = (4f * (1f - p) + 1f).dp.toPx(),
                        center = Offset(center.x + cos(a).toFloat() * r, center.y + sin(a).toFloat() * r)
                    )
                }
            }
        }
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            stringResource(R.string.action_favorite),
            tint = tint,
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = rotation.value }
        )
    }
}

@Composable
private fun NoteDialog(title: String, existing: UserItemEntity?, onSave: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var text by remember(existing) { mutableStateOf(existing?.text ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.note_hint)) }
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.note_save)) } },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.note_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
            }
        }
    )
}

@Composable
private fun ReaderHeader(
    bookName: String, versionTitle: String, chapter: Int,
    onPickBook: () -> Unit, onPickChapter: () -> Unit, onSearch: () -> Unit, onVersions: () -> Unit
) {
    ScreenHeader {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    versionTitle, Modifier.clickable(onClick = onVersions), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
                )
                Row(
                    Modifier.clickable(onClick = onPickBook),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        bookName, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
                    )
                    // Señal de que el nombre del libro se puede tocar.
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
            }
            IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.navigarion_bar_search)) }
            AssistChip(
                onClick = onPickChapter,
                label = { Text("${stringResource(R.string.bible_chapter_label)} $chapter") },
                trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** Solo aparece mientras se lee en voz alta. */
@Composable
private fun PlayerCard(
    title: String, verse: Int, progress: Float, voiceTag: String, rate: Float, paused: Boolean,
    onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onStop: () -> Unit, onSettings: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = onToggle, modifier = Modifier.size(46.dp)) {
                    Icon(
                        if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        stringResource(if (paused) R.string.bible_notification_play else R.string.bible_notification_pause)
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(title, maxLines = 1, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.reader_reading_verse, verse), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPrev) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.reader_prev_verse)) }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.reader_next_verse)) }
                IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, stringResource(R.string.bible_notification_stop)) }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                if (voiceTag.isNotBlank()) InfoPill(stringResource(R.string.reader_voice_chip, voiceTag))
                InfoPill(String.format(Locale.US, "%.1fx", rate))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Tune, stringResource(R.string.tts_voice_title)) }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
    }
}

@Composable
private fun ChapterBar(chapter: Int, showPlay: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onPick: () -> Unit, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(Modifier.weight(1f), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null) }
                TextButton(onClick = onPick) { Text("${stringResource(R.string.bible_chapter_label)} $chapter", fontWeight = FontWeight.SemiBold) }
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null) }
            }
        }
        if (showPlay) {
            FilledIconButton(onClick = onPlay, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.bible_notification_play))
            }
        }
    }
}
