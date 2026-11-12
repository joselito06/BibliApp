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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    navigation: NavHostController
) {
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

    Scaffold(Modifier.fillMaxSize(), bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WelcomeSection()
            StreakCard(stats)
            StatsRow(stats)
            ContinueReadingCard(
                bookName = book?.name ?: position.book, chapter = position.chapter, progress = bookProgress,
                onContinueClick = {
                    val go = { navigation.navigate(Routes.BibleScreen.route) }
                    if (activity != null) InterstitialAdManager.show(activity) { go() } else go()
                }
            )
            DailyVerseCard(versesDaily, onShareClick = { shareVerse(context, versesDaily) }, onReadAloudClick = { })
            com.jbncode.bibliaapp.view.components.NativeAdCard()
            QuickActionsGrid(
                onBibleClick = { navigation.navigate(Routes.BibleScreen.route) },
                onLibraryClick = { navigation.navigate(Routes.LibraryScreen.route) },
                onNotificationsClick = { navigation.navigate(Routes.NotificationScreen.route) },
                onDownloadClick = { navigation.navigate(Routes.BibleDownloadScreen.route) }
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
private fun WelcomeSection() {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
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
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(Modifier.weight(1f), "${stats.chaptersRead}", stringResource(R.string.stats_chapters_label))
        StatTile(Modifier.weight(1f), formatDuration(stats.totalSeconds), stringResource(R.string.stats_time_label))
        StatTile(Modifier.weight(1f), "${stats.biblePercent}%", stringResource(R.string.home_bible_read))
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun DailyVerseCard(verse: VersesDaily, onShareClick: () -> Unit, onReadAloudClick: () -> Unit) {
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
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.daily_verse_listen))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsGrid(onBibleClick: () -> Unit, onLibraryClick: () -> Unit, onNotificationsClick: () -> Unit, onDownloadClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.quick_actions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionItem(Icons.Default.Book, stringResource(R.string.quick_action_read), onBibleClick)
                QuickActionItem(Icons.Default.Bookmark, stringResource(R.string.quick_action_library), onLibraryClick)
                QuickActionItem(Icons.Default.Notifications, stringResource(R.string.quick_action_notifications), onNotificationsClick)
                QuickActionItem(Icons.Default.Download, stringResource(R.string.quick_action_downloads), onDownloadClick)
            }
        }
    }
}

@Composable
private fun QuickActionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(80.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
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
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${verse.verse_content}\n\n- ${verse.verse_direction}") }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_verse_with)))
}
