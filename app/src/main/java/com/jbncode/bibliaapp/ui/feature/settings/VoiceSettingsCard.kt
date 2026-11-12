package com.jbncode.bibliaapp.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.core.tts.VoiceOption
import com.jbncode.bibliaapp.ui.feature.reader.PlaybackViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VoiceSettingsCard(vm: PlaybackViewModel) {
    val rate by vm.speechRate.collectAsState()
    var local by remember(rate) { mutableFloatStateOf(rate) }
    var options by remember { mutableStateOf<List<VoiceOption>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.tts_voice_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.tts_speed_label) + "  " + String.format(Locale.US, "%.1fx", local),
                Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium
            )
            // Se aplica al soltar: evita reiniciar la voz en cada pixel del deslizador.
            Slider(
                value = local, onValueChange = { local = it },
                onValueChangeFinished = { vm.setSpeechRate(local) },
                valueRange = 0.6f..1.6f, steps = 9
            )
            OutlinedButton(
                onClick = { scope.launch { options = vm.voiceOptions(); showDialog = true } },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.tts_voice_pick)) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.tts_voice_pick)) },
            text = {
                Column {
                    VoiceRow(stringResource(R.string.tts_voice_auto), options.none { it.selected }) {
                        vm.selectVoice(null); showDialog = false
                    }
                    if (options.isEmpty()) Text(stringResource(R.string.tts_voice_none), Modifier.padding(top = 8.dp))
                    options.forEachIndexed { i, o ->
                        val label = "${i + 1} · ${o.tag}" + if (o.online) " · " + stringResource(R.string.tts_voice_online) else ""
                        VoiceRow(label, o.selected) { vm.selectVoice(o.name); showDialog = false }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }
}

@Composable
private fun VoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
