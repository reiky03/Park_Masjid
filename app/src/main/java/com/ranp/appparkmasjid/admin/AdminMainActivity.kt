// AdminMainActivity.kt dengan komentar dan penjelasan lengkap + login check via SharedPreferences
package com.ranp.appparkmasjid.admin

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.ranp.appparkmasjid.R
import com.ranp.appparkmasjid.SplashScreen

class AdminMainActivity : AppCompatActivity() {

    // BottomNavigation untuk navigasi antar fragment
    private lateinit var bottomNavigationViewAdmin: BottomNavigationView

    // Ikon di toolbar yang akan digunakan sebagai tombol logout
    private lateinit var logoutIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Memastikan aplikasi berjalan dalam mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_main)

        // Mengatur padding agar konten tidak tertutup oleh sistem UI (notch, dll)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Mengecek apakah admin sudah login via SharedPreferences
        val sharedPref = getSharedPreferences("adminSession", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) {
            // Jika belum login, kembalikan ke halaman login
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
            return
        }

        // Menghubungkan ikon toolbar sebagai tombol logout
        logoutIcon = findViewById(R.id.toolbar_icon)
        // Ketika ikon ditekan, munculkan dialog logout
        logoutIcon.setOnClickListener {
            showLogoutDialog()
        }

        // Inisialisasi BottomNavigationView
        bottomNavigationViewAdmin = findViewById(R.id.bottom_navigation_admin)

        // Menetapkan fragment default saat pertama kali activity dibuka
        if (savedInstanceState == null) {
            replaceFragment(AdminHomeFragment())
        }

        // Logika pergantian fragment saat menu di bottom nav dipilih
        bottomNavigationViewAdmin.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_admin -> {
                    replaceFragment(AdminHomeFragment()) // Tampilkan halaman home
                    true
                }
                R.id.jadwal_admin -> {
                    replaceFragment(AdminJadwalFragment()) // Tampilkan halaman jadwal
                    true
                }
                else -> false // Menu tidak dikenali
            }
        }
    }

    // Fungsi bantu untuk mengganti fragment di dalam FrameLayout
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_admin, fragment)
            commit()
        }
    }

    // Tampilkan dialog konfirmasi logout
    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        // Sesuaikan dengan layout dialog yang sesuai
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)
        val btnSubmit: Button = dialog.findViewById(R.id.btnSubmit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        btnSubmit.setOnClickListener {
            logoutAdmin()
            dialog.dismiss()
        }
        dialog.show()
    }


    // Proses logout admin: hapus sesi dan kembali ke splash screen
    private fun logoutAdmin() {
        val sharedPref = getSharedPreferences("adminSession", MODE_PRIVATE)
        sharedPref.edit().clear().apply() // Hapus status login

        FirebaseAuth.getInstance().signOut() // Logout dari Firebase

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        // Kembali ke splash screen dan tutup semua activity sebelumnya
        val intent = Intent(this, SplashScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
