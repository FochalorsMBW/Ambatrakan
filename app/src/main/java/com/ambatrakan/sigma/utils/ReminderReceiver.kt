package com.ambatrakan.sigma.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ambatrakan.sigma.AmbatrakanApplication
import com.ambatrakan.sigma.data.local.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleReminder(context)
            return
        }

        // Action when alarm fires
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as AmbatrakanApplication
            val repository = app.repository
            val state = repository.currentState
            
            val prefs = context.getSharedPreferences("ambatrakan_prefs", Context.MODE_PRIVATE)
            val currentTenantId = prefs.getLong("current_tenant_id", -1L).takeIf { it != -1L } ?: return@launch
            val currentUser = state.tenants.find { it.id == currentTenantId } ?: return@launch
            
            // Check if current user is the PIC for any bill
            val pendingBills = state.billingItems.filter { it.locationId == currentUser.locationId && it.currentPicTenantIds.contains(currentUser.id) }
            
            if (pendingBills.isNotEmpty()) {
                val billNames = pendingBills.joinToString(", ") { it.name }
                NotificationHelper.showNotification(
                    context,
                    "Peringatan Giliran Tagihan",
                    "Hai ${currentUser.name}, ini giliranmu membayar $billNames. Jangan sampai lupa ya!"
                )
            }
            
            // Reschedule for next day
            ReminderScheduler.scheduleReminder(context)
        }
    }
}

object ReminderScheduler {
    fun scheduleReminder(context: Context) {
        val prefs = NotificationPreferences(context)
        val hour = prefs.getReminderHour()
        val minute = prefs.getReminderMinute()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            1001, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Tanpa izin exact alarm, gunakan setAndAllowWhileIdle agar tetap menyala saat Doze.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            // setExactAndAllowWhileIdle: presisi + tetap menyala meski perangkat dalam mode Doze.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
    
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            1001, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
