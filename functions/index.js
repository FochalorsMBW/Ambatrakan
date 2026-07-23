/**
 * Cloud Functions untuk Ambatrakan.
 *
 * Menggantikan FCMHelper sisi-client yang lama (yang menanamkan private key
 * service-account di dalam APK — celah keamanan kritis). Kredensial admin kini
 * hanya hidup di lingkungan server Cloud Functions, tidak pernah di perangkat.
 *
 * Alur:
 *  1. Client menulis dokumen ke koleksi `notifications`
 *     { targetTenantId, title, message, timestamp }.
 *  2. Function ini terpicu (onCreate), mencari fcmToken tenant target di
 *     dokumen appData/state, lalu mengirim push FCM ke perangkat tersebut.
 *
 * Payload dikirim sebagai `data` (bukan `notification`) agar aplikasi selalu
 * menerima lewat onMessageReceived dan menampilkannya dengan channel/suara
 * kustom sendiri (lihat MyFirebaseMessagingService & NotificationHelper).
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.sendNotificationOnCreate = onDocumentCreated(
  "notifications/{notifId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const notif = snap.data();
    const targetTenantId = notif.targetTenantId;
    const title = notif.title || "Pemberitahuan";
    const message = notif.message || "";

    if (targetTenantId === undefined || targetTenantId === null) {
      console.log("Dokumen notifikasi tanpa targetTenantId, dilewati.");
      return;
    }

    // Ambil fcmToken tenant target dari appData/state.
    const stateDoc = await getFirestore()
      .collection("appData")
      .doc("state")
      .get();

    if (!stateDoc.exists) {
      console.log("appData/state tidak ada.");
      return;
    }

    let appState;
    try {
      appState = JSON.parse(stateDoc.data().data);
    } catch (e) {
      console.error("Gagal parse appData/state:", e);
      return;
    }

    const tenants = appState.tenants || [];
    const tenant = tenants.find((t) => String(t.id) === String(targetTenantId));
    const token = tenant && tenant.fcmToken;

    if (!token) {
      console.log(`Tenant ${targetTenantId} tidak punya fcmToken, dilewati.`);
      return;
    }

    try {
      await getMessaging().send({
        token: token,
        data: {
          title: String(title),
          message: String(message),
        },
        android: {
          priority: "high",
        },
      });
      console.log(`FCM terkirim ke tenant ${targetTenantId}.`);
    } catch (e) {
      console.error("Gagal mengirim FCM:", e);
    }
  }
);
