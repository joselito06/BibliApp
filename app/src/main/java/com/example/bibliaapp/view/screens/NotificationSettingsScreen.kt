package com.example.bibliaapp.view.screens


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.SettingsViewmodel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(viewModel: SettingsViewmodel, mainViewmodel: MainViewmodel, navigation: NavHostController) {
    val context = LocalContext
    val selectedTime by viewModel.selectedTime.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(false)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        ElevatedCard(
            modifier = Modifier,

        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    Text(text = if (notificationsEnabled) "Notificaciones activadas" else "Notificaciones desactivadas")
                }
                HorizontalDivider(thickness = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        viewModel.setSelectedTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }) {
                        Text("Aceptar")
                    }
                    Button(onClick = {  }) {
                        Text("Cancelar")
                    }
                }
            }

        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarNotification(navigation: NavHostController)
{
    TopAppBar(
        modifier = Modifier
            //.offset(y = if (isTopBarVisible) 0.dp else -56.dp) // Oculta la TopAppBar al hacer scroll
            .animateContentSize(),
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            IconButton(onClick = { navigation.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Acción adicional */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        title = {
            Text("Notificaciones")

        }
    )
}
