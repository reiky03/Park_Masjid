package com.ranp.appparkmasjid.user.util

import android.content.Context
import android.content.SharedPreferences

// Kelas helper untuk menyimpan dan mengambil data dari SharedPreferences
class PreferenceHelper(context: Context) {

    // Nama file SharedPreferences
    private val PREFS_NAME = "azan_app_prefs"
    // Key untuk menyimpan status aktivasi notifikasi adzan
    private val KEY_AZAN_NOTIFICATION_ENABLED = "azan_notification_enabled_status"
    // Prefix untuk menyimpan masing-masing waktu salat
    private val KEY_JADWAL_PREFIX = "jadwal_waktu_"

    // Inisialisasi SharedPreferences
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Menyimpan status apakah notifikasi adzan diaktifkan atau tidak oleh pengguna.
     * @param isEnabled true jika diaktifkan, false jika tidak.
     */
    fun setAzanNotificationEnabled(isEnabled: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(KEY_AZAN_NOTIFICATION_ENABLED, isEnabled)
        editor.apply() // Gunakan apply() untuk penyimpanan asinkron
    }

    /**
     * Mendapatkan status apakah notifikasi adzan diaktifkan atau tidak.
     * @return true jika diaktifkan, false jika tidak atau belum pernah disimpan (default false).
     */
    fun isAzanNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_AZAN_NOTIFICATION_ENABLED, false)
    }

    /**
     * Menyimpan jadwal salat (Map<String, String>) ke SharedPreferences.
     * Berguna untuk menjadwalkan ulang alarm saat perangkat reboot.
     */
    fun saveJadwalSalat(jadwal: Map<String, String>) {
        val editor = preferences.edit()
        jadwal.forEach { (namaSalat, waktuSalat) ->
            // Simpan setiap waktu salat dengan key unik berdasarkan nama salat
            editor.putString("$KEY_JADWAL_PREFIX$namaSalat", waktuSalat)
        }
        editor.apply()
    }

    /**
     * Mendapatkan jadwal salat yang tersimpan dari SharedPreferences.
     * @return Map jadwal salat. Kosong jika tidak ada yang tersimpan.
     */
    fun getSavedJadwalSalat(): Map<String, String> {
        val jadwal = mutableMapOf<String, String>()
        // Daftar nama salat yang konsisten dengan yang digunakan di AlarmUtils
        // Pastikan nama-nama ini sama persis dengan yang ada di `salatSchedule` di `AlarmUtils`
        val daftarSalat = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        daftarSalat.forEach { namaSalat ->
            preferences.getString("$KEY_JADWAL_PREFIX$namaSalat", null)?.let { waktuTersimpan ->
                jadwal[namaSalat] = waktuTersimpan
            }
        }
        return jadwal
    }
}