# 🕌 ParkMasjid  
*Aplikasi Mobile Sistem Parkir Masjid Berbasis IoT dengan Firebase Realtime Database*

![Banner](docs/images/banner.png)

---

## 📖 Deskripsi  
**ParkMasjid** adalah aplikasi mobile yang membantu jamaah masjid untuk mengetahui ketersediaan lahan parkir secara **real-time**.  
Aplikasi ini terintegrasi dengan perangkat IoT yang menghitung kendaraan masuk/keluar, lalu menyimpan datanya ke **Firebase Realtime Database**.  

Dengan aplikasi ini:  
- Jamaah dapat melihat **jumlah slot parkir motor & mobil yang tersedia**.  
- Admin/takmir masjid bisa mengatur **kapasitas parkir** sesuai kondisi lapangan.  
- Jadwal sholat dapat diperbarui dan ditampilkan untuk jamaah.  
- Notifikasi diberikan ketika **waktu sholat tiba**.  

---

## ✨ Fitur Utama  
- 🚗 **Monitoring Parkir**: Tampilkan slot parkir motor & mobil secara real-time.  
- ⏰ **Jadwal Sholat**: Admin dapat mengupdate jadwal, jamaah bisa langsung melihat.  
- 🔔 **Notifikasi**: Reminder adzan.  
- 🔑 **Role Access**:  
  - Jamaah → hanya melihat info parkir & jadwal sholat.  
  - Admin → bisa mengubah kapasitas parkir & jadwal sholat.  

---

## 🛠️ Teknologi yang Digunakan  
- **Kotlin** (Android Studio)  
- **Firebase Realtime Database**  
- **Firebase Authentication** (untuk akses admin)  
- **Material Design Components**  

---

## 📸 Tampilan Aplikasi  
*(Contoh, ganti sesuai screenshot-mu)*  

| Halaman Utama | Jadwal Sholat | Admin Panel |
|---------------|---------------|-------------|
| ![Home](docs/images/home.png) | ![Jadwal](docs/images/jadwal.png) | ![Admin](docs/images/admin.png) |

---

## 🚀 Instalasi & Menjalankan Projek  

1. Unduh/Clone repository ini:
   ```bash
   git clone https://github.com/username/parkmasjid.git
2. Buka proyek menggunakan Android Studio.
3. Pastikan sudah terhubung dengan Firebase Project (Realtime Database & Authentication).
4. Klik Run ▶ untuk menjalankan aplikasi pada emulator atau perangkat Android.
