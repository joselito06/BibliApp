package com.jbncode.bibliaapp.view.components

import android.widget.TextView
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

    LaunchedEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { ad -> nativeAd = ad }
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = Modifier.padding(12.dp),
            factory = { ctx ->
                // Inflamos un layout simple programáticamente
                val adView = NativeAdView(ctx)
                val layout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                }

                val title = TextView(ctx).apply {
                    text = ad.headline
                    textSize = 16f
                    setTextColor(android.graphics.Color.BLACK)
                }

                val body = TextView(ctx).apply {
                    text = ad.body
                    textSize = 12f
                }

                layout.addView(title)
                layout.addView(body)
                adView.addView(layout)

                adView.setNativeAd(ad)
                adView
            }
        )
    }
}