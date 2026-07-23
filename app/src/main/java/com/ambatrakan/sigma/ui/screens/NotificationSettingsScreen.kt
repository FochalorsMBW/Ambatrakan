package com.ambatrakan.sigma.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SecurityUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ambatrakan.sigma.data.local.NotificationPreferences
import com.ambatrakan.sigma.utils.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { NotificationPreferences(context) }

    var paymentNotifEnabled by remember { mutableStateOf(prefs.isPaymentNotificationEnabled()) }
    var adminChangeNotifEnabled by remember { mutableStateOf(prefs.isAdminChangesNotificationEnabled()) }
    var reminderHour by remember { mutableStateOf(prefs.getReminderHour()) }
    var reminderMinute by remember { mutableStateOf(prefs.getReminderMinute()) }
    
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Atur Jam Pengingat") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.outlineVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.outlineVariant,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = timePickerState.hour
                    reminderMinute = timePickerState.minute
                    prefs.setReminderTime(timePickerState.hour, timePickerState.minute)
                    ReminderScheduler.scheduleReminder(context)
                    showTimePicker = false
                }) {
                    Text("SIMPAN", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("BATAL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Zinc 900
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Notification Settings",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pengingat Giliran (Schedule)
                    Text("JADWAL PENGINGAT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable {
                            showTimePicker = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Peringatan Giliran Tagihan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text("Setiap hari pada ${String.format("%02d:%02d", reminderHour, reminderMinute)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Notifikasi Sistem
                    Text("SISTEM NOTIFIKASI", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Payment
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pemberitahuan Tagihan Lunas", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("Saat penghuni atau Anda membayar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = paymentNotifEnabled,
                                    onCheckedChange = {
                                        paymentNotifEnabled = it
                                        prefs.setPaymentNotificationEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Admin changes
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SecurityUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Aktivitas Admin", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("Tambah, Edit, Hapus tagihan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = adminChangeNotifEnabled,
                                    onCheckedChange = {
                                        adminChangeNotifEnabled = it
                                        prefs.setAdminChangesNotificationEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
