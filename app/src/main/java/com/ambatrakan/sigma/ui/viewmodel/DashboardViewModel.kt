package com.ambatrakan.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.model.KasLog
import com.ambatrakan.sigma.data.model.TransactionLog
import com.ambatrakan.sigma.data.repository.AmbatrakanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: AmbatrakanRepository) : ViewModel() {

    val locations: StateFlow<List<Location>> = repository.locations
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeLocationId = MutableStateFlow<Long?>(null)

    val billingItems: StateFlow<List<BillingItem>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getBillingItemsForLocation(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tenants: StateFlow<List<Tenant>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getTenantsForLocation(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    val transactionLogs: StateFlow<List<TransactionLog>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getTransactionLogsForLocation(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val kasLogs: StateFlow<List<KasLog>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getKasLogsForLocation(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTransactionLogs: StateFlow<List<TransactionLog>> = repository.allTransactionLogs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isAdminModeEnabled: StateFlow<Boolean> = repository.isAdminModeEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val currentTenant: StateFlow<Tenant?> = repository.currentTenantId
        .flatMapLatest { id ->
            if (id != null) {
                repository.allTenants.map { list -> list.find { it.id == id } }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val pendingPayments: StateFlow<List<TransactionLog>> = repository.pendingPayments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // User-specific transaction logs (filtered by current tenant)
    val userTransactionLogs: StateFlow<List<TransactionLog>> = repository.currentTenantId
        .flatMapLatest { id ->
            if (id != null) repository.getTransactionLogsForTenant(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTenants: StateFlow<List<Tenant>> = repository.allTenants
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            currentTenant.collect { tenant ->
                if (tenant != null) {
                    activeLocationId.value = tenant.locationId
                }
            }
        }
    }

    /** Admin directly confirms a payment (LUNAS + rotate PIC). */
    fun confirmPayment(billingItem: BillingItem, amount: Double, overrideTenantId: Long? = null) {
        viewModelScope.launch {
            repository.confirmPayment(billingItem, amount, com.ambatrakan.sigma.data.model.PaymentStatus.LUNAS, overrideTenantId)
        }
    }

    fun submitPaymentRequest(billingItem: BillingItem, amount: Double) {
        val tenantId = currentTenant.value?.id ?: return
        viewModelScope.launch {
            repository.submitPaymentRequest(billingItem, amount, tenantId)
        }
    }

    /** Admin approves a pending payment. */
    fun approvePayment(logId: Long) {
        viewModelScope.launch {
            repository.approvePayment(logId)
        }
    }

    /** Admin rejects a pending payment. */
    fun rejectPayment(logId: Long) {
        viewModelScope.launch {
            repository.rejectPayment(logId)
        }
    }

    /** Check if a billing item already has a pending payment. */
    fun hasPendingPayment(billingItemId: Long): Boolean {
        return repository.hasPendingPaymentForBilling(billingItemId)
    }

    fun resetSharedBilling(billingItemId: Long) {
        viewModelScope.launch {
            repository.resetSharedBilling(billingItemId)
        }
    }

    fun setActiveLocation(id: Long) {
        viewModelScope.launch {
            activeLocationId.value = id
        }
    }

    fun updateCurrentTenant(tenantId: Long?) {
        viewModelScope.launch {
            repository.selectCurrentTenant(tenantId)
        }
    }

    fun addLocation(name: String) {
        viewModelScope.launch {
            repository.insertLocation(name)
        }
    }

    fun addTenant(name: String, initials: String) {
        val locationId = activeLocationId.value ?: return
        viewModelScope.launch {
            repository.insertTenant(name, initials, locationId, queueOrder = 999)
        }
    }

    fun removeTenant(tenantId: Long) {
        viewModelScope.launch {
            val tenant = tenants.value.find { it.id == tenantId }
            if (tenant != null) {
                repository.deleteTenant(tenant)
            }
        }
    }

    fun addBillingItem(name: String, amount: Double, type: String) {
        val activeLocId = activeLocationId.value ?: return
        viewModelScope.launch {
            val locationIdToSave = if (type == "GLOBAL") null else activeLocId
            val locationTenants = repository.getTenantsForLocation(activeLocId).stateIn(viewModelScope).value.sortedBy { it.queueOrder }
            val firstGroup = locationTenants.groupBy { it.queueOrder }.values.firstOrNull() ?: emptyList()
            val firstTenantIds = firstGroup.map { it.id }
            
            repository.insertBillingItem(
                name = name,
                locationId = locationIdToSave,
                billingType = type,
                amount = amount,
                currentPicTenantIds = firstTenantIds
            )
        }
    }

    fun changeBillingPic(billingItemId: Long, tenantId: Long?) {
        viewModelScope.launch {
            repository.updateBillingPic(billingItemId, tenantId)
        }
    }

    fun remindPic(billingItem: BillingItem, customTitle: String? = null, customMessage: String? = null) {
        if (billingItem.billingType == "SHARED" || billingItem.billingType == "GLOBAL") {
            val allTenantsList = allTenants.value
            val targetTenants = if (billingItem.billingType == "GLOBAL") {
                allTenantsList
            } else {
                allTenantsList.filter { it.locationId == billingItem.locationId }
            }
            
            val unnotifiedTenants = targetTenants.filter { !billingItem.paidTenantIds.contains(it.id) }
            for (tenant in unnotifiedTenants) {
                repository.sendNotification(
                    targetTenantId = tenant.id,
                    title = customTitle ?: "Tagihan Belum Dibayar!",
                    message = customMessage ?: "Ayo segera bayar tagihan ${billingItem.name} sebesar Rp ${billingItem.amount}."
                )
            }
        } else {
            val picIds = billingItem.currentPicTenantIds
            if (picIds.isEmpty()) return
            for (picId in picIds) {
                repository.sendNotification(
                    targetTenantId = picId,
                    title = customTitle ?: "Giliran Anda Membayar!",
                    message = customMessage ?: "Sekarang adalah giliran Anda untuk membayar tagihan ${billingItem.name} sebesar Rp ${billingItem.amount}."
                )
            }
        }
    }

    fun addKasTransaction(amount: Double, type: String, description: String) {
        val locationId = activeLocationId.value ?: return
        val tenantId = currentTenant.value?.id ?: return
        viewModelScope.launch {
            repository.addKasLog(locationId, amount, type, description, tenantId)
        }
    }
}
