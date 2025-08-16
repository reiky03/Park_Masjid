package com.ranp.appparkmasjid.user

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ranp.appparkmasjid.R

class MainActivity : AppCompatActivity() {

    // Mendeklarasikan variabel untuk BottomNavigationView
    // 'lateinit' menandakan bahwa variabel ini akan diinisialisasi nanti sebelum digunakan
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Agar aplikasi berjalan dalam mode terang walau hp dalam mode gelap
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Mengambil referensi ke layout utama
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        // Mengambil referensi ke BottomNavigationView dari layout menggunakan ID-nya
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Mengatur padding layout dan BottomNavigationView agar tidak ketimpa sistem navigasi
        // dan tidak membuat navbar menjadi tinggi karena padding pada tombol navigasi
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Set padding untuk layout utama (main layout)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            // Tambahkan padding bawah ke BottomNavigation agar tidak tertutup tombol navigasi
            bottomNavigationView.setPadding(
                0, 0, 0,
                systemBars.bottom.coerceAtLeast(8)
            )
            insets
        }

        // Menetapkan fragment default (HomeFragment) saat Activity pertama kali dibuat
        // savedInstanceState == null menandakan bahwa Activity belum pernah dibuat sebelumnya
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Mengatur listener untuk mendengarkan saat item di BottomNavigationView dipilih
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            // Memeriksa ID dari item menu yang dipilih
            when (menuItem.itemId) {
                // Jika item yang dipilih adalah Home
                R.id.home -> {
                    // Ganti fragment yang ditampilkan dengan HomeFragment
                    replaceFragment(HomeFragment())
                    true // Mengembalikan true menandakan bahwa event telah ditangani
                }
                // Jika item yang dipilih adalah Jadwal
                R.id.jadwal -> {
                    // Ganti fragment yang ditampilkan dengan JadwalFragment
                    replaceFragment(JadwalFragment())
                    true // Mengembalikan true menandakan bahwa event telah ditangani
                }
                // Jika item lain yang tidak dikenali dipilih
                else -> false // Mengembalikan false menandakan bahwa event tidak ditangani
            }
        }
    }

    // Fungsi helper untuk mengganti fragment yang ditampilkan di FrameLayout
    private fun replaceFragment(fragment: Fragment) {
        // Memulai transaction FragmentManager untuk mengelola fragment
        supportFragmentManager.beginTransaction().apply {
            // Mengganti fragment yang ada di container dengan ID fragment_container
            // dengan fragment yang baru
            replace(R.id.fragment_container, fragment)
            // Menerapkan perubahan (menjalankan transaction)
            commit()
        }
    }
}
