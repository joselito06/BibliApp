package com.jbncode.bibliaapp.view.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import com.jbncode.bibliaapp.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.ui.components.AppTopBar
import com.jbncode.bibliaapp.ui.components.SystemBarsColor
import com.jbncode.bibliaapp.view.components.BannerAd
import com.jbncode.bibliaapp.view.components.NativeAdWidget
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    mainViewmodel: MainViewmodel,
    navigation: NavHostController,
) {
    SystemBarsColor(MaterialTheme.colorScheme.primaryContainer)
    val context = LocalContext.current
    val version = stringResource(R.string.app_version_name)

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(false)
        mainViewmodel.showBottomBar(false)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.about_screen_title),
                icon = Icons.Default.Info,
                onBack = { navigation.popBackStack() }
            )
        },
        bottomBar = { BannerAd(stringResource(R.string.banner_ad_id)) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Text(
                stringResource(R.string.app_name),
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    stringResource(R.string.about_screen_version) + " " + version,
                    Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Text(
                    stringResource(R.string.about_screen_description_text),
                    Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    lineHeight = 24.sp
                )
            }

            Text(
                stringResource(R.string.about_screen_developer) + " JBN Code",
                Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    val subject = Uri.encode("BibliApp $version")
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:soporte.bibliaapp@gmail.com?subject=$subject")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) { }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.about_contact_support))
            }

            Spacer(Modifier.height(24.dp))
            NativeAdWidget()
            Spacer(Modifier.height(24.dp))
        }
    }
}
