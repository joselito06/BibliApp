package com.jbncode.bibliaapp.view.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import com.jbncode.bibliaapp.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.jbncode.bibliaapp.ui.components.pressScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.data.user.HomeStats
import com.jbncode.bibliaapp.domain.model.BookRecomendation
import com.jbncode.bibliaapp.ui.feature.reader.ReaderViewModel
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.HomeViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Calendar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewmodel: HomeViewmodel,
    versesViewmodel: VersesViewmodel,
    readerViewmodel: ReaderViewModel,
    mainViewmodel: MainViewmodel,
    navigation: NavHostController,
    playbackViewmodel: com.jbncode.bibliaapp.ui.feature.reader.PlaybackViewModel? = null
) {
    // Al volver a la app (otro día) o al pasar la medianoche, refresca versículo y saludo.
    var dayTick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { versesViewmodel.refreshIfNewDay(); dayTick++ }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(dayTick) {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 2) }
        kotlinx.coroutines.delay(next.timeInMillis - now.timeInMillis)
        versesViewmodel.refreshIfNewDay(); dayTick++
    }
    val speakingDaily by (playbackViewmodel?.isSpeakingText ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    LaunchedEffect(true) { mainViewmodel.showTopAppBar(true); mainViewmodel.showBottomBar(true) }

    val versesDaily by versesViewmodel.verseOfTheDay.collectAsState()
    val position by readerViewmodel.position.collectAsState()
    val books by readerViewmodel.books.collectAsState()
    val stats by homeViewmodel.stats.collectAsState()
    val recommendations by homeViewmodel.recommendations.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val book = books.firstOrNull { it.osis == position.book }
    val bookChapters = book?.chapters ?: 0
    val bookProgress = if (bookChapters > 0) (stats.chaptersByBook[position.book] ?: 0).toFloat() / bookChapters else 0f

    Scaffold(
        Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WelcomeSection(dayTick)
            StreakCard(stats)
            StatsRow(stats)
            ContinueReadingCard(
                bookName = book?.name ?: position.book, chapter = position.chapter, progress = bookProgress,
                onContinueClick = {
                    val go = { navigation.navigate(Routes.BibleScreen.route) }
                    if (activity != null) InterstitialAdManager.show(activity) { go() } else go()
                }
            )
            DailyVerseCard(versesDaily, onShareClick = { shareVerse(context, versesDaily) }, speaking = speakingDaily, onReadAloudClick = {
                playbackViewmodel?.let { vm ->
                    if (speakingDaily) vm.stop()
                    else vm.speakText(listOfNotNull(versesDaily.verse_content, versesDaily.verse_direction).joinToString(". "))
                }
            })
            com.jbncode.bibliaapp.view.components.NativeAdCard()
            QuickActionsGrid(
                onBibleClick = { navigation.navigate(Routes.BibleScreen.route) },
                onLibraryClick = { navigation.navigate(Routes.LibraryScreen.route) },
                onNotificationsClick = { navigation.navigate(Routes.NotificationScreen.route) },
                onDownloadClick = { navigation.navigate(Routes.BibleSelectionScreen.route) }
            )
            RecommendationsCard(recommendations) { osis ->
                readerViewmodel.open(osis)
                navigation.navigate(Routes.BibleScreen.route)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WelcomeSection(tick: Int) {
    val hour = remember(tick) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    val icon = when (hour) { in 5..11 -> Icons.Default.Coffee; in 12..17 -> Icons.Default.WbSunny; else -> Icons.Default.ModeNight }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.greeting_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StreakCard(stats: HomeStats) {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = null, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${stats.streak}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.stats_streak_label), Modifier.padding(start = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(stringResource(R.string.home_best_streak, stats.bestStreak), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                stats.last7.forEachIndexed { i, read ->
                    val day = today.minusDays((6 - i).toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.dayOfWeek.getDisplayName(DayStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Box(
                            Modifier.padding(top = 4.dp).size(32.dp).clip(CircleShape)
                                .background(if (read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) { if (read) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                    }
                }
            }
            Text(
                stringResource(if (stats.readToday) R.string.home_today_done else R.string.home_today_pending),
                Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60
    return when { h == 0 -> "${m} min"; m == 0 -> "$h h"; else -> "$h h $m min" }
}

@Composable
private fun StatsRow(stats: HomeStats) {
    // height(IntrinsicSize.Min): todas las tarjetas igualan a la más alta.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(Modifier.weight(1f), "${stats.chaptersRead}", stringResource(R.string.stats_chapters_label))
        StatTile(Modifier.weight(1f), formatDuration(stats.totalSeconds), stringResource(R.string.stats_time_label))
        StatTile(Modifier.weight(1f), "${stats.biblePercent}%", stringResource(R.string.home_bible_read))
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String) {
    Card(modifier.fillMaxHeight(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            Modifier.padding(14.dp).fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ContinueReadingCard(bookName: String, chapter: Int, progress: Float, onContinueClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onContinueClick() }, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.continue_reading_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.continue_reading_chapter, bookName, chapter), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        }
    }
}

@Composable
private fun DailyVerseCard(verse: VersesDaily, onShareClick: () -> Unit, speaking: Boolean, onReadAloudClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.daily_verse_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.AutoStories, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            if (verse.verse_content?.isNotEmpty() == true) {
                Text(verse.verse_content, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif, lineHeight = 26.sp, modifier = Modifier.padding(vertical = 8.dp))
                verse.verse_direction?.let {
                    Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReadAloudClick, modifier = Modifier.weight(1f)) {
                        Icon(if (speaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (speaking) R.string.daily_verse_stop else R.string.daily_verse_listen))
                    }
                    OutlinedButton(onClick = onShareClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.daily_verse_share))
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(onBibleClick: () -> Unit, onLibraryClick: () -> Unit, onNotificationsClick: () -> Unit, onDownloadClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.quick_actions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionItem(Icons.Default.Book, stringResource(R.string.quick_action_read), cs.primary, cs.onPrimary, onBibleClick, Modifier.weight(1f))
            QuickActionItem(Icons.Default.Bookmark, stringResource(R.string.quick_action_library), cs.tertiaryContainer, cs.onTertiaryContainer, onLibraryClick, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionItem(Icons.Default.Notifications, stringResource(R.string.quick_action_notifications), cs.secondaryContainer, cs.onSecondaryContainer, onNotificationsClick, Modifier.weight(1f))
            QuickActionItem(Icons.Default.Download, stringResource(R.string.quick_action_downloads), cs.primaryContainer, cs.onPrimaryContainer, onDownloadClick, Modifier.weight(1f))
        }
    }
}

/** Botón rápido: tarjeta con borde + icono en círculo de color. Se ve bien en todos los temas. */
@Composable
private fun QuickActionItem(icon: ImageVector, title: String, iconBg: androidx.compose.ui.graphics.Color, iconFg: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(68.dp).pressScale(0.97f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = iconFg)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecommendationsCard(recommendations: List<BookRecomendation>, onExploreClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.recommendations_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            if (recommendations.isEmpty()) {
                Text(stringResource(R.string.recommendations_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            } else {
                recommendations.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { onExploreClick(r.bookOsis) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (r.priority == 1) Icons.Default.Star else Icons.Default.AutoStories, null, Modifier.size(20.dp),
                            tint = if (r.priority == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(r.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

private fun shareVerse(context: Context, verse: VersesDaily) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${verse.verse_content}\n\n- ${verse.verse_direction}\n\n" + context.getString(R.string.share_open_in_app) + " " + com.jbncode.bibliaapp.core.share.ShareLinks.PLAY_STORE) }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_verse_with)))
}
