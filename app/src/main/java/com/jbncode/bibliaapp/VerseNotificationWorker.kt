package com.jbncode.bibliaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jbncode.bibliaapp.domain.usescases.VersesDailyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class VerseNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val versesDailyUseCase: VersesDailyUseCase,
    private val sharedPreferenceBible: SharedPreferenceBible
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "verse_channel"
        const val NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val verseText = getVerseOfTheDay()
                sendNotification(verseText)
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private suspend fun getVerseOfTheDay(): String {
        // Obtener el código de la biblia activa (igual que VersesViewmodel)
        val selectedDbFileName = sharedPreferenceBible.getSelectedDatabaseFileName()
            ?: "tla.sqlite3"
        val targetBibleCode = selectedDbFileName
            .removeSuffix(".sqlite3")
            .lowercase()

        val allVerses = versesDailyUseCase.invoke("VersosDiarios")

        var filtered = allVerses.filter {
            it.code.equals(targetBibleCode, ignoreCase = true)
        }

        // Fallback a TLA si no hay versos para la biblia seleccionada
        if (filtered.isEmpty()) {
            filtered = allVerses.filter {
                it.code.equals("tla", ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) return ""

        // Reutilizar la misma lógica del verso del día que ya tienes
        val verse = sharedPreferenceBible.getVerseOfTheDay(filtered)
        return verse.verse_content ?: ""
    }

    private fun sendNotification(verseText: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Versículo Diario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorio diario con el versículo del día."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = verseText.ifBlank {
            applicationContext.getString(R.string.daily_verse_title)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("📖 Versículo del Día")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}