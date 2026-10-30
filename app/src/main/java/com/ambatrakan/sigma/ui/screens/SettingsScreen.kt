package com.ambatrakan.sigma.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.theme.*
import com.ambatrakan.sigma.ui.viewmodel.SettingsViewModel
import com.ambatrakan.sigma.ui.utils.tactileFeedback
import com.ambatrakan.sigma.ui.screens.formatRupiah

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val currentTenant by viewModel.currentTenant.collectAsState()
    val isAdmin = currentTenant?.role == "ADMIN"
    val locations by viewModel.locations.collectAsState()
    val activeLocationId by viewModel.activeLocationId.collectAsState()
    val billingItems by viewModel.billingItems.collectAsState()
    val tenants by viewModel.tenants.collectAsState()

    val isAdminModeEnabled by viewModel.isAdminModeEnabled.collectAsState()

    var showOccupantsDialog by remember { mutableStateOf(false) }
    var showManageBillsDialog by remember { mutableStateOf(false) }
    var activeEditBillingItem by remember { mutableStateOf<BillingItem?>(null) }
    var activeDeleteBillingItem by remember { mutableStateOf<BillingItem?>(null) }
    var showNotificationSettings by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVersionInfoDialog by remember { mutableStateOf(false) }

    val activeLocation = locations.find { it.id == activeLocationId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profil Pengguna
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentTenant?.initials ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentTenant?.name ?: "Tamu",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isAdminModeEnabled) "Administrator" else "Penghuni",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Edit Profil", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Status Kehadiran (Away/Cuti)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sedang Pulang Kampung", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Lewati tagihan bergilir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = currentTenant?.isAway == true,
                        onCheckedChange = { isAway ->
                            currentTenant?.let { t ->
                                viewModel.toggleAwayStatus(t.id, isAway)
                            }
                        }
                    )
                }
            }
        }

        if (isAdminModeEnabled) {
            item {
                Text("Manajemen Properti", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingRowItem(
                            icon = Icons.Default.PeopleAlt,
                            iconColor = YellowStatus,
                            title = "Kelola Penghuni",
                            subtitle = "Atur daftar penghuni kontrakan",
                            onClick = { showOccupantsDialog = true }
                        )
                    }
                }
            }

            item {
                Text("Kontrol Tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingRowItem(
                            icon = Icons.Default.ReceiptLong,
                            iconColor = GreenStatus,
                            title = "Pengaturan Tagihan",
                            subtitle = "Atur daftar dan jenis tagihan",
                            onClick = { showManageBillsDialog = true }
                        )
                    }
                }
            }
        }

        item {
            Text("Sistem & Perangkat", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingRowItem(
                        icon = Icons.Default.Notifications,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Notifikasi",
                        subtitle = "Peringatan, suara, dan getaran",
                        onClick = { showNotificationSettings = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingRowItem(
                        icon = Icons.Default.Security,
                        iconColor = GreenStatus,
                        title = "Privasi dan Keamanan",
                        subtitle = "Kata sandi, sidik jari",
                        onClick = { showSecurityDialog = true }
                    )
                }
            }
        }

        item {
            Text("Aplikasi", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingRowItem(
                        icon = Icons.Default.Info,
                        iconColor = Color.Gray,
                        title = "Informasi Versi",
                        subtitle = "Ambatrakan v2.0.0",
                        onClick = { showVersionInfoDialog = true }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Button(
                onClick = {
                    viewModel.logout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
    
    if (showOccupantsDialog) {
        ManageOccupantsScreen(viewModel = viewModel, onDismiss = { showOccupantsDialog = false })
    }
    


    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(currentTenant?.name ?: "") }
        var editEmail by remember { mutableStateOf(currentTenant?.email ?: "") }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profil", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Tampilan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email / Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateProfile(editName, editEmail)
                    }
                    showEditProfileDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showVersionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showVersionInfoDialog = false },
            title = { Text("Informasi Versi", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Ambatrakan v2.0.0", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Changelog:", fontWeight = FontWeight.SemiBold)
                    Text("- Peningkatan UI/UX Dashboard\n- Penambahan fitur push notification\n- Dukungan dark mode penuh\n- Perbaikan bug sinkronisasi", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lisensi:", fontWeight = FontWeight.SemiBold)
                    Text("Aplikasi ini menggunakan berbagai lisensi open-source termasuk Apache License 2.0 untuk komponen Jetpack Compose.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showVersionInfoDialog = false }) {
                    Text("Tutup")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    // Simulasi periksa pembaruan
                    android.widget.Toast.makeText(context, "Sudah menggunakan versi terbaru", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Periksa Pembaruan")
                }
            }
        )
    }

    if (showManageBillsDialog) {
        ManageBillsScreen(viewModel = viewModel, onDismiss = { showManageBillsDialog = false })
    }
    
    if (showNotificationSettings) {
        NotificationSettingsScreen(onDismiss = { showNotificationSettings = false })
    }
    
    if (showSecurityDialog && currentTenant != null) {
        ManageSecurityDialog(
            tenant = currentTenant!!,
            viewModel = viewModel,
            onDismiss = { showSecurityDialog = false }
        )
    }
}
@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Buka", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingRowItem(icon: ImageVector, iconColor: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ManageSecurityDialog(
    tenant: Tenant,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Keamanan Akun", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Ubah Kata Sandi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("Kata sandi baru") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        if (newPassword.isNotBlank()) {
                            viewModel.updatePassword(tenant.id, newPassword)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ganti Sandi")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Autentikasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Login Biometrik", fontSize = 14.sp)
                    androidx.compose.material3.Switch(
                        checked = false, // Biometric disabled intentionally
                        onCheckedChange = null,
                        enabled = false
                    )
                }
                Text("Fitur ini ditangguhkan untuk sementara.", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
