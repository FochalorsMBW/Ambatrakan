package com.ambatrakan.sigma.data.local

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)

    fun isPaymentNotificationEnabled(): Boolean {
        return prefs.getBoolean("pref_notify_payments", true)
    }

    fun setPaymentNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_notify_payments", enabled).apply()
    }

    fun isAdminChangesNotificationEnabled(): Boolean {
        return prefs.getBoolean("pref_notify_admin_changes", true)
    }

    fun setAdminChangesNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_notify_admin_changes", enabled).apply()
    }

    fun getReminderHour(): Int {
        return prefs.getInt("reminder_hour", 8) // Default 08:00
    }

    fun getReminderMinute(): Int {
        return prefs.getInt("reminder_minute", 0)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
    }
}
