package com.jbncode.bibliaapp

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JetpackApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) { initializationStatus ->
            // Opcional: Puedes verificar si se inicializó correctamente
            val status = initializationStatus.adapterStatusMap
            Log.d("AdMob", "SDK inicializado")
        }
    }
}