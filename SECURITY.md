# Catatan Keamanan Ambatrakan

Dokumen ini merangkum masalah keamanan yang ditemukan saat audit dan langkah
yang HARUS dilakukan manual di Firebase / Google Cloud Console.

## 🔴 KRITIS #1 — Private key service-account bocor di dalam APK

`app/.../utils/FCMHelper.kt` menanam private key service-account Firebase
(base64, bukan enkripsi). Siapa pun bisa decompile APK, decode, dan mendapat
kredensial admin penuh ke project `ambatrakan`.

Yang sudah dilakukan di kode:
- `FCMHelper.kt` DIHAPUS.
- Pemanggilan FCM dari client dihapus (`AmbatrakanRepository.sendNotification`).
- Dependency `google-auth-library-oauth2-http` dihapus dari `app/build.gradle.kts`.
- Pengiriman FCM dipindah ke Cloud Function (`functions/index.js`).

### WAJIB kamu lakukan sekarang (key lama sudah bocor, harus dicabut):
1. Buka Google Cloud Console → IAM & Admin → Service Accounts.
2. Pilih `firebase-adminsdk-fbsvc@ambatrakan.iam.gserviceaccount.com`.
3. Tab "Keys" → hapus key dengan id `4e9f3729f9a1b30af13c645c6d0d80ab941b5bd7`.
4. Anggap key itu sudah dikompromikan — jangan dipakai lagi di mana pun.

### Deploy Cloud Function pengganti (agar push tetap jalan saat app mati):
```bash
npm install -g firebase-tools
firebase login
cd functions && npm install && cd ..
firebase deploy --only functions
```
Function `sendNotificationOnCreate` akan otomatis mengirim FCM setiap ada
dokumen baru di koleksi `notifications`.

## 🔴 KRITIS #2 — Database publik + password plaintext

Seluruh state (semua user, PIN/password teks polos, token FCM) ada di SATU
dokumen `appData/state` yang bisa dibaca & ditimpa siapa pun tanpa autentikasi.
Login client (`inputPassword == tenant.password`) bukan autentikasi sungguhan.

Yang sudah dilakukan di kode:
- File `firestore.rules` dibuat: batasi akses hanya ke `appData/state` dan
  `notifications` (create/read saja), tolak path lain.

### WAJIB kamu lakukan:
1. Deploy rules: `firebase deploy --only firestore:rules`.
2. Verifikasi di Console → Firestore → Rules bahwa mode "test/open" (allow all)
   sudah tidak aktif.

### Rekomendasi jangka menengah (perlu refactor lebih besar, belum dikerjakan):
- Aktifkan **Firebase Authentication** (Anonymous/Email) agar rules bisa
  memakai `request.auth`. Blok rules "Level aman" sudah disiapkan (berkomentar)
  di `firestore.rules`.
- **Jangan simpan password plaintext.** Minimal hash (mis. bcrypt/PBKDF2) di
  sisi client sebelum disimpan; idealnya pindahkan autentikasi ke Firebase Auth
  dan buang field `password` dari model `Tenant`.
- Pisahkan data sensitif dari `appData/state` (dokumen tunggal) ke koleksi
  ber-dokumen per-tenant, agar rules bisa membatasi baca antar-user.

## Status ringkas

| Item | Status |
|------|--------|
| Cabut FCMHelper + dependency dari client | ✅ Selesai (kode) |
| Cloud Function pengganti | ✅ Dibuat, perlu kamu deploy |
| Revoke key bocor di Console | ⛔ Perlu tindakanmu |
| Firestore rules | ✅ Dibuat, perlu kamu deploy |
| Firebase Auth + hashing password | ⛔ Belum (refactor besar, diskusikan) |
