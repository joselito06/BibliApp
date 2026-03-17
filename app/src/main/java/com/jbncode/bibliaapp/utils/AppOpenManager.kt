package com.jbncode.bibliaapp.utils

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.jbncode.bibliaapp.JetpackApp
import com.jbncode.bibliaapp.R

class AppOpenManager(private val myApplication: JetpackApp) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        val adId = myApplication.getString(R.string.app_loaded_ad_id)
        AppOpenAd.load(myApplication, adId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                }
            })
    }

    fun showAdIfAvailable(activity: Activity) {
        appOpenAd?.fullScreenContentCallback = object :
            FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { appOpenAd = null; loadAd() }
        }
        appOpenAd?.show(activity)
    }

    private fun isAdAvailable(): Boolean = appOpenAd != null
}