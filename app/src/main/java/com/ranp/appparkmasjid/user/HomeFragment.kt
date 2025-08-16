package com.ranp.appparkmasjid.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.ranp.appparkmasjid.R

class HomeFragment : Fragment() {

    // Komponen tampilan data parkir motor
    private lateinit var terisiMotor: TextView
    private lateinit var kapasitasMotor: TextView
    private lateinit var textNotifikasiMotor: TextView
    private lateinit var progressBarMotor: ProgressBar
    // Komponen tampilan data parkir mobil
    private lateinit var terisiMobil: TextView
    private lateinit var kapasitasMobil: TextView
    private lateinit var textNotifikasiMobil: TextView
    private lateinit var progressBarMobil: ProgressBar
    // Komponen tombol refresh
    private lateinit var refreshButton: ImageView
    // Referensi ke Firebase
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inisialisasi tampilan data motor
        terisiMotor = view.findViewById(R.id.textTerisiMotor)
        kapasitasMotor = view.findViewById(R.id.textTotalMotor)
        textNotifikasiMotor = view.findViewById(R.id.textNotifikasiMotor)
        progressBarMotor = view.findViewById(R.id.progressBarMotor)

        // Inisialisasi tampilan data mobil
        terisiMobil = view.findViewById(R.id.textTerisiMobil)
        kapasitasMobil = view.findViewById(R.id.textTotalMobil)
        textNotifikasiMobil = view.findViewById(R.id.textNotifikasiMobil)
        progressBarMobil = view.findViewById(R.id.progressBarMobil)

        // Inisialisasi tombol refresh
        refreshButton = view.findViewById(R.id.refreshDataParkir)

        // Inisialisasi referensi database
        database = FirebaseDatabase.getInstance().reference

        // Tampilkan data saat pertama kali fragment dibuka
        loadParkirData()

        // Tambahkan listener untuk refresh manual
        refreshButton.setOnClickListener {
            loadParkirData()
        }

        return view
    }

    // Fungsi untuk memuat data parkir dari Firebase
    private fun loadParkirData() {
        // Ambil data dari path "parkir" di Firebase
        database.child("parkir").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Ambil data motor
                    val motor = snapshot.child("motor")
                    val kapasitasMotorVal = motor.child("kapasitas").getValue(Int::class.java) ?: 0
                    val terisiMotorVal = motor.child("terisi").getValue(Int::class.java) ?: 0

                    // Ambil data mobil
                    val mobil = snapshot.child("mobil")
                    val kapasitasMobilVal = mobil.child("kapasitas").getValue(Int::class.java) ?: 0
                    val terisiMobilVal = mobil.child("terisi").getValue(Int::class.java) ?: 0

                    // Tampilkan data berupa notifikasi di UI
                    // Item notifikasi untuk motor
                    kapasitasMotor.text = kapasitasMotorVal.toString()
                    terisiMotor.text = terisiMotorVal.toString()
                    // Item notifikasi untuk mobil
                    kapasitasMobil.text = kapasitasMobilVal.toString()
                    terisiMobil.text = terisiMobilVal.toString()

                    // Tampilkan notifikasi berdasarkan kapasitas dan terisi
                    // Notifikasi untuk motor
                    if (terisiMotorVal >= kapasitasMotorVal) {
                        textNotifikasiMotor.text = "Maaf, Ruang Parkir Telah Penuh"
                        // Mengubah warna teks notifikasi menjadi merah
                        textNotifikasiMotor.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    } else {
                        textNotifikasiMotor.text = "Ruang Parkir Tersedia"
                        // Mengubah warna teks notifikasi menjadi black
                        textNotifikasiMotor.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    // Notifikasi untuk mobil
                    if (terisiMobilVal >= kapasitasMobilVal) {
                        textNotifikasiMobil.text = "Maaf, Ruang Parkir Telah Penuh"
                        // Mengubah warna teks notifikasi menjadi merah
                        textNotifikasiMobil.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    } else {
                        textNotifikasiMobil.text = "Ruang Parkir Tersedia"
                        // Mengubah warna teks notifikasi menjadi black
                        textNotifikasiMobil.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    // Hitung persentase
                    val persenMotor = if (kapasitasMotorVal > 0) terisiMotorVal * 100 / kapasitasMotorVal else 0
                    val persenMobil = if (kapasitasMobilVal > 0) terisiMobilVal * 100 / kapasitasMobilVal else 0

                    // Tampilkan progress
                    progressBarMotor.progress = persenMotor
                    progressBarMobil.progress = persenMobil

                    // Ubah warna progress
                    setProgressBarColor(progressBarMotor, persenMotor)
                    setProgressBarColor(progressBarMobil, persenMobil)

                    // Tampilkan toast berhasil
                    Toast.makeText(requireContext(), "Data parkir berhasil dimuat", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Kesalahan dalam memproses data", Toast.LENGTH_SHORT).show()
                }
            }
            // Jika gagal mengambil data dari Firebase
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data parkir: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fungsi ubah warna progress bar
    private fun setProgressBarColor(progressBar: ProgressBar, persen: Int) {
        val warna = when {
            persen >= 80 -> ContextCompat.getColor(requireContext(), R.color.redLight)
            persen >= 50 -> ContextCompat.getColor(requireContext(), R.color.yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.green)
        }
        val drawable = progressBar.progressDrawable.mutate()
        drawable.setColorFilter(warna, android.graphics.PorterDuff.Mode.SRC_IN)
        progressBar.progressDrawable = drawable
    }
}

