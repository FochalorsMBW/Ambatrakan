package com.ambatrakan.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.model.KasLog
import com.ambatrakan.sigma.theme.*
import com.ambatrakan.sigma.ui.viewmodel.SettingsViewModel


fun formatDeadlineWithCountdown(deadlineMs: Long): String {
    val diff = deadlineMs - System.currentTimeMillis()
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date(deadlineMs))
    return if (days > 0) {
        "$dateStr ($days Hari Lagi)"
    } else if (days == 0) {
        "$dateStr (Hari Ini)"
    } else {
        "$dateStr (Terlewat)"
    }
}

@Composable
fun FullScreenManageDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
fun ManageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SectionHeader(title: String, badgeText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = badgeText.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ManagePrimaryButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ListCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun ListItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    isLinkedWithAbove: Boolean = false,
    onToggleLinkAbove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (onToggleLinkAbove != null) {
                val linkIcon = if (isLinkedWithAbove) Icons.Default.LinkOff else Icons.Default.Link
                val tint = if (isLinkedWithAbove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(linkIcon, contentDescription = "Toggle Link", tint = tint, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onToggleLinkAbove).padding(4.dp))
            }
            if (onMoveUp != null) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onMoveUp).padding(4.dp))
            }
            if (onMoveDown != null) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onMoveDown).padding(4.dp))
            }
            if (onEdit != null) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onEdit).padding(4.dp))
            }
            if (onDelete != null) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedStatus, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onDelete).padding(4.dp))
            }
        }
    }
}

@Composable
fun BottomDoneButton(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        ManagePrimaryButton(text = "Selesai", onClick = onDismiss)
    }
}

@Composable
fun ManageOccupantsScreen(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val tenants by viewModel.tenants.collectAsState()
    var newName by remember { mutableStateOf("") }
    var newInitials by remember { mutableStateOf("") }
    var activeDeleteTenant by remember { mutableStateOf<Tenant?>(null) }
    var activeEditTenant by remember { mutableStateOf<Tenant?>(null) }

    FullScreenManageDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxSize()) {
            ManageTopBar(title = "Kelola Penghuni", onBack = onDismiss)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SectionHeader(title = "Tambah Penghuni Baru")
                FormCard {
                    ManageTextField(value = newName, onValueChange = { newName = it }, placeholder = "Nama Penghuni")
                    ManageTextField(value = newInitials, onValueChange = { newInitials = it.take(2).uppercase() }, placeholder = "Inisial (Maks 2 Huruf)")
                    ManagePrimaryButton(text = "Tambahkan", icon = Icons.Default.Add) {
                        if (newName.isNotBlank() && newInitials.isNotBlank()) { viewModel.addTenant(newName, newInitials); newName = ""; newInitials = "" }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader(title = "Daftar Penghuni", badgeText = "${tenants.size} PENGHUNI")
                if (tenants.isNotEmpty()) {
                    ListCard {
                        tenants.forEachIndexed { index, tenant ->
                            val statusText = if (tenant.isAway) " (Cuti/Pulang)" else ""
                            val isLinked = index > 0 && tenant.queueOrder == tenants[index - 1].queueOrder
                            ListItemRow(
                                icon = Icons.Default.Person,
                                title = tenant.name + statusText,
                                subtitle = "Inisial: ${tenant.initials} • Antrean: ${tenant.queueOrder}",
                                onEdit = { activeEditTenant = tenant },
                                onDelete = { activeDeleteTenant = tenant },
                                onMoveUp = if (index > 0) { { viewModel.moveTenantUp(tenant) } } else null,
                                onMoveDown = if (index < tenants.size - 1) { { viewModel.moveTenantDown(tenant) } } else null,
                                isLinkedWithAbove = isLinked,
                                onToggleLinkAbove = if (index > 0) { { viewModel.toggleTenantLink(tenant, tenants[index - 1]) } } else null
                            )
                            if (index < tenants.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            BottomDoneButton(onDismiss = onDismiss)
        }
        
        if (activeEditTenant != null) {
            val tenant = activeEditTenant!!
            var isAway by remember { mutableStateOf(tenant.isAway) }
            
            AlertDialog(
                onDismissRequest = { activeEditTenant = null },
                title = { Text("Edit Penghuni", fontWeight = FontWeight.Bold) },
                text = { 
                    Column {
                        Text(tenant.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sedang Pulang / Cuti", modifier = Modifier.weight(1f))
                            Switch(
                                checked = isAway,
                                onCheckedChange = { isAway = it }
                            )
                        }
                        Text("Jika diaktifkan, penghuni ini akan dilewati pada tagihan bergilir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { 
                    Button(onClick = { 
                        viewModel.toggleAwayStatus(tenant.id, isAway)
                        activeEditTenant = null 
                    }) { 
                        Text("Simpan") 
                    } 
                },
                dismissButton = { 
                    TextButton(onClick = { activeEditTenant = null }) { Text("Batal") } 
                }
            )
        }

        if (activeDeleteTenant != null) {
            val tenant = activeDeleteTenant!!
            AlertDialog(
                onDismissRequest = { activeDeleteTenant = null },
                title = { Text("Hapus Penghuni?", fontWeight = FontWeight.Bold) },
                text = { Text("Yakin ingin menghapus ${tenant.name} dari lokasi ini?") },
                confirmButton = { Button(onClick = { viewModel.removeTenant(tenant); activeDeleteTenant = null }, colors = ButtonDefaults.buttonColors(containerColor = RedStatus)) { Text("Hapus", color = MaterialTheme.colorScheme.onError) } },
                dismissButton = { TextButton(onClick = { activeDeleteTenant = null }) { Text("Batal") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBillsScreen(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val billingItems by viewModel.billingItems.collectAsState()
    var newBillName by remember { mutableStateOf("") }
    var newBillAmount by remember { mutableStateOf("") }
    var newBillType by remember { mutableStateOf("SHARED") }
    var activeEditBillingItem by remember { mutableStateOf<BillingItem?>(null) }
    var activeDeleteBillingItem by remember { mutableStateOf<BillingItem?>(null) }

    FullScreenManageDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ManageTopBar(title = "Tagihan", onBack = onDismiss)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(title = "Tambah Tagihan Baru")
                FormCard {
                    ManageTextField(value = newBillName, onValueChange = { newBillName = it }, placeholder = "Nama Tagihan (Maks 3 Kata)")
                    ManageTextField(value = newBillAmount, onValueChange = { if(it.isEmpty() || it.all { char -> char.isDigit() }) newBillAmount = it }, placeholder = "Nominal (Misal: 50000)")
                    
                    Text("Tipe Tagihan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val types = listOf("ROLLING" to "Giliran (cth: Galon)", "DEMAND" to "Kapan Saja (cth: Token)", "SHARED" to "Patungan (cth: Wi-Fi)", "GLOBAL" to "Tetap (Semua Penghuni)")
                    val editTypes = listOf("ROLLING" to "Giliran (cth: Galon)", "DEMAND" to "Kapan Saja (cth: Token)", "SHARED" to "Patungan (cth: Wi-Fi)", "GLOBAL" to "Tetap (Semua Penghuni)")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            editTypes.forEach { (type, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (newBillType == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant).clickable { newBillType = type }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (newBillType == type), onClick = { newBillType = type })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    ManagePrimaryButton(text = "Tambahkan", icon = Icons.Default.Add) {
                        val amt = newBillAmount.toDoubleOrNull()
                        if (newBillName.isNotBlank() && amt != null) { viewModel.addBillingItem(newBillName, amt, newBillType); newBillName = ""; newBillAmount = "" }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader(title = "Daftar Tagihan", badgeText = "${billingItems.size} TAGIHAN")
                if (billingItems.isNotEmpty()) {
                    ListCard {
                        billingItems.forEachIndexed { index, b ->
                            ListItemRow(
                                icon = Icons.Default.Receipt,
                                title = b.name,
                                subtitle = "Rp ${b.amount.toLong()} • ${b.billingType}",
                                onEdit = { activeEditBillingItem = b },
                                onDelete = { activeDeleteBillingItem = b }
                            )
                            if (index < billingItems.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            BottomDoneButton(onDismiss = onDismiss)
        }

        if (activeEditBillingItem != null) {
            val b = activeEditBillingItem!!
            var editName by remember { mutableStateOf(b.name) }
            var editAmount by remember { mutableStateOf(b.amount.toLong().toString()) }
            var editType by remember { mutableStateOf(b.billingType) }
            var deadlineTimestamp by remember { mutableStateOf<Long?>(null) }
            
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = deadlineTimestamp ?: System.currentTimeMillis()
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            datePickerState.selectedDateMillis?.let {
                                val cal = java.util.Calendar.getInstance()
                                cal.timeInMillis = it
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                cal.set(java.util.Calendar.MINUTE, 59)
                                cal.set(java.util.Calendar.SECOND, 59)
                                deadlineTimestamp = cal.timeInMillis
                            }
                            showDatePicker = false 
                        }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            headlineContentColor = MaterialTheme.colorScheme.onSurface,
                            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            subheadContentColor = MaterialTheme.colorScheme.onSurface,
                            yearContentColor = MaterialTheme.colorScheme.onSurface,
                            currentYearContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedYearContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                            dayContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedDayContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                            todayContentColor = MaterialTheme.colorScheme.primary,
                            todayDateBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            
            Dialog(onDismissRequest = { activeEditBillingItem = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        Text("Edit Tagihan", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        ManageTextField(value = editName, onValueChange = { editName = it }, placeholder = "Nama Tagihan")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ManageTextField(value = editAmount, onValueChange = { if(it.isEmpty() || it.all { char -> char.isDigit() }) editAmount = it }, placeholder = "Nominal")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Tipe Tagihan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val editTypes = listOf("ROLLING" to "Giliran (cth: Galon)", "DEMAND" to "Kapan Saja (cth: Token)", "SHARED" to "Patungan (cth: Wi-Fi)", "GLOBAL" to "Tetap (Semua Penghuni)")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            editTypes.forEach { (type, desc) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (editType == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant).clickable { editType = type }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = (editType == type), onClick = { editType = type })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        if (editType == "GLOBAL" || editType == "SHARED") {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Batas Waktu (Opsional)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ManageTextField(
                                    value = deadlineTimestamp?.let { formatDeadlineWithCountdown(it) } ?: "",
                                    onValueChange = {},
                                    placeholder = "Pilih Batas Waktu (Tanggal)"
                                )
                                Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { showDatePicker = true })
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    val newDeadline = deadlineTimestamp ?: (System.currentTimeMillis() + (24L * 60 * 60 * 1000))
                                    val updatedBill = b.copy(
                                        name = editName,
                                        amount = editAmount.toDoubleOrNull() ?: b.amount,
                                        billingType = editType,
                                        paidTenantIds = emptyList(),
                                        cycleStartTimestamp = System.currentTimeMillis(),
                                        deadlineTimestamp = newDeadline
                                    )
                                    viewModel.updateBillingItem(updatedBill)
                                    activeEditBillingItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentEnergy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Mulai Siklus Baru (Reset)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeEditBillingItem = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    val amt = editAmount.toDoubleOrNull()
                                    if (editName.isNotBlank() && amt != null) { 
                                        viewModel.updateBillingItem(b.copy(name = editName, amount = amt, billingType = editType))
                                        activeEditBillingItem = null 
                                    } 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text("Simpan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                            } 
                        }
                    }
                }
            }
        }

        if (activeDeleteBillingItem != null) {
            val b = activeDeleteBillingItem!!
            Dialog(onDismissRequest = { activeDeleteBillingItem = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Hapus Tagihan?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tagihan '${b.name}' akan dihapus secara permanen dari daftar tagihan.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeDeleteBillingItem = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.deleteBillingItem(b); activeDeleteBillingItem = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Hapus", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageKasDialog(
    kasLogs: List<KasLog>,
    isAdmin: Boolean,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onAddTransaction: (Double, String, String) -> Unit
) {
    FullScreenManageDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Riwayat Uang Kas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            var showInputForm by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Kas Saat Ini", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatRupiah(currentBalance),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (isAdmin) {
                    item {
                        Button(
                            onClick = { showInputForm = !showInputForm },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (showInputForm) "Tutup Form Input" else "+ Tambah Transaksi Kas")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (showInputForm) {
                        item {
                            var amount by remember { mutableStateOf("") }
                            var type by remember { mutableStateOf("PEMASUKAN") }
                            var description by remember { mutableStateOf("") }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Input Transaksi Baru", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = type == "PEMASUKAN",
                                            onClick = { type = "PEMASUKAN" },
                                            label = { Text("Pemasukan") },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenStatus, selectedLabelColor = Color.White)
                                        )
                                        FilterChip(
                                            selected = type == "PENGELUARAN",
                                            onClick = { type = "PENGELUARAN" },
                                            label = { Text("Pengeluaran") },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedStatus, selectedLabelColor = Color.White)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = { amount = it },
                                        label = { Text("Nominal (Rp)") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Keterangan") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            val amt = amount.toDoubleOrNull()
                                            if (amt != null && description.isNotBlank()) {
                                                onAddTransaction(amt, type, description)
                                                amount = ""
                                                description = ""
                                                showInputForm = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Simpan Transaksi")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                item {
                    Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (kasLogs.isEmpty()) {
                    item {
                        Text("Belum ada riwayat transaksi kas.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    }
                } else {
                    items(kasLogs) { log ->
                        val isIncome = log.type == "PEMASUKAN"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.description, fontWeight = FontWeight.Medium)
                                    val df = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                                    Text(df.format(java.util.Date(log.timestamp)), fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(
                                    text = (if (isIncome) "+" else "-") + formatRupiah(log.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) GreenStatus else RedStatus
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

