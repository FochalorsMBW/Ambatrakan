package com.ambatrakan.sigma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambatrakan.sigma.data.model.TransactionLog
import com.ambatrakan.sigma.data.model.PaymentStatus
// removed import
import com.ambatrakan.sigma.ui.viewmodel.HistoryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val transactionLogs by viewModel.transactionLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Aktivitas Terkini", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Linimasa transaksi Anda", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saring", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Cari transaksi, nama, tanggal...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        val groupedLogs = remember(transactionLogs) {
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_YEAR)
            transactionLogs.groupBy { 
                cal.timeInMillis = it.timestamp
                val logDay = cal.get(Calendar.DAY_OF_YEAR)
                when (today - logDay) {
                    0 -> "HARI INI"
                    1 -> "KEMARIN"
                    else -> formatter.format(Date(it.timestamp)).uppercase()
                }
            }
        }

        val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("in", "ID")) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp + contentPadding.calculateBottomPadding())
        ) {
            groupedLogs.forEach { (dateHeader, logs) ->
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .drawBehind {
                                    drawLine(
                                        color = Color.LightGray,
                                        start = Offset(size.width / 2, size.height),
                                        end = Offset(size.width / 2, size.height + 60.dp.toPx()),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(dateHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                itemsIndexed(items = logs, key = { _, log -> log.id }) { index, log ->
                    val isLast = index == logs.size - 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (!isLast) {
                                    drawLine(
                                        color = Color.LightGray,
                                        start = Offset(16.dp.toPx(), 0f),
                                        end = Offset(16.dp.toPx(), size.height + 32.dp.toPx()),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                } else {
                                     drawLine(
                                        color = Color.LightGray,
                                        start = Offset(16.dp.toPx(), 0f),
                                        end = Offset(16.dp.toPx(), size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Icons.Default.ErrorOutline else Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Color(0xFFDC2626) else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(log.billingItemName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(log.tenantName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        timeFormatter.format(Date(log.timestamp)) + " • ID: #${log.id}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val isExpense = true
                                    Text(
                                        (if (isExpense) "-" else "") + formatRupiah(log.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Color(0xFFDC2626) else Color(0xFF059669)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (log.status == PaymentStatus.PENDING) "TERTUNDA" else if (log.status == PaymentStatus.DITOLAK) "GAGAL" else "SELESAI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (log.status == PaymentStatus.PENDING || log.status == PaymentStatus.DITOLAK) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
