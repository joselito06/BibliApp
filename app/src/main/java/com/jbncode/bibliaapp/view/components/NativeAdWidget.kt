package com.jbncode.bibliaapp.view.components

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.jbncode.bibliaapp.R

@Composable
fun NativeAdWidget() {
    val context = LocalContext.current
    val adId = stringResource(R.string.native_ad_id)
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val adLabelText = stringResource(R.string.bible_ad_label)

    LaunchedEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    // Log para debug
                    android.util.Log.e("AdMob", "Error al cargar nativo: ${error.message}")
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            factory = { ctx ->
                val adView = NativeAdView(ctx)

                val layout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Etiqueta "Anuncio"
                val adLabel = TextView(ctx).apply {
                    text = adLabelText
                    textSize = 10f
                    setPadding(10, 2, 10, 2)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#757575"))
                }

                // Título
                val title = TextView(ctx).apply {
                    text = ad.headline
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    // Usamos colores que funcionen en modo claro/oscuro
                    setTextColor(if (isSystemInDarkModeCustom(ctx)) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                    setPadding(0, 8, 0, 4)
                }

                // Cuerpo
                val body = TextView(ctx).apply {
                    text = ad.body
                    textSize = 13f
                    setTextColor(android.graphics.Color.GRAY)
                }

                layout.addView(adLabel)
                layout.addView(title)
                layout.addView(body)

                adView.addView(layout)

                // ✅ CRUCIAL: Registrar las vistas en el NativeAdView
                adView.headlineView = title
                adView.bodyView = body

                // Asignar el anuncio cargado al view
                adView.setNativeAd(ad)

                adView
            },
            update = { adView ->
                // Esto asegura que el anuncio se mantenga actualizado si cambia el estado
                adView.setNativeAd(ad)
            }
        )
    }
}

// Función auxiliar para detectar modo oscuro en el contexto de View
fun isSystemInDarkModeCustom(context: android.content.Context): Boolean {
    val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}