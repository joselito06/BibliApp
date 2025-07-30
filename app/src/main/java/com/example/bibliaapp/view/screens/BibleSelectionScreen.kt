package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.SettingsViewmodel

@Composable
fun BibleSelectionScreen(viewModel: SettingsViewmodel, mainViewmodel: MainViewmodel) {
    val bibleVersions = listOf("Reina-Valera 1960", "Nueva Versión Internacional", "Biblia de Jerusalén", "Dios Habla Hoy")
    val selectedBible by viewModel.selectedBible.collectAsState()

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(false)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        //verticalArrangement = Arrangement.Center,
        //horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(

        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Seleccionar versión de la Biblia", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    itemsIndexed(bibleVersions) { index, version ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (version == selectedBible),
                                onClick = { viewModel.setSelectedBible(version) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(version.toString())
                        }
                        if (index+1 < bibleVersions.count())
                            HorizontalDivider(thickness = 1.dp)


                    }
                }
            }

        }

    }
}
