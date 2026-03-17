package com.jbncode.bibliaapp.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jbncode.bibliaapp.R

object InterstitialAdManager {
    private var mInterstitialAd: InterstitialAd? = null

    fun load(context: Context) {
        val adId = context.getString(R.string.interstitial_ad_id)
        InterstitialAd.load(context, adId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    fun show(activity: Activity, onAdClosed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object :
                FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    load(activity) // Recargar para la próxima vez
                    onAdClosed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            onAdClosed()
        }
    }
}