package com.ambatrakan.sigma

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ambatrakan.sigma.theme.AmbatrakanTheme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            val prefs = getSharedPreferences("ambatrakan_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("fcm_token", token).apply()
            
            val currentTenantId = prefs.getLong("current_tenant_id", -1L)
            if (currentTenantId != -1L) {
                (application as AmbatrakanApplication).repository.updateFcmToken(currentTenantId, token)
            }
        }
    }

    enableEdgeToEdge()
    setContent {
      val themeMode by (application as AmbatrakanApplication).themePreferences.themeMode.collectAsState()
      AmbatrakanTheme(themeMode = themeMode) { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}

