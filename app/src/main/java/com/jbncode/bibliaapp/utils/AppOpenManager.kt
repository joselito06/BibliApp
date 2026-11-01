package com.jbncode.bibliaapp.utils

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.jbncode.bibliaapp.JetpackApp
import com.jbncode.bibliaapp.R

class AppOpenManager(private val myApplication: JetpackApp) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadedAt = 0L

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        val adId = myApplication.getString(R.string.app_loaded_ad_id)
        AppOpenAd.load(myApplication, adId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadedAt = System.currentTimeMillis()
                    isLoadingAd = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                }
            })
    }

    fun showAdIfAvailable(activity: Activity) {
        // Siempre evaluamos (consume el estado), aunque no haya anuncio listo
        val allowed = AdGate.shouldShowAppOpen(activity)
        if (!allowed) { loadAd(); return }
        val ad = appOpenAd ?: run { loadAd(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { AdGate.isFullScreenAdShowing = true }
            override fun onAdDismissedFullScreenContent() {
                AdGate.isFullScreenAdShowing = false
                AdGate.lastFullScreenAdDismissedAt = System.currentTimeMillis()
                appOpenAd = null; loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                AdGate.isFullScreenAdShowing = false
                appOpenAd = null; loadAd()
            }
        }
        ad.show(activity)
    }

    // Los anuncios AppOpen caducan a las 4h
    private fun isAdAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - loadedAt < 4 * 3_600_000L
}