package com.jbncode.bibliaapp.view.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.jbncode.bibliaapp.R

/**
 * Anuncio nativo discreto con los colores del tema actual (claro/oscuro de la app, no del sistema).
 * No ocupa espacio hasta que el anuncio carga, así que nunca deja un hueco vacío.
 */
@Composable
fun InlineNativeAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adId = stringResource(R.string.native_ad_id)
    val label = stringResource(R.string.bible_ad_label)
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adId) {
        val loader = AdLoader.Builder(context, adId)
            .forNativeAd { ad -> nativeAd?.destroy(); nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMob", "Nativo no cargó: ${error.message}")
                }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy(); nativeAd = null }
    }

    val ad = nativeAd ?: return
    val cs = MaterialTheme.colorScheme
    val titleColor = cs.onSurface.toArgb()
    val bodyColor = cs.onSurfaceVariant.toArgb()
    val accent = cs.primary.toArgb()
    val onAccent = cs.onPrimary.toArgb()

    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        key(ad) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                factory = { ctx -> buildAdView(ctx, ad, label, titleColor, bodyColor, accent, onAccent) }
            )
        }
    }
}


private fun Context.dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

private fun buildAdView(ctx: Context, ad: NativeAd, label: String, title: Int, body: Int, accent: Int, onAccent: Int): NativeAdView {
    val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val icon = ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ctx.dp(44), ctx.dp(44)).apply { rightMargin = ctx.dp(12) }
        ad.icon?.drawable?.let { setImageDrawable(it) } ?: run { visibility = View.GONE }
    }
    val texts = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    val tag = TextView(ctx).apply {
        text = label; textSize = 10f; setTextColor(onAccent)
        setPadding(ctx.dp(6), ctx.dp(1), ctx.dp(6), ctx.dp(1))
        background = GradientDrawable().apply { cornerRadius = ctx.dp(4).toFloat(); setColor(accent) }
    }
    val headline = TextView(ctx).apply {
        text = ad.headline; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(title)
        maxLines = 2; setPadding(0, ctx.dp(4), 0, 0)
    }
    texts.addView(tag, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    texts.addView(headline)
    header.addView(icon); header.addView(texts)
    root.addView(header)

    val bodyView = TextView(ctx).apply {
        text = ad.body; textSize = 13f; setTextColor(body); maxLines = 3; setPadding(0, ctx.dp(8), 0, 0)
        if (ad.body.isNullOrBlank()) visibility = View.GONE
    }
    root.addView(bodyView)

    val adView = NativeAdView(ctx)
    var media: MediaView? = null
    if (ad.mediaContent != null) {
        media = MediaView(ctx).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(150)).apply { topMargin = ctx.dp(10) } }
        root.addView(media)
    }

    val cta = Button(ctx).apply {
        text = ad.callToAction; isAllCaps = false; setTextColor(onAccent)
        background = GradientDrawable().apply { cornerRadius = ctx.dp(24).toFloat(); setColor(accent) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(44)).apply { topMargin = ctx.dp(10) }
        if (ad.callToAction.isNullOrBlank()) visibility = View.GONE
    }
    root.addView(cta)

    adView.addView(root)
    adView.iconView = icon
    adView.headlineView = headline
    adView.bodyView = bodyView
    adView.callToActionView = cta
    media?.let { adView.mediaView = it }
    adView.setNativeAd(ad)
    return adView
}
