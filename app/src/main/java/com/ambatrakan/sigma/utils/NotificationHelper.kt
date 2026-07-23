package com.ambatrakan.sigma.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ambatrakan.sigma.MainActivity
import com.ambatrakan.sigma.R

object NotificationHelper {
    private const val CHANNEL_ID = "ambatrakan_channel_v3"

    // Counter atomik agar tiap notifikasi punya ID unik. Sebelumnya memakai
    // System.currentTimeMillis().toInt() yang bisa tabrakan (dua notifikasi
    // dalam 1 ms) dan ter-truncate ke Int.
    private val notificationIdCounter = java.util.concurrent.atomic.AtomicInteger(1000)

    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Buat URI untuk file suara (contoh: res/raw/notif_sound.mp3)
        // Kita gunakan R.raw.notif_sound sebagai default, pastikan file ini ada
        val soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.notif_sound)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ambatrakan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationIdCounter.getAndIncrement(), builder.build())
    }
}
