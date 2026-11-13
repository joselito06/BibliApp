package com.jbncode.bibliaapp.ui.feature.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.core.tts.VoiceOption
import java.util.Locale

/** Ajustes de la lectura en voz alta, sin salir del lector: velocidad, voz y anuncios. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsSheet(vm: PlaybackViewModel, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rate by vm.speechRate.collectAsState()
    var local by remember(rate) { mutableFloatStateOf(rate) }
    val announceVerses by vm.announceVerseNumbers.collectAsState()
    val announceTitles by vm.announceChapterTitles.collectAsState()
    var options by remember { mutableStateOf<List<VoiceOption>?>(null) }

    LaunchedEffect(Unit) { options = vm.voiceOptions() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.tts_voice_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            Text(
                stringResource(R.string.tts_speed_label) + "  " + String.format(Locale.US, "%.1fx", local),
                Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium
            )
            // Se aplica al soltar: evita reiniciar la voz en cada pixel del deslizador.
            Slider(
                value = local, onValueChange = { local = it },
                onValueChangeFinished = { vm.setSpeechRate(local) },
                valueRange = 0.6f..1.6f, steps = 9
            )

            SwitchRow(stringResource(R.string.announce_verse_numbers), announceVerses, vm::setAnnounceVerseNumbers)
            SwitchRow(stringResource(R.string.announce_chapter_titles), announceTitles, vm::setAnnounceChapterTitles)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.tts_voice_pick), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            val list = options
            when {
                list == null -> Text(stringResource(R.string.bible_selection_loading_details), Modifier.padding(vertical = 10.dp))
                else -> {
                    VoiceRow(stringResource(R.string.tts_voice_auto), list.none { it.selected }) { vm.selectVoice(null) }
                    if (list.isEmpty()) Text(stringResource(R.string.tts_voice_none), Modifier.padding(vertical = 8.dp))
                    list.forEachIndexed { i, o ->
                        val label = "${i + 1} · ${o.tag}" + if (o.online) " · " + stringResource(R.string.tts_voice_online) else ""
                        VoiceRow(label, o.selected) { vm.selectVoice(o.name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun VoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
