package com.ambatrakan.sigma.data.repository

import android.content.Context
import com.ambatrakan.sigma.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ListenerRegistration
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AmbatrakanRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("ambatrakan_prefs", Context.MODE_PRIVATE)

    private val jsonFile = File(context.filesDir, "ambatrakan_state.json")
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _state = MutableStateFlow(AppState())
    val currentState: AppState get() = _state.value
    
    private val _localCurrentTenantId = MutableStateFlow<Long?>(
        if (prefs.contains("current_tenant_id")) prefs.getLong("current_tenant_id", -1L).takeIf { it != -1L } else null
    )

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    // Concern notifikasi diekstrak ke NotificationService (state & efek samping di-inject).
    private val notificationService = NotificationService(
        context = context,
        getState = { _state.value },
        setState = { _state.value = it },
        persist = { saveState() },
        persistLocalOnly = { saveStateLocalOnly() },
        currentTenantId = { _localCurrentTenantId.value },
        scope = repositoryScope,
    )

    // Domain pembayaran diekstrak ke PaymentService (state & efek samping di-inject).
    private val paymentService = PaymentService(
        getState = { _state.value },
        setState = { _state.value = it },
        persist = { saveState() },
        notify = { tenantId, title, message -> sendNotification(tenantId, title, message) },
    )

    init {
        loadState()
        notificationService.startDeadlineReminderWorker()

        // Initial token sync
        val tenantId = _localCurrentTenantId.value
        val token = prefs.getString("fcm_token", null)
        if (tenantId != null && token != null) {
            updateFcmToken(tenantId, token)
        }
    }

    val locations: Flow<List<Location>> = _state.map { it.locations }
    val allTransactionLogs: Flow<List<TransactionLog>> = _state.map { it.transactionLogs }
    val currentTenantId: Flow<Long?> = _localCurrentTenantId
    val isAdminModeEnabled: Flow<Boolean> = kotlinx.coroutines.flow.combine(_state, _localCurrentTenantId) { state, tenantId ->
        val currentTenant = state.tenants.find { it.id == tenantId }
        currentTenant?.role == "ADMIN"
    }
    val allTenants: Flow<List<Tenant>> = _state.map { it.tenants }
    val notifications: Flow<List<NotificationLog>> = _state.map { it.notifications }

    val pendingPayments: Flow<List<TransactionLog>> = _state.map { state ->
        state.transactionLogs.filter { it.status == PaymentStatus.PENDING }.sortedByDescending { it.timestamp }
    }

    fun getTenantsForLocation(locationId: Long): Flow<List<Tenant>> =
        _state.map { state ->
            state.tenants.filter { it.locationId == locationId }.sortedBy { it.queueOrder }
        }

    fun getBillingItemsForLocation(locationId: Long): Flow<List<BillingItem>> =
        _state.map { state ->
            state.billingItems.filter { it.locationId == locationId || it.locationId == null }
        }

    fun getTransactionLogsForLocation(locationId: Long): Flow<List<TransactionLog>> =
        _state.map { state ->
            state.transactionLogs.filter { it.locationId == locationId }.sortedByDescending { it.timestamp }
        }

    fun getKasLogsForLocation(locationId: Long): Flow<List<KasLog>> =
        _state.map { state ->
            state.kasLogs.filter { it.locationId == locationId }.sortedByDescending { it.timestamp }
        }

    fun getTransactionLogsForTenant(tenantId: Long): Flow<List<TransactionLog>> =
        _state.map { state ->
            state.transactionLogs.filter { it.tenantId == tenantId }.sortedByDescending { it.timestamp }
        }

    fun hasPendingPaymentForBilling(billingItemId: Long): Boolean =
        paymentService.hasPendingPaymentForBilling(billingItemId)

    private fun loadState() {
        try {
            if (jsonFile.exists()) {
                val content = jsonFile.readText()
                val loaded = json.decodeFromString<AppState>(content)
                // Migrasi billing: pastikan tagihan GLOBAL tidak punya locationId/picId
                val fixedBillingItems = loaded.billingItems.map {
                    val migratedIds = if (it.currentPicTenantIds.isEmpty() && it.currentPicTenantId != null) listOf(it.currentPicTenantId) else it.currentPicTenantIds
                    if (it.billingType == "GLOBAL") it.copy(locationId = null, currentPicTenantId = null, currentPicTenantIds = emptyList())
                    else it.copy(currentPicTenantIds = migratedIds)
                }
                _state.value = loaded.copy(billingItems = fixedBillingItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setupFirestoreSync()
    }

    private fun setupFirestoreSync() {
        try {
            val db = Firebase.firestore
            db.collection("appData").document("state").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val jsonString = snapshot.getString("data")
                    if (jsonString != null) {
                        try {
                            val loaded = json.decodeFromString<AppState>(jsonString)
                            val fixedBillingItems = loaded.billingItems.map {
                                val migratedIds = if (it.currentPicTenantIds.isEmpty() && it.currentPicTenantId != null) listOf(it.currentPicTenantId) else it.currentPicTenantIds
                                if (it.billingType == "GLOBAL") {
                                    it.copy(locationId = null, currentPicTenantId = null, currentPicTenantIds = emptyList())
                                } else it.copy(currentPicTenantIds = migratedIds)
                            }
                            val migrated = loaded.copy(billingItems = fixedBillingItems)

                            if (migrated != _state.value) {
                                _state.value = migrated
                                val content = json.encodeToString(AppState.serializer(), migrated)
                                jsonFile.writeText(content)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                } else {
                    seedInitialData() // Call seedInitialData only if document doesn't exist!
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendNotification(targetTenantId: Long, title: String, message: String) =
        notificationService.sendNotification(targetTenantId, title, message)

    fun updateTenantProfile(tenantId: Long, newName: String, newEmail: String) {
        repositoryScope.launch {
            val currentState = _state.value
            val updatedTenants = currentState.tenants.map { t ->
                if (t.id == tenantId) t.copy(name = newName, email = newEmail, initials = newName.take(2).uppercase()) else t
            }
            _state.value = currentState.copy(tenants = updatedTenants)
            saveState()
        }
    }

    fun toggleTenantAwayStatus(tenantId: Long, isAway: Boolean) {
        repositoryScope.launch {
            val currentState = _state.value
            val updatedTenants = currentState.tenants.map { t ->
                if (t.id == tenantId) t.copy(isAway = isAway) else t
            }
            _state.value = currentState.copy(tenants = updatedTenants)
            saveState()
        }
    }

    fun updateFcmToken(tenantId: Long, token: String) {
        if (token.isBlank()) return
        repositoryScope.launch {
            val currentState = _state.value
            val tenant = currentState.tenants.find { it.id == tenantId }
            if (tenant != null && tenant.fcmToken != token) {
                val updatedTenants = currentState.tenants.map { t ->
                    if (t.id == tenantId) t.copy(fcmToken = token) else t
                }
                _state.value = currentState.copy(tenants = updatedTenants)
                saveState()
            }
        }
    }

    fun listenToNotifications(tenantId: Long) = notificationService.startListening(tenantId)

    private fun saveState() {
        // Snapshot state di thread pemanggil (murah), lalu offload I/O berat ke IO scope
        // agar tidak pernah menulis file / jaringan di main thread (mencegah jank/ANR).
        val snapshot = _state.value
        repositoryScope.launch {
            try {
                val content = json.encodeToString(AppState.serializer(), snapshot)
                jsonFile.writeText(content)

                val db = Firebase.firestore
                db.collection("appData").document("state").set(mapOf("data" to content))
                    .addOnFailureListener { e ->
                        android.util.Log.e("FIREBASE_DEBUG", "FAILED to write to Firestore!", e)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seedInitialData() {
        val ambaId = 1L
        val kecambahId = 2L

        val locs = listOf(
            Location(ambaId, "Kontrakan Amba"),
            Location(kecambahId, "Kecambah")
        )

        val tenants = listOf(
            Tenant(1, "Luthfi", "LF", ambaId, 0, "luthfi@ambatrakan.com", "ADMIN", "182930"),
            Tenant(2, "Dicky", "DK", ambaId, 1, "dicky@ambatrakan.com", "USER", "239481"),
            Tenant(3, "Raja", "RJ", ambaId, 2, "raja@ambatrakan.com", "USER", "394857"),
            Tenant(4, "Agil", "AG", ambaId, 3, "agil@ambatrakan.com", "USER", "481029"),
            Tenant(5, "Farli", "FL", kecambahId, 0, "farli@ambatrakan.com", "ADMIN", "510293"),
            Tenant(6, "Hanif", "HN", kecambahId, 1, "hanif@ambatrakan.com", "USER", "629384"),
            Tenant(7, "Fadli", "FD", kecambahId, 2, "fadli@ambatrakan.com", "USER", "738495"),
            Tenant(8, "Ilham", "IH", kecambahId, 3, "ilham@ambatrakan.com", "USER", "849506"),
            Tenant(9, "Aldi", "AD", kecambahId, 4, "aldi@ambatrakan.com", "USER", "950617")
        )

        val bills = listOf(
            BillingItem(1, "Galon Air", ambaId, "WEEKLY", 20000.0, 1, listOf(1)),
            BillingItem(2, "Token Listrik", ambaId, "ON_DEMAND", 100000.0, 1, listOf(1)),
            BillingItem(3, "Galon Air", kecambahId, "WEEKLY", 20000.0, 5, listOf(5)),
            BillingItem(4, "Token Listrik", kecambahId, "ON_DEMAND", 100000.0, 5, listOf(5))
        )

        val now = System.currentTimeMillis()
        val hour = 3600000L
        val day = 86400000L

        _state.value = AppState(
            locations = locs,
            tenants = tenants,
            billingItems = bills,
            transactionLogs = emptyList(),
            nextLocationId = 3,
            nextTenantId = 10,
            nextBillingItemId = 5,
            nextTransactionLogId = 1
        )
        saveState()
    }

    // --- Mutation Methods ---
    fun insertLocation(name: String): Long {
        val id = _state.value.nextLocationId
        val newLoc = Location(id, name)
        _state.value = _state.value.copy(
            locations = _state.value.locations + newLoc,
            nextLocationId = id + 1
        )
        saveState()
        return id
    }

    fun updateLocation(location: Location) {
        _state.value = _state.value.copy(
            locations = _state.value.locations.map { if (it.id == location.id) location else it }
        )
        saveState()
    }

    fun deleteLocation(location: Location) {
        _state.value = _state.value.copy(
            locations = _state.value.locations.filter { it.id != location.id },
            tenants = _state.value.tenants.filter { it.locationId != location.id },
            billingItems = _state.value.billingItems.filter { it.locationId != location.id },
            transactionLogs = _state.value.transactionLogs.filter { it.locationId != location.id }
        )
        saveState()
    }

    fun insertTenant(name: String, initials: String, locationId: Long, queueOrder: Int): Long {
        val id = _state.value.nextTenantId
        val randomPassword = (100000..999999).random().toString()
        val newTenant = Tenant(id, name, initials, locationId, queueOrder, password = randomPassword)
        _state.value = _state.value.copy(
            tenants = _state.value.tenants + newTenant,
            nextTenantId = id + 1
        )
        saveState()
        return id
    }

    fun updateTenant(tenant: Tenant) {
        // Fix Ghost Admin: Prevent demoting the last admin
        val isDemotingAdmin = tenant.role != "ADMIN" && 
                              _state.value.tenants.find { it.id == tenant.id }?.role == "ADMIN"
        val adminCount = _state.value.tenants.count { it.role == "ADMIN" }
        
        val finalTenant = if (isDemotingAdmin && adminCount <= 1) {
            tenant.copy(role = "ADMIN") // Force keep admin
        } else {
            tenant
        }

        _state.value = _state.value.copy(
            tenants = _state.value.tenants.map { if (it.id == finalTenant.id) finalTenant else it }
        )
        saveState()
    }

    fun updateTenants(tenantsList: List<Tenant>) {
        val updatedMap = tenantsList.associateBy { it.id }
        _state.value = _state.value.copy(
            tenants = _state.value.tenants.map { updatedMap[it.id] ?: it }
        )
        saveState()
    }

    fun deleteTenant(tenant: Tenant) {
        _state.value = _state.value.copy(
            tenants = _state.value.tenants.filter { it.id != tenant.id },
            billingItems = _state.value.billingItems.map { 
                val newIds = it.currentPicTenantIds.filter { id -> id != tenant.id }
                if (it.currentPicTenantId == tenant.id) {
                    it.copy(currentPicTenantId = null, currentPicTenantIds = newIds)
                } else {
                    it.copy(currentPicTenantIds = newIds)
                }
            }
        )
        saveState()
    }

    fun insertBillingItem(name: String, locationId: Long?, billingType: String, amount: Double, currentPicTenantIds: List<Long>): Long {
        val id = _state.value.nextBillingItemId
        val safeAmount = amount.coerceAtMost(50000000.0) // Fix Billionaire Typo: Max 50 Juta
        val newItem = BillingItem(id, name, locationId, billingType, safeAmount, currentPicTenantIds = currentPicTenantIds)
        _state.value = _state.value.copy(
            billingItems = _state.value.billingItems + newItem,
            nextBillingItemId = id + 1
        )
        saveState()
        
        val tenantsToNotify = _state.value.tenants.filter { locationId == null || it.locationId == locationId }
        for (t in tenantsToNotify) {
            sendNotification(t.id, "Aktivitas Admin", "Admin menambahkan tagihan baru: $name")
        }
        
        return id
    }

    fun updateBillingItem(item: BillingItem) {
        val safeItem = item.copy(amount = item.amount.coerceAtMost(50000000.0)) // Fix Billionaire Typo
        val enforcedItem = if (safeItem.billingType == "GLOBAL") {
            safeItem.copy(locationId = null, currentPicTenantId = null, currentPicTenantIds = emptyList())
        } else safeItem

        _state.value = _state.value.copy(
            billingItems = _state.value.billingItems.map { if (it.id == enforcedItem.id) enforcedItem else it }
        )
        saveState()
        
        val tenantsToNotify = _state.value.tenants.filter { enforcedItem.locationId == null || it.locationId == enforcedItem.locationId }
        for (t in tenantsToNotify) {
            sendNotification(t.id, "Aktivitas Admin", "Admin telah memperbarui tagihan: ${enforcedItem.name}")
        }
    }

    fun deleteBillingItem(item: BillingItem) {
        _state.value = _state.value.copy(
            billingItems = _state.value.billingItems.filter { it.id != item.id }
        )
        saveState()
        
        val tenantsToNotify = _state.value.tenants.filter { item.locationId == null || it.locationId == item.locationId }
        for (t in tenantsToNotify) {
            sendNotification(t.id, "Aktivitas Admin", "Admin telah menghapus tagihan: ${item.name}")
        }
    }

    fun pushAndSaveNotification(title: String, message: String, type: String = "INFO", locationId: Long? = null, tenantId: Long? = null) =
        notificationService.pushAndSaveNotification(title, message, type, locationId, tenantId)

    private fun saveStateLocalOnly() {
        val snapshot = _state.value
        repositoryScope.launch {
            try {
                jsonFile.writeText(json.encodeToString(AppState.serializer(), snapshot))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markNotificationAsRead(notificationId: Long) = notificationService.markNotificationAsRead(notificationId)

    fun markAllNotificationsAsRead() = notificationService.markAllNotificationsAsRead()

    fun insertTransactionLog(log: TransactionLog): Long {
        val id = System.currentTimeMillis() // Fix: Timestamp based ID
        val newLog = log.copy(id = id)
        _state.value = _state.value.copy(
            transactionLogs = _state.value.transactionLogs + newLog
        )
        saveState()
        return id
    }

    // --- Payment Approval Workflow (didelegasikan ke PaymentService) ---

    fun submitPaymentRequest(billingItem: BillingItem, amount: Double, tenantId: Long) =
        paymentService.submitPaymentRequest(billingItem, amount, tenantId)

    fun resetSharedBilling(billingItemId: Long) =
        paymentService.resetSharedBilling(billingItemId)

    fun approvePayment(logId: Long) = paymentService.approvePayment(logId)

    fun rejectPayment(logId: Long) = paymentService.rejectPayment(logId)

    fun confirmPayment(billingItem: BillingItem, amount: Double, status: String = PaymentStatus.LUNAS, overrideTenantId: Long? = null) =
        paymentService.confirmPayment(billingItem, amount, status, overrideTenantId)

    fun updateBillingPic(billingItemId: Long, tenantId: Long?) {
        val st = _state.value
        val tenant = st.tenants.find { it.id == tenantId }
        
        // Find all tenants in the same room/group
        val newPicIds = if (tenant != null) {
            st.tenants.filter { it.locationId == tenant.locationId && it.queueOrder == tenant.queueOrder }.map { it.id }
        } else {
            emptyList()
        }

        _state.value = st.copy(
            billingItems = st.billingItems.map { 
                if (it.id == billingItemId) it.copy(currentPicTenantId = tenantId, currentPicTenantIds = newPicIds) else it
            }
        )
        saveState()
    }

    fun swapTenantOrder(tenant1: Tenant, tenant2: Tenant) {
        val order1 = tenant1.queueOrder
        val order2 = tenant2.queueOrder
        _state.value = _state.value.copy(
            tenants = _state.value.tenants.map { 
                when (it.id) {
                    tenant1.id -> it.copy(queueOrder = order2)
                    tenant2.id -> it.copy(queueOrder = order1)
                    else -> it
                }
            }
        )
        saveState()
    }

    fun toggleTenantLink(tenant: Tenant, tenantAbove: Tenant) {
        val isLinked = tenant.queueOrder == tenantAbove.queueOrder
        
        if (isLinked) {
            // UNLINK: give tenant a new queueOrder, and push everyone else down
            val newQueueOrder = tenantAbove.queueOrder + 1
            _state.value = _state.value.copy(
                tenants = _state.value.tenants.map {
                    if (it.id == tenant.id) {
                        it.copy(queueOrder = newQueueOrder)
                    } else if (it.locationId == tenant.locationId && it.queueOrder > tenantAbove.queueOrder && it.id != tenant.id) {
                        it.copy(queueOrder = it.queueOrder + 1)
                    } else {
                        it
                    }
                }
            )
        } else {
            // LINK: tenant joins tenantAbove's queueOrder
            _state.value = _state.value.copy(
                tenants = _state.value.tenants.map {
                    if (it.id == tenant.id) {
                        it.copy(queueOrder = tenantAbove.queueOrder)
                    } else {
                        it
                    }
                }
            )
        }
        saveState()
    }

    fun updateTenantPreferences(tenantId: Long, prefs: TenantPreferences) {
        val tenant = _state.value.tenants.find { it.id == tenantId } ?: return
        updateTenant(tenant.copy(preferences = prefs))
    }

    fun selectCurrentTenant(tenantId: Long?): Boolean {
        if (tenantId != null) {
            listenToNotifications(tenantId)
            prefs.edit().putLong("current_tenant_id", tenantId).apply()

            // Jadwalkan reminder harian saat login (sebelumnya hanya dijadwalkan
            // saat BOOT_COMPLETED / buka setting, jadi fitur ini efektif tidak aktif).
            com.ambatrakan.sigma.utils.ReminderScheduler.scheduleReminder(context)

            // Try to update token from prefs if it was refreshed
            val token = prefs.getString("fcm_token", null)
            if (token != null) {
                updateFcmToken(tenantId, token)
            }
        } else {
            // Fix: Clear FCM token on logout to prevent push notification leaks to other users on the same device
            val prevTenantId = prefs.getLong("current_tenant_id", -1L)
            if (prevTenantId != -1L) {
                updateFcmToken(prevTenantId, "")
            }
            
            notificationService.stopListening()
            com.ambatrakan.sigma.utils.ReminderScheduler.cancelReminder(context)
            prefs.edit().remove("current_tenant_id").apply()
        }

        _localCurrentTenantId.value = tenantId
        return true
    }


    fun clearAllLogs() {
        _state.value = _state.value.copy(transactionLogs = emptyList(), nextTransactionLogId = 1)
        saveState()
    }

    fun addKasLog(locationId: Long, amount: Double, type: String, description: String, tenantId: Long) {
        val logId = System.currentTimeMillis()
        val safeAmount = amount.coerceAtMost(50000000.0) // Fix Billionaire Typo
        val log = KasLog(
            id = logId,
            locationId = locationId,
            amount = safeAmount,
            type = type,
            description = description,
            timestamp = System.currentTimeMillis(),
            createdByTenantId = tenantId
        )
        _state.value = _state.value.copy(
            kasLogs = listOf(log) + _state.value.kasLogs
        )
        saveState()
    }
}
