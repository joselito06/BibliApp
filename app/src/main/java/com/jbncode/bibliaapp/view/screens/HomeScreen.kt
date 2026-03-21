package com.jbncode.bibliaapp.view.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import com.jbncode.bibliaapp.domain.model.BookRecomendation
import com.jbncode.bibliaapp.domain.model.ReadingStats
import com.jbncode.bibliaapp.utils.InterstitialAdManager
import com.jbncode.bibliaapp.view.Routes
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.viewmodel.BibleViewmodel
import com.jbncode.bibliaapp.view.viewmodel.HomeViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import com.jbncode.bibliaapp.view.viewmodel.VersesViewmodel
import java.util.Calendar

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomePreview(){

}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewmodel : HomeViewmodel,
    versesViewmodel: VersesViewmodel,
    bibleViewmodel: BibleViewmodel,
    mainViewmodel: MainViewmodel,
    navigation : NavHostController
){

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(true)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }

    homeViewmodel.OnGet()

    val scrollState = rememberScrollState()
    val versesDaily by versesViewmodel.verseOfTheDay.collectAsState()
    val currentBook by bibleViewmodel.libro.collectAsState()
    val currentChapter by bibleViewmodel.capitulo.collectAsState()
    val readingStats by homeViewmodel.readingStats.collectAsState()
    val recommendations by homeViewmodel.recommendations.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    Scaffold(
        Modifier.fillMaxSize(),
        bottomBar = {
            BannerAd(stringResource(R.string.banner_ad_id))
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SALUDO PERSONALIZADO
            WelcomeSection()

            // 2. VERSO DEL DÍA MEJORADO
            DailyVerseCard(
                verse = versesDaily,
                onShareClick = { shareVerse(context, versesDaily) },
                onReadAloudClick = { /* TODO: Implementar TTS para verso del día */ }
            )

            com.jbncode.bibliaapp.view.components.NativeAdCard()

            // 3. CONTINUAR LECTURA
            ContinueReadingCard(
                bookName = currentBook,
                chapter = currentChapter,
                onContinueClick = {
                    if(activity != null){
                        InterstitialAdManager.show(activity){
                            navigation.navigate(Routes.BibleScreen.route)
                        }
                    }else{
                        navigation.navigate(Routes.BibleScreen.route)
                    }
                }
            )

            // 4. ACCESOS RÁPIDOS
            QuickActionsGrid(
                onBibleClick = { navigation.navigate(Routes.BibleScreen.route) },
                onSearchClick = { /* TODO: Implementar búsqueda */ },
                onNotificationsClick = { navigation.navigate(Routes.NotificationScreen.route) },
                onDownloadClick = { navigation.navigate(Routes.BibleDownloadScreen.route) }
            )

            // 5. PROGRESO DE LECTURA
            ReadingProgressCard(stats = readingStats)

            // 6. RECOMENDACIONES
            RecommendationsCard(
                recommendations = recommendations,
                onExploreClick = { bookOsis ->
                    bibleViewmodel.getBook(bookOsis)
                    navigation.navigate(Routes.BibleScreen.route) }
            )

            // 7. ESTADÍSTICAS RÁPIDAS
            QuickStatsCard(stats = readingStats)

            // Espacio adicional para el bottom navigation
            Spacer(modifier = Modifier.height(80.dp))
        }
    }


}

// 1. SECCIÓN DE BIENVENIDA
@Composable
private fun WelcomeSection() {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (currentHour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.greeting_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            when (currentHour) {
                in 5..11 -> Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                in 12..17 -> Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                else -> Icon(
                    imageVector = Icons.Default.ModeNight,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}

// 2. VERSO DEL DÍA MEJORADO
@Composable
private fun DailyVerseCard(
    verse: VersesDaily,
    onShareClick: () -> Unit,
    onReadAloudClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_verse_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (verse.verse_content?.isNotEmpty() == true) {
                Text(
                    text = verse.verse_content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                verse.verse_direction?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReadAloudClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.daily_verse_listen))
                    }

                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.daily_verse_share))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// 3. CONTINUAR LECTURA
@Composable
private fun ContinueReadingCard(
    bookName: String,
    chapter: Int,
    onContinueClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinueClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.continue_reading_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.continue_reading_chapter, bookName, chapter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// 4. ACCESOS RÁPIDOS
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsGrid(
    onBibleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    icon = Icons.Default.Book,
                    title = stringResource(R.string.quick_action_read),
                    onClick = onBibleClick
                )

                QuickActionItem(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.quick_action_search),
                    onClick = onSearchClick
                )

                QuickActionItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.quick_action_notifications),
                    onClick = onNotificationsClick
                )

                QuickActionItem(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.quick_action_downloads),
                    onClick = onDownloadClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 5. PROGRESO DE LECTURA
@Composable
private fun ReadingProgressCard(stats: ReadingStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reading_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(stats.progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { stats.progressPercentage },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.reading_progress_books, stats.booksRead, stats.totalBooks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (stats.chaptersRead > 0) {
                Text(
                    text = stringResource(R.string.reading_progress_chapters, stats.chaptersRead),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 6. RECOMENDACIONES
@Composable
private fun RecommendationsCard(
    recommendations: List<BookRecomendation>,
    onExploreClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.recommendations_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (recommendations.isEmpty()) {
                Text(
                    text = stringResource(R.string.recommendations_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                recommendations.forEach { recommendation ->
                    RecommendationItem(
                        recommendation = recommendation,
                        onClick = { onExploreClick(recommendation.bookOsis) }
                    )

                    if (recommendation != recommendations.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    recommendation: BookRecomendation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (recommendation.priority) {
                1 -> Icons.Default.Star
                2 -> Icons.Default.AutoStories
                else -> Icons.AutoMirrored.Filled.MenuBook
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when (recommendation.priority) {
                1 -> MaterialTheme.colorScheme.primary
                2 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// 7. ESTADÍSTICAS RÁPIDAS
@Composable
private fun QuickStatsCard(stats: ReadingStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.readingStreak}",
                    label = stringResource(R.string.stats_streak_label)
                )

                StatItem(
                    value = "${stats.chaptersRead}",
                    label = stringResource(R.string.stats_chapters_label)
                )

                StatItem(
                    value = stats.totalReadingTimeFormatted,
                    label = stringResource(R.string.stats_time_label)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

// FUNCIÓN DE COMPARTIR VERSO
private fun shareVerse(context: Context, verse: VersesDaily) {
    val shareText = "${verse.verse_content}\n\n- ${verse.verse_direction}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooserTitle = context.getString(R.string.share_verse_with)
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}