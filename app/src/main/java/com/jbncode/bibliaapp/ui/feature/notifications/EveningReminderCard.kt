package com.jbncode.bibliaapp.ui.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jbncode.bibliaapp.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Recordatorio nocturno "proteger mi racha". Se agrega al final de NotificationSettingsScreen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveningReminderCard(vm: NotificationsViewModel = hiltViewModel()) {
    val enabled by vm.eveningEnabled.collectAsState()
    val time by vm.eveningTime.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.NightsStay, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_evening_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.reminder_evening_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = vm::setEveningEnabled)
            }
            if (enabled) {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reminder_evening_time) + ": " + time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { vm.setEveningTime(LocalTime.of(state.hour, state.minute)); showPicker = false }) {
                    Text(stringResource(R.string.accept_button))
                }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
    }
}
