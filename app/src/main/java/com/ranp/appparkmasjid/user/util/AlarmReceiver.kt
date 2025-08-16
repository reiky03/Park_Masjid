package com.ranp.appparkmasjid.user.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ranp.appparkmasjid.R
import com.ranp.appparkmasjid.SplashScreen

// AlarmReceiver.kt — Menangani notifikasi adzan dari AlarmManager
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_EXTRA = "extra_notification_id" // Kunci pengenal ID notifikasi
        const val SALAT_NAME_EXTRA = "extra_salat_name"           // Kunci pengenal nama salat
        const val CHANNEL_ID = "azan_notification_channel_001"   // ID unik untuk Notification Channel
        const val CHANNEL_NAME = "Notifikasi Adzan"              // Nama channel untuk ditampilkan ke user
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Fungsi ini dipanggil saat alarm aktif atau saat perangkat booting ulang

        // Jika perangkat baru selesai booting
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("AlarmReceiver", "Perangkat selesai boot.")

            // Ambil preferensi pengguna terkait notifikasi adzan
            val preferenceHelper = PreferenceHelper(context)
            val isNotificationEnabled = preferenceHelper.isAzanNotificationEnabled()
            val savedJadwal = preferenceHelper.getSavedJadwalSalat()

            // Jika notifikasi diaktifkan dan ada jadwal salat tersimpan, atur ulang semua alarm
            if (isNotificationEnabled && savedJadwal.isNotEmpty()) {
                Log.i("AlarmReceiver", "Menjadwalkan ulang alarm karena boot.")
                AlarmUtils.rescheduleAlarmsOnBoot(context, savedJadwal, isNotificationEnabled)
            } else {
                Log.i("AlarmReceiver", "Tidak ada alarm yang dijadwalkan ulang.")
            }
            return
        }

        // Jika ini adalah alarm salat biasa, ambil data dari intent
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, 0)
        val salatName = intent.getStringExtra(SALAT_NAME_EXTRA) ?: "Waktu Salat Telah Tiba"

        Log.i("AlarmReceiver", "Alarm diterima untuk: $salatName (ID: $notificationId)")

        // Tampilkan notifikasi untuk waktu salat
        showPrayerTimeNotification(context, salatName, notificationId)
    }

    private fun showPrayerTimeNotification(context: Context, salatName: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent untuk membuka SplashScreen ketika notifikasi diklik
        val notificationIntent = Intent(context, SplashScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Atur flags untuk PendingIntent berdasarkan versi Android
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, notificationId, notificationIntent, pendingIntentFlags)

        // Tentukan suara adzan berdasarkan nama salat
        val soundUri: Uri = if (salatName.equals("Subuh", ignoreCase = true)) {
            Uri.parse("android.resource://${context.packageName}/${R.raw.adzan_subuh}").also {
                Log.d("AlarmReceiver", "Menggunakan suara adzan Subuh.")
            }
        } else {
            Uri.parse("android.resource://${context.packageName}/${R.raw.adzan_biasa}").also {
                Log.d("AlarmReceiver", "Menggunakan suara adzan biasa untuk $salatName.")
            }
        }

        // Buat builder notifikasi dengan suara dan gaya teks besar
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_mosque)
            .setContentTitle("Adzan $salatName Berkumandang")
            .setContentText("Yuk tunaikan Salat $salatName. Cek juga info parkir masjid di sini!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Saatnya Salat $salatName, Jamaah!")
                    .bigText("Sebelum berangkat, pastikan kenyamanan ibadahmu dengan mengecek ketersediaan parkir di Masjid melalui aplikasi.")
            )

        // Buat channel jika perlu (untuk Android 8 ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel khusus untuk menampilkan notifikasi waktu adzan dengan suara."
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(200, 200, 200, 200, 600, 200, 200, 200, 200, 600)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("AlarmReceiver", "Channel notifikasi '$CHANNEL_ID' dibuat atau sudah tersedia.")
        }

        // Tampilkan notifikasi
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.i("AlarmReceiver", "Notifikasi adzan '$salatName' ditampilkan (ID: $notificationId)")
    }
}
