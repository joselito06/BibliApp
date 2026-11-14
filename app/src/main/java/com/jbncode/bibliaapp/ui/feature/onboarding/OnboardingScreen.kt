package com.jbncode.bibliaapp.ui.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.components.appearOnce
import com.jbncode.bibliaapp.domain.model.AppLanguage
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel

@Composable
fun OnboardingScreen(
    mainViewmodel: MainViewmodel,
    onFinished: (needsRestart: Boolean) -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { mainViewmodel.showTopAppBar(false); mainViewmodel.showBottomBar(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.onNotificationsResult(it)
    }
    BackHandler(enabled = ui.step in 1..2 && !ui.downloading) { vm.back() }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(
                        if (i <= ui.step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
        // Volver al paso anterior (también funciona el gesto/botón atrás del sistema).
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = ui.step in 1..2 && !ui.downloading,
                enter = fadeIn() + slideInHorizontally { -it / 2 },
                exit = fadeOut() + slideOutHorizontally { -it / 2 }
            ) {
                TextButton(onClick = vm::back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, Modifier.size(20.dp))
                    Text(stringResource(R.string.onboarding_back), Modifier.padding(start = 6.dp))
                }
            }
        }
        AnimatedContent(
            targetState = ui.step,
            modifier = Modifier.weight(1f),
            label = "onboarding",
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            }
        ) { step ->
            when (step) {
                0 -> LanguageStep(ui, vm::selectLanguage, vm::confirmLanguage)
                1 -> BibleStep(ui, vm::selectBible, vm::confirmBible, vm::useIncluded)
                2 -> NotificationsStep(
                    onEnable = {
                        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else vm.onNotificationsResult(true)
                    },
                    onLater = vm::next
                )
                else -> com.jbncode.bibliaapp.ui.feature.tutorial.TutorialPager(
                    finishLabel = R.string.onboarding_start,
                    onFinish = { onFinished(vm.finish()) },
                    onBackFromFirst = vm::back,
                    onSkip = { onFinished(vm.finish()) }
                )
            }
        }
    }
}

// ── Pasos ────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageStep(ui: OnboardingUi, onSelect: (AppLanguage) -> Unit, onConfirm: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.onboarding_lang_title),
        subtitle = stringResource(R.string.onboarding_lang_sub),
        content = {
            AppLanguage.entries.forEach { lang ->
                OptionCard(selected = lang == ui.language, onClick = { onSelect(lang) }) {
                    Text(lang.nativeName(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (lang == ui.detected) Badge(stringResource(R.string.onboarding_detected))
                }
            }
        },
        footer = { PrimaryButton(stringResource(R.string.onboarding_continue), onConfirm) }
    )
}

@Composable
private fun BibleStep(ui: OnboardingUi, onSelect: (String) -> Unit, onConfirm: () -> Unit, onUseIncluded: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.onboarding_bible_title),
        subtitle = stringResource(R.string.onboarding_bible_sub),
        content = {
            if (!ui.catalogLoaded) CircularProgressIndicator()
            ui.options.forEachIndexed { i, bible ->
                val selected = bible.code == ui.selectedCode
                OptionCard(selected = selected, onClick = { if (!ui.downloading) onSelect(bible.code) }) {
                    Column(Modifier.weight(1f)) {
                        Text(bible.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                if (bible.database_name == "tla.sqlite3") R.string.onboarding_included
                                else R.string.onboarding_to_download
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selected && ui.downloading) {
                            LinearProgressIndicator(progress = { ui.progress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            Text(stringResource(R.string.onboarding_downloading, ui.progress), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (i == 0) Badge(stringResource(R.string.onboarding_recommended))
                }
            }
            if (ui.error) Text(stringResource(R.string.onboarding_error), color = MaterialTheme.colorScheme.error)
        },
        footer = {
            if (ui.error) {
                PrimaryButton(stringResource(R.string.onboarding_retry), onConfirm)
                TextButton(onClick = onUseIncluded, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_use_default))
                }
            } else {
                PrimaryButton(stringResource(R.string.onboarding_continue), onConfirm, enabled = !ui.downloading && ui.catalogLoaded)
            }
        }
    )
}

@Composable
private fun NotificationsStep(onEnable: () -> Unit, onLater: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.onboarding_notif_title),
        subtitle = stringResource(R.string.onboarding_notif_body),
        content = {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                IconBubble(Icons.Default.Notifications, 96.dp)
            }
        },
        footer = {
            PrimaryButton(stringResource(R.string.onboarding_notif_enable), onEnable)
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_notif_later)) }
        }
    )
}

// ── Piezas ───────────────────────────────────────────────────────────────────

@Composable
private fun StepLayout(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
        Text(subtitle, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        footer()
    }
}

@Composable
private fun OptionCard(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected = selected, onClick = null)
            content()
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconBubble(icon: ImageVector, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
    }
}

private fun AppLanguage.nativeName() = when (this) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.SPANISH -> "Español"
    AppLanguage.FRENCH -> "Français"
    AppLanguage.PORTUGUESE -> "Português"
    AppLanguage.ROMANIAN -> "Română"
}
