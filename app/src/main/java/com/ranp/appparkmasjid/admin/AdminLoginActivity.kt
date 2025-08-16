package com.ranp.appparkmasjid.admin

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.ranp.appparkmasjid.R

class AdminLoginActivity : AppCompatActivity() {

    // Deklarasi view komponen UI
    private lateinit var kolomEmail: EditText
    private lateinit var kolomPassword: EditText
    private lateinit var btnLoginSubmit: Button

    // Firebase Auth instance
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Cek apakah sudah login sebelumnya
        val sharedPref = getSharedPreferences("adminSession", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            // Jika sudah login, langsung ke AdminMainActivity
            val intent = Intent(this, AdminMainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Tampilkan layout login
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Inisialisasi komponen UI login
        kolomEmail = findViewById(R.id.email)
        kolomPassword = findViewById(R.id.password)
        btnLoginSubmit = findViewById(R.id.submitLogin)

        // Tampilkan ikon mata untuk password
        val passwordEditText: EditText = findViewById(R.id.password)
        val eyeToggle: ImageView = findViewById(R.id.eye_toggle)
        // Variabel untuk melacak apakah password terlihat atau tidak
        var isPasswordVisible = false
        // Aksi ketika ikon mata ditekan untuk menampilkan/sembunyikan password
        eyeToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                eyeToggle.setImageResource(R.drawable.ic_eye_on)
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeToggle.setImageResource(R.drawable.ic_eye_off)
            }
            // Pindahkan kursor ke akhir teks untuk menampilkan password
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // Aksi ketika tombol login ditekan
        btnLoginSubmit.setOnClickListener {
            val email = kolomEmail.text.toString().trim()
            val password = kolomPassword.text.toString().trim()

            // Validasi input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proses login Firebase
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Simpan status login ke SharedPreferences
                        sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                        // Pindah ke AdminMainActivity
                        Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, AdminMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
