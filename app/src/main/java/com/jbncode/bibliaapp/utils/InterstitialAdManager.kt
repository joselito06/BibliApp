package com.jbncode.bibliaapp.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jbncode.bibliaapp.R

object InterstitialAdManager {
    private var mInterstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var actionCount = 0L
    private var lastShownAt = 0L

    fun load(context: Context) {
        if (mInterstitialAd != null || isLoading) return
        isLoading = true
        val adId = context.getString(R.string.interstitial_ad_id)
        InterstitialAd.load(context, adId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isLoading = false
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isLoading = false
                }
            })
    }

    private fun policyAllows(): Boolean {
        actionCount++
        val now = System.currentTimeMillis()
        val everyN = AdGate.interstitialEveryN()
        val intervalOk = now - lastShownAt >= AdGate.interstitialMinIntervalSec() * 1000L
        return actionCount % everyN == 0L && intervalOk
    }

    fun show(activity: Activity, onAdClosed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad == null || !policyAllows()) {
            if (ad == null) load(activity)
            onAdClosed(); return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdGate.isFullScreenAdShowing = true
                lastShownAt = System.currentTimeMillis()
            }
            override fun onAdDismissedFullScreenContent() {
                AdGate.isFullScreenAdShowing = false
                AdGate.lastFullScreenAdDismissedAt = System.currentTimeMillis()
                mInterstitialAd = null
                load(activity)
                onAdClosed()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                AdGate.isFullScreenAdShowing = false
                mInterstitialAd = null
                load(activity)
                onAdClosed()
            }
        }
        ad.show(activity)
    }
}