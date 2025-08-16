package com.ranp.appparkmasjid.user.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Kelas object untuk mengatur alarm
object AlarmUtils {

    // Daftar nama salat dan ID unik yang akan digunakan untuk requestCode PendingIntent dan ID Notifikasi.
    // Pastikan nama salat (key) konsisten dengan yang diambil dari Firebase/SharedPreferences.
    private val salatScheduleDetails = mapOf(
        "Subuh" to 1001,
        "Dzuhur" to 1002,
        "Ashar" to 1003,
        "Maghrib" to 1004,
        "Isya" to 1005
    )

    /**
     * Menjadwalkan alarm untuk semua waktu salat yang diberikan.
     * @param jadwalSalat Map berisi nama salat (String) dan waktunya (String format "HH:mm").
     */
    @SuppressLint("ScheduleExactAlarm") // Diperlukan jika menargetkan API 31+ dan menggunakan setExact...
    fun setAlarmForAllSalat(context: Context, jadwalSalat: Map<String, String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Periksa izin untuk menjadwalkan alarm presisi (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmUtils", "Aplikasi tidak memiliki izin SCHEDULE_EXACT_ALARM.")
                Toast.makeText(context, "Izin alarm presisi belum diberikan. Notifikasi mungkin tidak akurat.", Toast.LENGTH_LONG).show()
                // Arahkan pengguna ke pengaturan untuk memberikan izin (opsional tapi direkomendasikan)
                // try {
                //     context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                // } catch (e: Exception) {
                //     Log.e("AlarmUtils", "Gagal membuka pengaturan izin alarm presisi.", e)
                // }
                // Jika izin tidak ada, notifikasi yang dijadwalkan dengan setExact... mungkin tidak berfungsi sesuai harapan.
                // Anda bisa mempertimbangkan menggunakan setWindow() sebagai fallback, tapi kurang ideal untuk adzan.
            }
        }
        // Formatter untuk mengubah waktu ke format yang lebih mudah dibaca
        val sdfOutput = SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.getDefault())
        sdfOutput.timeZone = TimeZone.getDefault()
        // Periksa dan jadwalkan alarm untuk setiap salat
        jadwalSalat.forEach { (namaSalat, waktuInput) ->
            // Periksa apakah ID Alarm ada dalam daftar salatScheduleDetails
            val alarmId = salatScheduleDetails[namaSalat]
            if (alarmId == null) {
                Log.e("AlarmUtils", "ID Alarm tidak ditemukan untuk salat: $namaSalat. Alarm tidak disetel.")
                return@forEach // Lanjut ke salat berikutnya jika ID tidak ada
            }
            // Periksa apakah waktu salat valid (tidak kosong dan bukan "-")
            if (waktuInput == "-") {
                Log.w("AlarmUtils", "Waktu untuk $namaSalat belum tersedia ('-'). Alarm tidak disetel.")
                return@forEach // Lewati jika waktu belum valid
            }

            // Parse waktu salat dari format "HH:mm"
            val (hour, minute) = try {
                val parts = waktuInput.split(":")
                if (parts.size == 2) {
                    parts[0].toInt() to parts[1].toInt()
                } else {
                    Log.e("AlarmUtils", "Format waktu salah untuk $namaSalat: '$waktuInput'. Harusnya 'HH:mm'. Alarm tidak disetel.")
                    return@forEach
                }
            } catch (e: NumberFormatException) {
                Log.e("AlarmUtils", "Error parsing waktu untuk $namaSalat: '$waktuInput'.", e)
                return@forEach
            } catch (e: Exception) {
                Log.e("AlarmUtils", "Error tidak diketahui saat parsing waktu untuk $namaSalat: '$waktuInput'.", e)
                return@forEach
            }

            // Buat instance Calendar untuk waktu alarm
            val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                // Set waktu alarm sesuai dengan waktu salat
                // Set waktu alarm untuk hari ini
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Jika waktu alarm hari ini sudah lewat, set alarm untuk besok pada jam yang sama
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Tambah 1 hari
                Log.d("AlarmUtils", "Waktu $namaSalat ($waktuInput) hari ini sudah lewat. Alarm disetel untuk besok: ${sdfOutput.format(calendar.time)}")
            } else {
                Log.d("AlarmUtils", "Alarm untuk $namaSalat ($waktuInput) disetel untuk hari ini: ${sdfOutput.format(calendar.time)}")
            }

            // Intent yang akan dikirim ke AlarmReceiver saat alarm berbunyi
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.NOTIFICATION_ID_EXTRA, alarmId)
                putExtra(AlarmReceiver.SALAT_NAME_EXTRA, namaSalat)
            }

            // Tentukan flags untuk PendingIntent
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            // Buat PendingIntent
            // requestCode (alarmId) harus unik untuk setiap alarm
            val pendingIntent = PendingIntent.getBroadcast(
                context.applicationContext,
                alarmId,
                intent,
                pendingIntentFlags
            )

            // Menjadwalkan alarm yang presisi dan tetap berjalan meski device dalam mode Doze
            try {
                // Gunakan setExactAndAllowWhileIdle untuk alarm yang harus berbunyi tepat waktu
                // bahkan saat perangkat dalam mode Doze (lebih boros baterai tapi penting untuk adzan).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    // Untuk API di bawah 23, setExact sudah cukup presisi dan akan membangunkan perangkat.
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
                Log.i("AlarmUtils", "Alarm BERHASIL disetel untuk $namaSalat pada ${sdfOutput.format(calendar.time)} dengan ID: $alarmId")

            } catch (se: SecurityException) {
                Log.e("AlarmUtils", "SecurityException saat menjadwalkan alarm untuk $namaSalat. Cek izin SCHEDULE_EXACT_ALARM atau USE_EXACT_ALARM.", se)
                Toast.makeText(context, "Gagal menyetel alarm untuk $namaSalat karena masalah izin.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("AlarmUtils", "Exception umum saat menjadwalkan alarm untuk $namaSalat.", e)
                Toast.makeText(context, "Gagal menyetel alarm untuk $namaSalat.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Membatalkan SEMUA alarm waktu salat yang telah dijadwalkan.
    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        salatScheduleDetails.forEach { (namaSalat, alarmId) ->
            val intent = Intent(context, AlarmReceiver::class.java) // Intent harus sama dengan yang digunakan saat set
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE untuk cek apakah ada, IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            // Dapatkan PendingIntent yang sudah ada (jika ada)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                pendingIntentFlags
            )

            if (pendingIntent != null) {
                // Jika PendingIntent ada, batalkan alarm dan PendingIntent-nya
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel() // Juga batalkan PendingIntent-nya
                Log.i("AlarmUtils", "Alarm untuk $namaSalat (ID: $alarmId) berhasil dibatalkan.")
            } else {
                Log.d("AlarmUtils", "Tidak ada alarm yang ditemukan untuk $namaSalat (ID: $alarmId) untuk dibatalkan.")
            }
        }
        Log.i("AlarmUtils", "Semua alarm waktu salat telah dicoba untuk dibatalkan.")
    }

    /**
     * Menjadwalkan ulang semua alarm setelah perangkat selesai boot.
     * Fungsi ini dipanggil dari AlarmReceiver ketika menerima Intent.ACTION_BOOT_COMPLETED.
     */
    fun rescheduleAlarmsOnBoot(context: Context, savedJadwal: Map<String, String>, isNotificationEnabled: Boolean) {
        if (isNotificationEnabled && savedJadwal.isNotEmpty()) {
            Log.i("AlarmUtils", "Menjadwalkan ulang alarm setelah boot...")
            // Panggil fungsi utama untuk menyetel semua alarm berdasarkan jadwal yang tersimpan
            setAlarmForAllSalat(context, savedJadwal)
            Toast.makeText(context, "Notifikasi Adzan berhasil diaktifkan kembali setelah reboot.", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("AlarmUtils", "Tidak ada penjadwalan ulang alarm saat boot: Notifikasi dimatikan atau tidak ada jadwal tersimpan.")
        }
    }
}