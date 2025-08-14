package com.example.bibliaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.bibliaapp.view.viewmodel.VersesViewmodel

class VerseNotificationWorker(context: Context, params: WorkerParameters, versesViewmodel: VersesViewmodel) : Worker(context, params) {

    val _versesViewmodel = versesViewmodel

    override fun doWork(): Result {
        sendNotification()
        return Result.success()
    }

    private fun sendNotification() {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("verse_channel", "Versículo Diario", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "verse_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("📖 Versículo del Día")
            .setContentText(getVerseOfTheDay())
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun getVerseOfTheDay(): String? {
        val verse = _versesViewmodel.verseOfTheDay.value
        if (verse.verse_id >= 0){
            return verse.verse_content
        }else{
            return ""
        }
    }
}