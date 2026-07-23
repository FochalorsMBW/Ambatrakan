package com.ambatrakan.sigma

import android.app.Application
import com.ambatrakan.sigma.data.repository.AmbatrakanRepository

class AmbatrakanApplication : Application() {
    val repository by lazy { AmbatrakanRepository(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Fetch FCM token for debug / setup purposes
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            android.util.Log.d("FCM", "FCM Token: $token")
        }
    }
}
