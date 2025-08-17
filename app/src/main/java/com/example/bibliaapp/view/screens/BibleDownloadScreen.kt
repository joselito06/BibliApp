package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.bibliaapp.data.networking.models.BibleVersionInfo
import com.example.bibliaapp.view.Routes
import com.example.bibliaapp.view.viewmodel.BibleManagerViewmodel

@Composable
@Preview(device = "spec:width=411dp,height=891dp")
fun PreviewBibleDownloadScreen(){
    //BibleDownloadScreen()
}

@Composable
fun BibleDownloadScreen(viewModel: BibleManagerViewmodel = hiltViewModel(), navController: NavHostController){
    val bibles by viewModel.filteredBibleList.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguageFilter.collectAsState()

    val statusMessage by viewModel.downloadStatus.observeAsState(initial = "")
    val selectedDbName by viewModel.sharedPreferenceBible.getSelectedDatabaseFileNameFlow()
        .collectAsState(initial = viewModel.sharedPreferenceBible.getSelectedDatabaseFileName())

    // Estado para controlar la expansión del DropdownMenu
    var languageFilterExpanded by remember { mutableStateOf(false) }

    // Configurar el callback en el ViewModel
    LaunchedEffect(viewModel, navController) {
        viewModel.onNavigateToRestartScreen = {
            // Asegúrate de que la navegación se haga de una manera que limpie el backstack
            // si es necesario, o simplemente navega.
            navController.navigate(Routes.RestartingScreen.route) {
                // Opcional: limpiar backstack hasta la pantalla de inicio si quieres
                // popUpTo(navController.graph.startDestinationId) { inclusive = true }
                // launchSingleTop = true // Evita múltiples copias si se presiona rápido
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) { // Usar fillMaxSize si la lista puede ser larga
        if (statusMessage.isNotBlank()) {
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            )
        }

        // Fila para el Filtro de Idioma
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Filtrar por idioma:", style = MaterialTheme.typography.titleSmall)

            Box { // Box es necesario para anclar correctamente el DropdownMenu
                OutlinedButton(
                    onClick = { languageFilterExpanded = true },
                    modifier = Modifier.defaultMinSize(minWidth = 120.dp) // Ajusta según necesites
                ) {
                    Text(selectedLanguage ?: "Todos") // Muestra el idioma seleccionado o "Todos"
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Seleccionar idioma")
                }

                DropdownMenu(
                    expanded = languageFilterExpanded,
                    onDismissRequest = { languageFilterExpanded = false }
                ) {
                    availableLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language) },
                            onClick = {
                                viewModel.setLanguageFilter(if (language == "Todos") null else language)
                                languageFilterExpanded = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        if (bibles.isEmpty() && selectedLanguage != "Todos" && selectedLanguage != null) {
            Text(
                "No hay Biblias disponibles para el idioma '$selectedLanguage'.",
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) { // weight(1f) si está dentro de una Column con otros elementos fijos
                items(bibles, key = { it.id }) { bible -> // Añadir una key es bueno para el rendimiento
                    val isDefaultAssetBible = bible.database_name == "tla.sqlite3" // Ajusta esto
                    val isActive = (selectedDbName == bible.database_name) ||
                            (selectedDbName == null && isDefaultAssetBible)

                    BibleListItem(
                        bibleInfo = bible,
                        isCurrentlyActive = isActive,
                        onDownloadClick = { viewModel.startDownload(bible) },
                        onActivateClick = { viewModel.activateDownloadedDatabase(bible.database_name) },
                        onSetAsDefaultClick = if (isActive && !isDefaultAssetBible) {
                            { viewModel.activateDefaultDatabase() }
                        } else null
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }
            }
        }
    }

}

@Composable
fun BibleListItem(
    bibleInfo: BibleVersionInfo,
    isCurrentlyActive: Boolean,
    onDownloadClick: () -> Unit,
    onActivateClick: () -> Unit,
    onSetAsDefaultClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Book,
            contentDescription = "Icono de Biblia",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bibleInfo.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Código: ${bibleInfo.code.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Podrías añadir el idioma aquí si quieres que sea visible en el ítem,
            // aunque ya está siendo usado para filtrar.
            // Text(
            // text = "Idioma: ${bibleInfo.lang}",
            // style = MaterialTheme.typography.caption,
            // color = MaterialTheme.colorScheme.outline
            // )
            if (bibleInfo.description.isNotBlank()) {
                Text(
                    text = bibleInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (bibleInfo.isDownloading) {
                LinearProgressIndicator(
                    progress = { bibleInfo.downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                Text(
                    "Descargando: ${bibleInfo.downloadProgress}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        when {
            bibleInfo.isDownloading -> {
                Button(
                    onClick = { /* Considerar acción de cancelar */ },
                    enabled = false,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                ) {
                    Text("Descargando...")
                }
            }
            bibleInfo.isDownloaded -> {
                if (isCurrentlyActive) {
                    if (onSetAsDefaultClick != null && bibleInfo.database_name != "tla.sqlite3" /* Ajusta a tu BD de assets */) {
                        OutlinedButton(
                            onClick = onSetAsDefaultClick,
                            modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                        ) {
                            Text("Usar Default")
                        }
                    } else {
                        Text(
                            "Activa",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onActivateClick,
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text("Activar")
                    }
                }
            }
            else -> { // No descargada
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                ) {
                    Text("Descargar")
                }
            }
        }
    }
}