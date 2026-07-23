package com.ambatrakan.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.model.TransactionLog
import com.ambatrakan.sigma.data.repository.AmbatrakanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repository: AmbatrakanRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val currentTenant: StateFlow<Tenant?> = repository.currentTenantId
        .flatMapLatest { id ->
            if (id != null) {
                repository.allTenants.map { list -> list.find { it.id == id } }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Role-aware transaction logs: Admin sees all, User sees only their own
    val transactionLogs: StateFlow<List<TransactionLog>> = currentTenant
        .flatMapLatest { tenant ->
            if (tenant == null) {
                flowOf(emptyList())
            } else if (tenant.role == "ADMIN") {
                repository.allTransactionLogs
            } else {
                repository.getTransactionLogsForTenant(tenant.id)
            }
        }
        .combine(searchQuery) { logs, query ->
            if (query.isBlank()) {
                logs
            } else {
                logs.filter {
                    it.billingItemName.contains(query, ignoreCase = true) ||
                    it.tenantName.contains(query, ignoreCase = true) ||
                    it.status.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
