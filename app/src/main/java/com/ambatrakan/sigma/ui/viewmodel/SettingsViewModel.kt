package com.ambatrakan.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ambatrakan.sigma.data.model.BillingItem
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.model.TenantPreferences
import com.ambatrakan.sigma.data.repository.AmbatrakanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(private val repository: AmbatrakanRepository) : ViewModel() {

    val locations: StateFlow<List<Location>> = repository.locations
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeLocationId = MutableStateFlow<Long?>(null)

    val tenants: StateFlow<List<Tenant>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getTenantsForLocation(id) else flowOf(emptyList())
        }
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

    val billingItems: StateFlow<List<BillingItem>> = activeLocationId
        .flatMapLatest { id ->
            if (id != null) repository.getBillingItemsForLocation(id) else flowOf(emptyList())
        }
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

    fun updatePassword(tenantId: Long, newPassword: String) {
        val t = tenants.value.find { it.id == tenantId } ?: return
        repository.updateTenant(t.copy(password = newPassword))
    }

    fun updateBiometrics(tenantId: Long, enabled: Boolean) {
        val t = tenants.value.find { it.id == tenantId } ?: return
        repository.updateTenant(t.copy(preferences = t.preferences.copy(useBiometrics = enabled)))
    }

    fun addTenant(name: String, initials: String) {
        val locationId = activeLocationId.value ?: return
        viewModelScope.launch {
            val currentTenants = tenants.value
            val nextQueueOrder = if (currentTenants.isNotEmpty()) {
                currentTenants.maxOf { it.queueOrder } + 1
            } else {
                0
            }
            repository.insertTenant(
                name = name,
                initials = initials.uppercase(),
                locationId = locationId,
                queueOrder = nextQueueOrder
            )
        }
    }

    fun removeTenant(tenant: Tenant) {
        viewModelScope.launch {
            repository.deleteTenant(tenant)
            // Re-order remaining tenants' queueOrder so there are no gaps
            val remainingTenants = tenants.value.filter { it.id != tenant.id }
            val reordered = remainingTenants.mapIndexed { index, item ->
                item.copy(queueOrder = index)
            }
            repository.updateTenants(reordered)
        }
    }

    fun reorderTenants(newOrder: List<Tenant>) {
        viewModelScope.launch {
            val updated = newOrder.mapIndexed { index, tenant ->
                tenant.copy(queueOrder = index)
            }
            repository.updateTenants(updated)
        }
    }

    fun moveTenantUp(tenant: Tenant) {
        val currentList = tenants.value
        val index = currentList.indexOfFirst { it.id == tenant.id }
        if (index > 0) {
            val tenantAbove = currentList[index - 1]
            viewModelScope.launch {
                repository.swapTenantOrder(tenant, tenantAbove)
            }
        }
    }

    fun moveTenantDown(tenant: Tenant) {
        val currentList = tenants.value
        val index = currentList.indexOfFirst { it.id == tenant.id }
        if (index != -1 && index < currentList.size - 1) {
            val tenantBelow = currentList[index + 1]
            viewModelScope.launch {
                repository.swapTenantOrder(tenant, tenantBelow)
            }
        }
    }

    fun toggleTenantLink(tenant: Tenant, tenantAbove: Tenant) {
        viewModelScope.launch {
            repository.toggleTenantLink(tenant, tenantAbove)
        }
    }

    fun updateTenantPreferences(tenantId: Long, prefs: TenantPreferences) {
        repository.updateTenantPreferences(tenantId, prefs)
    }

    
    fun updateProfile(name: String, email: String) {
        val tenantId = currentTenant.value?.id ?: return
        repository.updateTenantProfile(tenantId, name, email)
    }

    fun toggleAwayStatus(tenantId: Long, isAway: Boolean) {
        repository.toggleTenantAwayStatus(tenantId, isAway)
    }

    fun logout() {
        viewModelScope.launch {
            repository.selectCurrentTenant(null)
        }
    }

    
    fun addBillingItem(name: String, amount: Double, type: String) {
        val activeLocId = activeLocationId.value ?: return
        viewModelScope.launch {
            val locationIdToSave = if (type == "GLOBAL") null else activeLocId
            var firstTenantIds = emptyList<Long>()
            if (type != "GLOBAL") {
                val locationTenants = repository.getTenantsForLocation(activeLocId).stateIn(viewModelScope).value.sortedBy { it.queueOrder }
                val firstGroup = locationTenants.groupBy { it.queueOrder }.values.firstOrNull() ?: emptyList()
                firstTenantIds = firstGroup.map { it.id }
            }
            repository.insertBillingItem(name, locationIdToSave, type, amount, firstTenantIds)
        }
    }

    fun updateBillingItem(item: BillingItem) {
        viewModelScope.launch {
            repository.updateBillingItem(item)
        }
    }

    fun deleteBillingItem(item: BillingItem) {
        viewModelScope.launch {
            repository.deleteBillingItem(item)
        }
    }

    fun resetGlobalBill(item: BillingItem) {
        viewModelScope.launch {
            val updated = item.copy(
                paidTenantIds = emptyList(),
                cycleStartTimestamp = System.currentTimeMillis()
            )
            repository.updateBillingItem(updated)
        }
    }
}
