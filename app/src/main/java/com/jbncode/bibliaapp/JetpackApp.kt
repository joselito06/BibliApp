package com.jbncode.bibliaapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JetpackApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Este getter es el que WorkManager usa para construir Workers
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        setupRemoteConfig()

        MobileAds.initialize(this) { initializationStatus ->
            // Opcional: Puedes verificar si se inicializó correctamente
            val status = initializationStatus.adapterStatusMap
            Log.d("AdMob", "SDK inicializado")
        }
    }

    private fun setupRemoteConfig() {
        val remoteConfig: FirebaseRemoteConfig =
            FirebaseRemoteConfig.getInstance()

        // Configuración de intervalo de escaneo (ej. cada 1 hora en producción)
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(30)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Valores por defecto (coinciden con tu versionName actual)
        val defaultValues = mapOf(
            "min_version_required" to "1.0.0",
            "latest_version" to "1.0.0"
        )
        remoteConfig.setDefaultsAsync(defaultValues)

        // Cargar los valores desde la nube
        remoteConfig.fetchAndActivate()
    }


}