package com.ambatrakan.sigma.services

import android.content.Context
import com.ambatrakan.sigma.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Simpan token; akan dipungut repository dan disinkron ke tenant saat login.
        val prefs = getSharedPreferences("ambatrakan_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        val currentTenantId = prefs.getLong("current_tenant_id", -1L)
        if (currentTenantId != -1L) {
            val app = applicationContext as? com.ambatrakan.sigma.AmbatrakanApplication
            app?.repository?.updateFcmToken(currentTenantId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Dukung payload notification MAUPUN data-only. Payload data lebih andal karena
        // selalu memicu onMessageReceived dan memakai channel/suara kustom kita sendiri.
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Pemberitahuan Baru"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: ""

        // Satu jalur tampilan yang konsisten dengan notifikasi lokal (channel & suara sama).
        NotificationHelper.showNotification(this, title, body)
    }
}
