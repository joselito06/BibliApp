package com.jbncode.bibliaapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.jbncode.bibliaapp.R

object RewardedDownloadAdManager {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context) {
            preloadAd(context)
        }
    }

    fun preloadAd(context: Context) {
        if (rewardedInterstitialAd != null || isLoading) return
        isLoading = true

        // Toma automáticamente el resValue("string", "ad_unit_id", ...) según sea debug o release
        val adUnitId = context.getString(R.string.ad_unit_id)

        RewardedInterstitialAd.load(
            context.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoading = false
                    Log.d("ADMOB_REWARD", "Intersticial bonificado cargado")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoading = false
                    Log.w("ADMOB_REWARD", "Error al precargar anuncio: ${error.message}")
                }
            }
        )
    }

    fun showAdThenDownload(
        context: Context,
        onProceedToDownload: () -> Unit
    ) {
        val activity = context.findActivity()
        val ad = rewardedInterstitialAd

        if (activity == null || ad == null) {
            preloadAd(context)
            onProceedToDownload()
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                preloadAd(context)
                if (rewardGranted) {
                    onProceedToDownload()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                preloadAd(context)
                onProceedToDownload()
            }
        }

        ad.show(activity) { _ ->
            rewardGranted = true
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}