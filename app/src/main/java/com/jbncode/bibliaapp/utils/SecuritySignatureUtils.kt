package com.jbncode.bibliaapp.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecuritySignatureUtils {
    private val OBFUSCATED_SEED = byteArrayOf(
        0x50, 0x72, 0x33, 0x66, 0x69, 0x33, 0x72, 0x6F,
        0x53, 0x33, 0x72, 0x53, 0x65, 0x72, 0x69, 0x6F,
        0x32, 0x30, 0x32, 0x36, 0x5F, 0x42, 0x69, 0x62,
        0x6C, 0x69, 0x61, 0x70, 0x70, 0x5F, 0x39, 0x39,
        0x35, 0x35, 0x37, 0x37, 0x30, 0x31
    )

    fun generateProtectedHeaders(context: Context, fileName: String): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val installId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        val packageName = context.packageName
        val apkSha256 = getApkSignatureSha256(context)

        Log.d("APK_HASH", "Tu APK_SHA256_DEBUG para Cloudflare es: $apkSha256")

        val secretSeed = String(OBFUSCATED_SEED, Charsets.UTF_8)
        val combinedSecret = "${secretSeed}_$apkSha256"
        val payload = "$fileName:$timestamp:$packageName:$installId"
        val signature = hmacSha256(payload, combinedSecret)

        Log.d("HMAC_DEBUG", "Kotlin envio -> payload=[$payload], secretLen=[${secretSeed.length}], hashLen=[${apkSha256.length}], sig=[$signature]")

        return mapOf(
            "User-Agent" to "BibliApp-Android/1.0-Official",
            "X-Timestamp" to timestamp,
            "X-Signature" to signature,
            "X-App-Package" to packageName,
            "X-Install-Id" to installId
        )
    }

    /*fun generateProtectedHeaders(context: Context, fileName: String): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val installId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        // Obtiene el SHA-256 real del certificado del APK en ejecución
        val apkSha256 = getApkSignatureSha256(context)

        if (BuildConfig.DEBUG) {
            // Mira el Logcat con el filtro "APK_HASH" para copiar tu hash de Debug y pegarlo en Cloudflare
            Log.d("APK_HASH", "Tu APK_SHA256_DEBUG para Cloudflare es: $apkSha256")
        }

        val combinedSecret = "${APP_SECRET}_$apkSha256"
        val payload = "$fileName:$timestamp:$PACKAGE_NAME:$installId"
        val signature = hmacSha256(payload, combinedSecret)

        return mapOf(
            "User-Agent" to "BibliApp-Android/1.0-Official",
            "X-Timestamp" to timestamp,
            "X-Signature" to signature,
            "X-App-Package" to PACKAGE_NAME,
            "X-Install-Id" to installId
        )
    }*/

    @Suppress("DEPRECATION")
    private fun getApkSignatureSha256(context: Context): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                packageInfo.signatures
            }

            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return "invalid"
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            digest.joinToString("") { "%02x".format(it) }.lowercase()
        } catch (e: Exception) {
            "error"
        }
    }

    private fun hmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}