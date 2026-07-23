package com.ambatrakan.sigma.data.repository

import com.ambatrakan.sigma.data.model.AppState
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.PaymentStatus
import com.ambatrakan.sigma.data.model.TransactionLog

/**
 * Logika alur pembayaran (state machine PENDING → LUNAS/DITOLAK) + rotasi PIC.
 *
 * Diekstrak dari AmbatrakanRepository (god object) agar domain pembayaran berdiri sendiri
 * dan bisa dites. Tidak memegang referensi ke repository; state & efek samping di-inject:
 *  - [getState]/[setState]: akses & mutasi AppState (StateFlow tetap dimiliki repository)
 *  - [persist]: menyimpan state (repository.saveState)
 *  - [notify]: mengirim notifikasi (targetTenantId, title, message)
 *
 * State machine:
 *   submitPaymentRequest → PENDING
 *   approvePayment       → PENDING → LUNAS  (+ rotasi PIC untuk ROLLING)
 *   rejectPayment        → PENDING → DITOLAK
 *   confirmPayment       → langsung LUNAS   (admin bayar tanpa lewat PENDING)
 */
class PaymentService(
    private val getState: () -> AppState,
    private val setState: (AppState) -> Unit,
    private val persist: () -> Unit,
    private val notify: (Long, String, String) -> Unit,
) {
    private fun formatRupiah(amount: Double): String {
        val fmt = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("in", "ID"))
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }

    /**
     * Hitung PIC berikutnya untuk tagihan ROLLING: kelompokkan penghuni lokasi per queueOrder
     * (abaikan yang isAway), pilih grup dengan queueOrder > PIC sekarang, wrap-around bila habis.
     * Mengembalikan Triple(nextPicId?, nextTenantIds, nextTenantName).
     */
    private fun computeNextPic(billingItem: BillingItem): Triple<Long?, List<Long>, String> {
        val allLocTenants = getState().tenants
            .filter { it.locationId == billingItem.locationId }
            .sortedBy { it.queueOrder }
        val currentPicOriginalQueue = allLocTenants.find { it.id in billingItem.currentPicTenantIds }?.queueOrder ?: -1

        val activeTenants = allLocTenants.filter { !it.isAway }
        val distinctGroups = activeTenants.groupBy { it.queueOrder }.values.toList()

        val nextGroup = if (distinctGroups.isEmpty()) {
            null
        } else {
            distinctGroups.find { group -> group.first().queueOrder > currentPicOriginalQueue }
                ?: distinctGroups.first()
        }
        val nextTenantIds = nextGroup?.map { it.id } ?: emptyList()
        val nextTenantName = nextGroup?.joinToString(" & ") { it.name } ?: ""
        return Triple(nextGroup?.firstOrNull()?.id, nextTenantIds, nextTenantName)
    }

    fun hasPendingPaymentForBilling(billingItemId: Long): Boolean =
        getState().transactionLogs.any { it.billingItemId == billingItemId && it.status == PaymentStatus.PENDING }

    /** User submits a payment request → creates a PENDING transaction log. */
    fun submitPaymentRequest(billingItem: BillingItem, amount: Double, tenantId: Long) {
        val tenant = getState().tenants.find { it.id == tenantId } ?: return
        val logId = System.currentTimeMillis() // timestamp sebagai ID untuk cegah tabrakan
        val log = TransactionLog(
            id = logId,
            locationId = billingItem.locationId,
            billingItemId = billingItem.id,
            billingItemName = billingItem.name,
            tenantId = tenantId,
            tenantName = tenant.name,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            status = PaymentStatus.PENDING
        )
        setState(getState().copy(transactionLogs = getState().transactionLogs + log))
        persist()
    }

    fun resetSharedBilling(billingItemId: Long) {
        setState(getState().copy(
            billingItems = getState().billingItems.map {
                if (it.id == billingItemId) it.copy(paidTenantIds = emptyList()) else it
            }
        ))
        persist()
    }

    /** Admin approves a pending payment → status LUNAS, PIC rotates (or adds to paid for SHARED/GLOBAL). */
    fun approvePayment(logId: Long) {
        val log = getState().transactionLogs.find { it.id == logId } ?: return
        val billingItem = getState().billingItems.find { it.id == log.billingItemId } ?: return

        var nextTenantName = ""
        val updatedBillingItems = if (billingItem.billingType == "SHARED" || billingItem.billingType == "GLOBAL") {
            getState().billingItems.map {
                if (it.id == billingItem.id && !it.paidTenantIds.contains(log.tenantId)) {
                    it.copy(paidTenantIds = it.paidTenantIds + log.tenantId)
                } else it
            }
        } else {
            val (nextPicId, nextTenantIds, nm) = computeNextPic(billingItem)
            nextTenantName = nm
            getState().billingItems.map {
                if (it.id == billingItem.id) it.copy(currentPicTenantId = nextPicId, currentPicTenantIds = nextTenantIds) else it
            }
        }

        setState(getState().copy(
            transactionLogs = getState().transactionLogs.map {
                if (it.id == logId) it.copy(status = PaymentStatus.LUNAS) else it
            },
            billingItems = updatedBillingItems
        ))
        persist()

        val hargaStr = formatRupiah(log.amount)
        val notifMessage = if (nextTenantName.isNotEmpty()) {
            "${log.tenantName} sudah membayar tagihan ${billingItem.name} sebesar $hargaStr! Selanjutnya giliran $nextTenantName, jangan sampai lupa!"
        } else {
            "${log.tenantName} sudah membayar tagihan ${billingItem.name} sebesar $hargaStr!"
        }
        val locationTenants = if (billingItem.billingType == "GLOBAL") {
            getState().tenants
        } else {
            getState().tenants.filter { it.locationId == billingItem.locationId }
        }
        locationTenants.forEach { t -> notify(t.id, "Pembayaran Berhasil", notifMessage) }
    }

    fun rejectPayment(logId: Long) {
        setState(getState().copy(
            transactionLogs = getState().transactionLogs.map {
                if (it.id == logId) it.copy(status = PaymentStatus.DITOLAK) else it
            }
        ))
        persist()
    }

    /** Admin directly confirms a payment → creates LUNAS log and rotates PIC immediately. */
    fun confirmPayment(billingItem: BillingItem, amount: Double, status: String = PaymentStatus.LUNAS, overrideTenantId: Long? = null) {
        val isGlobal = billingItem.billingType == "GLOBAL"
        val tenants = if (isGlobal) {
            getState().tenants.sortedBy { it.queueOrder }
        } else {
            getState().tenants.filter { it.locationId == billingItem.locationId }.sortedBy { it.queueOrder }
        }
        if (tenants.isEmpty()) return

        val currentPicId = overrideTenantId ?: (tenants.filter { it.id in billingItem.currentPicTenantIds }.firstOrNull()?.id ?: 0L)
        val currentPicName = tenants.find { it.id == currentPicId }?.name ?: "Tidak Diketahui"

        val logId = System.currentTimeMillis()
        val log = TransactionLog(
            id = logId,
            locationId = billingItem.locationId,
            billingItemId = billingItem.id,
            billingItemName = billingItem.name,
            tenantId = currentPicId,
            tenantName = currentPicName,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            status = status
        )

        var nextTenantName = ""
        val updatedBillingItems = if (status == PaymentStatus.LUNAS) {
            if (isGlobal || billingItem.billingType == "SHARED") {
                getState().billingItems.map {
                    if (it.id == billingItem.id && !it.paidTenantIds.contains(currentPicId)) {
                        it.copy(paidTenantIds = it.paidTenantIds + currentPicId)
                    } else it
                }
            } else {
                val (nextPicId, nextTenantIds, nm) = computeNextPic(billingItem)
                nextTenantName = nm
                getState().billingItems.map {
                    if (it.id == billingItem.id) it.copy(currentPicTenantId = nextPicId, currentPicTenantIds = nextTenantIds) else it
                }
            }
        } else {
            getState().billingItems
        }

        setState(getState().copy(
            transactionLogs = getState().transactionLogs + log,
            billingItems = updatedBillingItems
        ))
        persist()

        if (status == PaymentStatus.LUNAS) {
            val hargaStr = formatRupiah(amount)
            val notifMessageConf = if (nextTenantName.isNotEmpty()) {
                "$currentPicName sudah membayar tagihan ${billingItem.name} sebesar $hargaStr! Selanjutnya giliran $nextTenantName, jangan sampai lupa!"
            } else {
                "$currentPicName sudah membayar tagihan ${billingItem.name} sebesar $hargaStr!"
            }
            tenants.forEach { t -> notify(t.id, "Pembayaran Berhasil", notifMessageConf) }
        }
    }
}
