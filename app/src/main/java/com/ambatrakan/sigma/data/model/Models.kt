package com.ambatrakan.sigma.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: Long,
    val name: String
)

@Serializable
data class TenantPreferences(
    val remindOnTurn: Boolean = true,
    val reminderDays: Int = 1,
    val receiveAdminBroadcasts: Boolean = true,
    val useBiometrics: Boolean = true,
    val hideBalances: Boolean = false
)

@Serializable
data class Tenant(
    val id: Long,
    val name: String,
    val initials: String,
    val locationId: Long,
    val queueOrder: Int,
    val email: String = "",
    val role: String = "USER", // "ADMIN" or "USER"
    val password: String = "",
    val preferences: TenantPreferences = TenantPreferences(),
    val fcmToken: String = "",
    val isAway: Boolean = false
)

@Serializable
data class BillingItem(
    val id: Long,
    val name: String,
    val locationId: Long?,
    val billingType: String, // "ROLLING", "DEMAND", "SHARED", "GLOBAL"
    val amount: Double,
    val currentPicTenantId: Long? = null,
    val currentPicTenantIds: List<Long> = emptyList(),
    val paidTenantIds: List<Long> = emptyList(),
    val deadlineTimestamp: Long? = null,
    val cycleStartTimestamp: Long? = null
)

@Serializable
data class TransactionLog(
    val id: Long,
    val locationId: Long?,
    val billingItemId: Long = 0,
    val billingItemName: String,
    val tenantId: Long = 0,
    val tenantName: String,
    val amount: Double,
    val timestamp: Long,
    val status: String // gunakan konstanta PaymentStatus (PENDING/LUNAS/DITOLAK)
)

/**
 * Status pembayaran sebuah TransactionLog. Disimpan sebagai String (agar data JSON/Firestore
 * lama tetap kompatibel), tapi dipusatkan di sini sebagai satu sumber kebenaran — mencegah
 * typo string yang sebelumnya tersebar di repository & seluruh screen.
 *
 * State machine:
 *   submitPaymentRequest → PENDING
 *   approvePayment       → PENDING → LUNAS  (+ rotasi PIC)
 *   rejectPayment        → PENDING → DITOLAK
 *   confirmPayment       → langsung LUNAS   (admin bayar tanpa lewat PENDING)
 */
object PaymentStatus {
    const val PENDING = "PENDING"
    const val LUNAS = "LUNAS"
    const val DITOLAK = "DITOLAK"
}

@Serializable
data class KasLog(
    val id: Long,
    val locationId: Long,
    val amount: Double,
    val type: String, // "PEMASUKAN" or "PENGELUARAN"
    val description: String,
    val timestamp: Long,
    val createdByTenantId: Long
)

@Serializable
data class NotificationLog(
    val id: Long,
    val locationId: Long?, // Null if global/broadcast to all locations
    val tenantId: Long?, // Null if broadcast to all in location
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String = "INFO" // INFO, ALERT, PAYMENT, dst
)

@Serializable
data class AppState(
    val locations: List<Location> = emptyList(),
    val tenants: List<Tenant> = emptyList(),
    val billingItems: List<BillingItem> = emptyList(),
    val transactionLogs: List<TransactionLog> = emptyList(),
    val kasLogs: List<KasLog> = emptyList(),
    val nextLocationId: Long = 1,
    val nextTenantId: Long = 1,
    val nextBillingItemId: Long = 1,
    val nextTransactionLogId: Long = 1,
    val nextKasLogId: Long = 1,
    val nextNotificationId: Long = 1,
    val currentTenantId: Long? = null,
    val notifications: List<NotificationLog> = emptyList()
)
