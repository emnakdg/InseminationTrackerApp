package com.akdag.inseminationtrackerapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class CowBirthReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val earTag = inputData.getString("earTag") ?: return Result.failure()
        showNotification(
            title = "Kuruya Çıkma Zamanı!",
            message = "Küpe #$earTag — Tohumlamadan 195 gün geçti, kuruya çıkarma zamanı geldi."
        )
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val channelId = "cow_birth_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Kuruya Çıkma Hatırlatıcı", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "İnek kuruya çıkma bildirimleri" }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(earTag.hashCode(), notification)
    }
}
