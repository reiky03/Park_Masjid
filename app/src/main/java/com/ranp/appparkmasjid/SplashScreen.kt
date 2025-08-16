package com.ranp.appparkmasjid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ranp.appparkmasjid.admin.AdminLoginActivity
import com.ranp.appparkmasjid.user.MainActivity

class SplashScreen : AppCompatActivity() {

    // Waktu splash screen ditampilkan (3 detik = 3000ms)
    private val waktu: Long = 3000
    // Jumlah ketukan pada logo
    private var tapCount = 0
    // Waktu terakhir logo ditap
    private var lastTapTime = 0L
    // Batas waktu antar tap maksimal (1000ms = 1 detik)
    private val maxIntervalBetweenTaps = 1000
    // Penanda agar tidak masuk ke 2 activity sekaligus
    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Memastikan aplikasi berjalan dalam mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Ambil referensi dari ImageView logo
        val logo = findViewById<ImageView>(R.id.logo_aplikasi)

        // Saat logo diklik
        logo.setOnClickListener {
            val currentTime = System.currentTimeMillis() // ambil waktu saat ini

            // Cek apakah tap ini masih dalam rentang waktu yang diizinkan
            if (currentTime - lastTapTime <= maxIntervalBetweenTaps) {
                tapCount++ // kalau iya, tambahkan hitungan tap
            } else {
                tapCount = 1 // kalau tidak, reset hitungan tap
            }

            // Update waktu tap terakhir
            lastTapTime = currentTime

            // Kalau sudah tap 5x dan belum berpindah activity
            if (tapCount == 3 && !isNavigated) {
                isNavigated = true // tandai bahwa kita sudah berpindah
                Toast.makeText(this, "Mode Admin Diaktifkan", Toast.LENGTH_SHORT).show()

                // Pindah ke halaman login admin
                startActivity(Intent(this, AdminLoginActivity::class.java))
                finish() // tutup splash screen
            }
        }

        // Setelah 3 detik, jika belum masuk ke admin, maka masuk ke user
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isNavigated) {
                startActivity(Intent(this, MainActivity::class.java)) // masuk ke sisi user
                finish() // tutup splash screen
            }
        }, waktu)
    }
}
