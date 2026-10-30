package com.ambatrakan.sigma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.model.TransactionLog
import com.ambatrakan.sigma.data.model.KasLog
import com.ambatrakan.sigma.data.model.PaymentStatus
import com.ambatrakan.sigma.theme.GreenStatus
import com.ambatrakan.sigma.theme.YellowStatus
import com.ambatrakan.sigma.theme.OrangeText
import com.ambatrakan.sigma.theme.AccentEnergy
import com.ambatrakan.sigma.theme.YellowContainerLight

import com.ambatrakan.sigma.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatRupiah(amount: Double): String {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0
    return formatRp.format(amount)
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val currentTenant by viewModel.currentTenant.collectAsState()
    val isAdmin = currentTenant?.role == "ADMIN"
    
    val allLogs by viewModel.allTransactionLogs.collectAsState()
    val billingItems by viewModel.billingItems.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val activeLocationId by viewModel.activeLocationId.collectAsState()
    val tenants by viewModel.tenants.collectAsState()
    val allTenants by viewModel.allTenants.collectAsState()
    val kasLogs by viewModel.kasLogs.collectAsState()

    val totalSaldo = kasLogs.sumOf { if (it.type == "PEMASUKAN") it.amount else -it.amount }
    
    var showKasDialog by remember { mutableStateOf(false) }

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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {


        if (isAdmin) {
            item {
                val pendingLogs = allLogs.filter { it.status == PaymentStatus.PENDING && (it.locationId == activeLocationId || it.locationId == null || activeLocationId == null) }
                AdminPendingApprovalsSection(
                    pendingLogs = pendingLogs,
                    onApprove = { logId -> viewModel.approvePayment(logId) },
                    onReject = { logId -> viewModel.rejectPayment(logId) }
                )
            }
            item {
                HeroBalanceCard(totalSaldo) {
                    showKasDialog = true
                }
            }
        }
        
        item {
            val myLocationBills = billingItems.filter { it.locationId == null || it.locationId == currentTenant?.locationId }
            MyPendingBillsSection(
                myBills = myLocationBills,
                locations = locations,
                tenants = tenants,
                allTenants = allTenants,
                currentTenantId = currentTenant?.id,
                allLogs = allLogs,
                onPay = { item, amount ->
                    viewModel.submitPaymentRequest(item, amount)
                }
            )
        }

        if (isAdmin) {
            item {
                val filteredBillingItems = billingItems.filter { it.billingType != "GLOBAL" && (it.locationId == activeLocationId || activeLocationId == null) }
                QueueScheduleSection(
                    billingItems = filteredBillingItems,
                    tenants = tenants,
                    locations = locations,
                    onRemindPic = { item, customTitle, customMessage -> viewModel.remindPic(item, customTitle, customMessage) }
                )
            }
        }


        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showKasDialog) {
        ManageKasDialog(
            kasLogs = kasLogs,
            isAdmin = isAdmin,
            currentBalance = totalSaldo,
            onDismiss = { showKasDialog = false },
            onAddTransaction = { amount, type, desc ->
                viewModel.addKasTransaction(amount, type, desc)
            }
        )
    }
}

@Composable
fun HeroBalanceCard(balance: Double, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UANG KAS",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatRupiah(balance),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "+12% bulan ini", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun QueueScheduleSection(
    billingItems: List<BillingItem>,
    tenants: List<Tenant>,
    locations: List<Location>,
    onRemindPic: (BillingItem, String?, String?) -> Unit
) {
    var activeReminderItem by remember { mutableStateOf<BillingItem?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var customMessage by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jadwal Giliran", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("LIVE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (billingItems.isEmpty()) {
            Text("Tidak ada jadwal giliran untuk lokasi ini.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                billingItems.forEach { item ->
                    val currentPics = tenants.filter { it.id in item.currentPicTenantIds }
                    
                    val locTenants = tenants.filter { it.locationId == item.locationId }.sortedBy { it.queueOrder }
                    val distinctGroups = locTenants.groupBy { it.queueOrder }.values.toList()
                    val currentGroupIndex = distinctGroups.indexOfFirst { group -> group.any { it.id in item.currentPicTenantIds } }
                    val nextGroup = if (currentGroupIndex != -1 && distinctGroups.size > 1) {
                        distinctGroups[(currentGroupIndex + 1) % distinctGroups.size]
                    } else null

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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (item.name.contains("Galon", true)) Icons.Default.WaterDrop else Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = if (item.name.contains("Galon", true)) MaterialTheme.colorScheme.primary else YellowStatus
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Sekarang: ${if (currentPics.isNotEmpty()) currentPics.joinToString(" & ") { it.name + if(it.isAway) " ✈\uFE0F" else "" } else "Sdr. -"}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Selanjutnya", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(nextGroup?.joinToString(" & ") { it.name + if(it.isAway) " ✈\uFE0F" else "" } ?: "Sdr. -", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { 
                                customTitle = "Giliran Anda Membayar!"
                                customMessage = "Sekarang adalah giliran Anda untuk membayar tagihan ${item.name} sebesar Rp ${item.amount}."
                                activeReminderItem = item
                            }) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Peringatkan Giliran", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeReminderItem != null) {
        val item = activeReminderItem!!
        val currentPics = tenants.filter { it.id in item.currentPicTenantIds }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        AlertDialog(
            onDismissRequest = { activeReminderItem = null },
            title = { Text("Kirim Peringatan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Kirim peringatan ke ${if (currentPics.isNotEmpty()) currentPics.joinToString(" & ") { it.name } else "penghuni"}?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Judul Notifikasi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Isi Pesan") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRemindPic(item, customTitle.ifBlank { null }, customMessage.ifBlank { null })
                    android.widget.Toast.makeText(context, "Peringatan dikirim!", android.widget.Toast.LENGTH_SHORT).show()
                    activeReminderItem = null
                }) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeReminderItem = null }) { Text("Batal") }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPendingBillsSection(
    myBills: List<BillingItem>, 
    locations: List<Location>, 
    tenants: List<Tenant>,
    allTenants: List<Tenant>,
    currentTenantId: Long?,
    allLogs: List<TransactionLog>,
    onPay: (BillingItem, Double) -> Unit
) {
    var selectedTokenItem by remember { mutableStateOf<BillingItem?>(null) }
    var showTokenSheet by remember { mutableStateOf(false) }
    
    if (showTokenSheet && selectedTokenItem != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showTokenSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pilih Nominal Token Listrik", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))
                listOf(20000.0, 50000.0, 100000.0).forEach { amount ->
                    Button(
                        onClick = { 
                            onPay(selectedTokenItem!!, amount)
                            showTokenSheet = false 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(formatRupiah(amount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column {
        Text("Informasi Tagihan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (myBills.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenStatus, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Yeay! Semua aman.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tidak ada tagihan untuk saat ini.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            myBills.forEach { item ->
                if (item.billingType == "GLOBAL" || item.billingType == "SHARED") {
                    val isGlobal = item.billingType == "GLOBAL"
                    val locationName = locations.find { it.id == item.locationId }?.name ?: "GLOBAL"
                    val relevantTenants = if (isGlobal) allTenants else allTenants.filter { it.locationId == item.locationId }
                    
                    SharedOrGlobalBillCard(
                        item = item,
                        currentTenantId = currentTenantId,
                        allLogs = allLogs,
                        tenants = relevantTenants,
                        locationName = if (isGlobal) "SEMUA PENGHUNI" else locationName,
                        onPay = onPay
                    )
                } else {
                    val locationName = locations.find { it.id == item.locationId }?.name ?: "GLOBAL"
                    
                    val isMyTurn = item.currentPicTenantIds.contains(currentTenantId)
                    val pendingLogs = allLogs.filter { it.billingItemId == item.id && it.status == PaymentStatus.PENDING }
                    val isMyPending = pendingLogs.any { it.tenantId == currentTenantId }
                    val lastLog = allLogs.filter { it.billingItemId == item.id }.maxByOrNull { it.timestamp }
                    
                    val currentPics = tenants.filter { it.id in item.currentPicTenantIds }
                    val currentPicName = if (currentPics.isNotEmpty()) currentPics.joinToString(" & ") { it.name } else "Belum ada"

                    val (containerColor, contentColor, iconTint) = when {
                        isMyTurn && !isMyPending -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.error)
                        isMyTurn && isMyPending -> Triple(YellowContainerLight, OrangeText, AccentEnergy) // Yellow/Amber
                        else -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.primary)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isMyTurn && !isMyPending) 0.dp else 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (item.name.contains("Galon", true)) Icons.Default.WaterDrop else Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = iconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(locationName, fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (isMyTurn) {
                                    if (isMyPending) {
                                        Text("Menunggu konfirmasi...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = contentColor)
                                    } else {
                                        Text("Giliran Anda!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    }
                                } else {
                                    if (lastLog != null && lastLog.tenantId == currentTenantId && lastLog.status == PaymentStatus.LUNAS) {
                                        Text("Sudah dibayar.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenStatus)
                                    } else {
                                        Text("Sekarang: $currentPicName", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = contentColor)
                                    }
                                }
                            }
                            
                            if (isMyTurn && !isMyPending) {
                                Button(
                                    onClick = { 
                                        if (item.name.contains("Token Listrik", ignoreCase = true)) {
                                            selectedTokenItem = item
                                            showTokenSheet = true
                                        } else {
                                            onPay(item, item.amount)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Bayar", fontWeight = FontWeight.Bold)
                                }
                            } else if (!isMyTurn && lastLog != null && lastLog.tenantId == currentTenantId && lastLog.status == PaymentStatus.LUNAS) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Selanjutnya", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
                                    Text(currentPicName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPendingApprovalsSection(
    pendingLogs: List<TransactionLog>,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit
) {
    if (pendingLogs.isEmpty()) return

    Column {
        Text("Menunggu Konfirmasi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        pendingLogs.forEach { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YellowContainerLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentEnergy.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentEnergy)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${log.tenantName} telah membayar", fontSize = 14.sp, color = OrangeText)
                            Text(log.billingItemName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OrangeText)
                            Text(formatRupiah(log.amount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OrangeText)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { onReject(log.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Tolak", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onApprove(log.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenStatus),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Terima", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedOrGlobalBillCard(
    item: BillingItem,
    currentTenantId: Long?,
    allLogs: List<TransactionLog>,
    tenants: List<Tenant>,
    locationName: String,
    onPay: (BillingItem, Double) -> Unit
) {
    val cycleStart = item.cycleStartTimestamp ?: 0L
    val relevantLogs = allLogs.filter { it.billingItemId == item.id && it.timestamp >= cycleStart && it.status == PaymentStatus.LUNAS }
    val paidOrder = relevantLogs.sortedBy { it.timestamp }.map { it.tenantId }.distinct()
    
    val pendingLogs = allLogs.filter { it.billingItemId == item.id && it.status == PaymentStatus.PENDING && it.timestamp >= cycleStart }
    val isMyPending = pendingLogs.any { it.tenantId == currentTenantId }
    
    val isMyTurn = !item.paidTenantIds.contains(currentTenantId)
    
    val (containerColor, contentColor, iconTint) = when {
        isMyTurn && !isMyPending -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.error)
        isMyTurn && isMyPending -> Triple(YellowContainerLight, OrangeText, AccentEnergy) // Yellow/Amber
        else -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMyTurn && !isMyPending) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    val titleSuffix = if (item.billingType == "GLOBAL") "TAGIHAN GLOBAL" else "TAGIHAN BERSAMA"
                    Text("$titleSuffix ($locationName)".uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (isMyTurn) {
                        if (isMyPending) {
                            Text("Menunggu konfirmasi...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = contentColor)
                        } else {
                            Text("Segera bayar bagianmu!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        }
                    } else {
                        Text("Sudah Lunas \uD83C\uDF89", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenStatus)
                    }
                }
                
                if (isMyTurn && !isMyPending) {
                    val splitAmount = item.amount / tenants.size.coerceAtLeast(1)
                    val formattedAmount = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).format(splitAmount)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formattedAmount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onPay(item, splitAmount) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bayar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val collectedAmount = relevantLogs.sumOf { it.amount }
            val progress = if (item.amount > 0) (collectedAmount / item.amount).toFloat().coerceIn(0f, 1f) else 0f
            val formattedCollected = formatRupiah(collectedAmount)
            val formattedTotal = formatRupiah(item.amount)
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Terkumpul: $formattedCollected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    Text("Target: $formattedTotal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = GreenStatus,
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("🏆 LEADERBOARD PEMBAYARAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            
            val allTenants = tenants.sortedBy { it.name }
            val paidTenantsList = paidOrder.mapNotNull { id -> allTenants.find { it.id == id } }
            val unpaidTenantsList = allTenants.filter { !item.paidTenantIds.contains(it.id) }
            
            paidTenantsList.forEachIndexed { index, tenant ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tenant.name + if(tenant.isAway) " ✈\uFE0F" else "", fontSize = 14.sp, color = contentColor)
                    }
                    if (index == 0) {
                        Text("🥇 Gilo Amba 😂", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = YellowStatus)
                    } else {
                        Text("✅ Lunas", fontSize = 12.sp, color = GreenStatus)
                    }
                }
            }
            
            val isDeadlinePassed = item.deadlineTimestamp != null && System.currentTimeMillis() > item.deadlineTimestamp
            unpaidTenantsList.forEach { tenant ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentColor.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tenant.name + if(tenant.isAway) " ✈\uFE0F" else "", fontSize = 14.sp, color = contentColor.copy(alpha = 0.5f))
                    }
                    if (isDeadlinePassed || (paidTenantsList.isNotEmpty() && unpaidTenantsList.size == 1)) {
                        Text("😡 Ambakentod", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("⏳ Belum Bayar", fontSize = 12.sp, color = contentColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
