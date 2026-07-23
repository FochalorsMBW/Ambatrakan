package com.ambatrakan.sigma.data.repository

import android.content.Context
import com.ambatrakan.sigma.data.local.NotificationPreferences
import com.ambatrakan.sigma.data.model.AppState
import com.ambatrakan.sigma.data.model.NotificationLog
import com.ambatrakan.sigma.utils.NotificationHelper
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Semua concern notifikasi diekstrak dari AmbatrakanRepository (god object):
 *  - Kirim notifikasi antar-client (tulis ke koleksi Firestore `notifications`; push FCM ditangani Cloud Function)
 *  - Dengarkan notifikasi masuk untuk tenant aktif & tampilkan (in-app + sistem)
 *  - Pusat notifikasi in-app (simpan/tandai dibaca)
 *  - Worker pengingat deadline (anti-spam, cek per jam)
 *
 * State & efek samping di-inject agar tidak memegang referensi ke repository:
 *  - [context]: untuk NotificationHelper & NotificationPreferences
 *  - [getState]/[setState]: akses & mutasi AppState
 *  - [persist]: simpan state ke lokal + Firestore (repository.saveState)
 *  - [persistLocalOnly]: simpan state ke file lokal saja (cegah write-amplification saat menerima notifikasi)
 *  - [currentTenantId]: id tenant yang sedang login (untuk worker deadline)
 *  - [scope]: coroutine scope milik repository
 */
class NotificationService(
    private val context: Context,
    private val getState: () -> AppState,
    private val setState: (AppState) -> Unit,
    private val persist: () -> Unit,
    private val persistLocalOnly: () -> Unit,
    private val currentTenantId: () -> Long?,
    private val scope: CoroutineScope,
) {
    private var listenerRegistration: ListenerRegistration? = null
    private var lastNotificationTime: Long = System.currentTimeMillis()

    // "billId:deadlineTimestamp" yang sudah dinotifikasi — cegah notifikasi berulang untuk deadline sama.
    private val notifiedDeadlines = mutableSetOf<String>()

    /**
     * Kirim notifikasi ke tenant target. Cukup tulis ke Firestore; pengiriman FCM (push saat app mati)
     * ditangani Cloud Function di sisi server (functions/index.js). Client TIDAK memegang service-account key.
     * Saat app hidup, notifikasi in-app ditampilkan oleh [startListening].
     */
    fun sendNotification(targetTenantId: Long, title: String, message: String) {
        try {
            val db = Firebase.firestore
            val notif = hashMapOf(
                "targetTenantId" to targetTenantId,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("notifications").add(notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Mulai mendengarkan notifikasi masuk untuk [tenantId]. Membatalkan listener sebelumnya. */
    fun startListening(tenantId: Long) {
        listenerRegistration?.remove()
        lastNotificationTime = System.currentTimeMillis()
        try {
            val db = Firebase.firestore
            listenerRegistration = db.collection("notifications")
                .whereEqualTo("targetTenantId", tenantId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        for (dc in snapshots.documentChanges) {
                            if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val timestamp = dc.document.getLong("timestamp") ?: 0L
                                if (timestamp > lastNotificationTime) {
                                    val title = dc.document.getString("title") ?: "Pemberitahuan"
                                    val message = dc.document.getString("message") ?: ""

                                    val prefs = NotificationPreferences(context)
                                    var shouldShow = true
                                    if (title.contains("Pembayaran", ignoreCase = true) && !prefs.isPaymentNotificationEnabled()) shouldShow = false
                                    if (title.contains("Admin", ignoreCase = true) && !prefs.isAdminChangesNotificationEnabled()) shouldShow = false

                                    if (shouldShow) {
                                        // Simpan ke pusat notifikasi (lokal saja, jangan push balik ke Firestore),
                                        // lalu tampilkan notifikasi sistem.
                                        addNotificationToCenter(title, message, "INFO", null, tenantId, syncToCloud = false)
                                        NotificationHelper.showNotification(context, title, message)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Hentikan listener notifikasi (dipanggil saat logout). */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /** Worker pengingat deadline: cek tiap jam, satu notifikasi per deadline (anti-spam). */
    fun startDeadlineReminderWorker() {
        scope.launch {
            while (isActive) {
                val state = getState()
                val billsWithDeadline = state.billingItems.filter { it.deadlineTimestamp != null }
                val tenantId = currentTenantId()
                val currentTenant = state.tenants.find { it.id == tenantId }

                if (currentTenant != null) {
                    for (bill in billsWithDeadline) {
                        if (System.currentTimeMillis() > bill.deadlineTimestamp!!) {
                            val isTargetLocation = bill.billingType == "GLOBAL" || bill.locationId == currentTenant.locationId
                            if (isTargetLocation) {
                                val isUnpaid = if (bill.billingType == "GLOBAL" || bill.billingType == "SHARED") {
                                    !bill.paidTenantIds.contains(tenantId)
                                } else {
                                    bill.currentPicTenantIds.contains(tenantId)
                                }
                                val dedupeKey = "${bill.id}:${bill.deadlineTimestamp}"
                                if (isUnpaid && notifiedDeadlines.add(dedupeKey)) {
                                    NotificationHelper.showNotification(
                                        context,
                                        "Deadline Tagihan Terlewati",
                                        "Tagihan ${bill.name} sudah lewat batas waktu. Segera lakukan pembayaran ya!"
                                    )
                                }
                            }
                        }
                    }
                }
                // Cek tiap jam (hemat baterai + tidak spam).
                delay(60 * 60 * 1000L)
            }
        }
    }

    /** Tambah ke pusat notifikasi + tampilkan notifikasi sistem, lalu sinkron ke Firestore. */
    fun pushAndSaveNotification(title: String, message: String, type: String = "INFO", locationId: Long? = null, tenantId: Long? = null) {
        addNotificationToCenter(title, message, type, locationId, tenantId, syncToCloud = true)
        NotificationHelper.showNotification(context, title, message)
    }

    /**
     * Tambahkan entri ke pusat notifikasi in-app.
     * @param syncToCloud jika false, hanya simpan ke file lokal tanpa push ulang ke Firestore
     *   (dipakai listener agar tidak write-amplification saat menerima notifikasi).
     */
    private fun addNotificationToCenter(title: String, message: String, type: String, locationId: Long?, tenantId: Long?, syncToCloud: Boolean) {
        val st = getState()
        val newNotification = NotificationLog(
            id = st.nextNotificationId,
            locationId = locationId,
            tenantId = tenantId,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        setState(st.copy(
            notifications = listOf(newNotification) + st.notifications,
            nextNotificationId = st.nextNotificationId + 1
        ))
        if (syncToCloud) persist() else persistLocalOnly()
    }

    fun markNotificationAsRead(notificationId: Long) {
        val st = getState()
        setState(st.copy(notifications = st.notifications.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }))
        persist()
    }

    fun markAllNotificationsAsRead() {
        val st = getState()
        setState(st.copy(notifications = st.notifications.map { it.copy(isRead = true) }))
        persist()
    }
}
