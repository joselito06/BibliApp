package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bibliaapp.view.viewmodel.MainViewmodel

@Composable
fun AboutScreen(mainViewmodel: MainViewmodel) {

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(false)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }
    Box(

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Esta aplicación te ayuda a recibir y leer versículos diarios de la Biblia, permitiéndote personalizar notificaciones y seleccionar diferentes versiones de la Biblia.",
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),

                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Versión: 1.0.0",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Desarrollado por: JBN Code",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

}