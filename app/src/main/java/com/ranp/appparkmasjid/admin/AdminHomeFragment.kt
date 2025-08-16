package com.ranp.appparkmasjid.admin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.ranp.appparkmasjid.R

class AdminHomeFragment : Fragment() {

    // Deklarasi elemen tampilan motor
    private lateinit var terisiMotorAdmin: TextView
    private lateinit var kapasitasMotorAdmin: TextView
    private lateinit var textNotifikasiMotorAdmin: TextView
    private lateinit var progressBarMotorAdmin: ProgressBar

    // Deklarasi elemen tampilan mobil
    private lateinit var terisiMobilAdmin: TextView
    private lateinit var kapasitasMobilAdmin: TextView
    private lateinit var textNotifikasiMobilAdmin: TextView
    private lateinit var progressBarMobilAdmin: ProgressBar

    // Deklarasi komponen pemicu dialog
    private lateinit var textEditParkir: TextView

    // Referensi Firebase Realtime Database
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_home, container, false)

        // Inisialisasi komponen tampilan motor
        terisiMotorAdmin = view.findViewById(R.id.textTerisiMotorAdmin)
        kapasitasMotorAdmin = view.findViewById(R.id.textTotalMotorAdmin)
        textNotifikasiMotorAdmin = view.findViewById(R.id.textNotifikasiMotorAdmin)
        progressBarMotorAdmin = view.findViewById(R.id.progressBarMotorAdmin)

        // Inisialisasi komponen tampilan mobil
        terisiMobilAdmin = view.findViewById(R.id.textTerisiMobilAdmin)
        kapasitasMobilAdmin = view.findViewById(R.id.textTotalMobilAdmin)
        textNotifikasiMobilAdmin = view.findViewById(R.id.textNotifikasiMobilAdmin)
        progressBarMobilAdmin = view.findViewById(R.id.progressBarMobilAdmin)

        // Inisialisasi komponen pemicu dialog
        textEditParkir = view.findViewById(R.id.textEditParkir)

        // Inisialisasi koneksi ke Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Memuat data parkir dari Firebase
        loadParkirData()

        // Tampilkan dialog saat tombol edit diklik
        textEditParkir.setOnClickListener {
            showEditParkirDialog()
        }
    }

    // Fungsi untuk mengambil data dari Firebase dan menampilkan ke UI
    private fun loadParkirData() {
        database.child("parkir").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Data motor
                val motor = snapshot.child("motor")
                val motorKapasitas = motor.child("kapasitas").getValue(Int::class.java) ?: 0
                val motorTerisi = motor.child("terisi").getValue(Int::class.java) ?: 0

                // Data mobil
                val mobil = snapshot.child("mobil")
                val mobilKapasitas = mobil.child("kapasitas").getValue(Int::class.java) ?: 0
                val mobilTerisi = mobil.child("terisi").getValue(Int::class.java) ?: 0

                // Tampilkan data ke UI berupa notifikasi untuk motor
                kapasitasMotorAdmin.text = motorKapasitas.toString()
                terisiMotorAdmin.text = motorTerisi.toString()
                if (motorTerisi >= motorKapasitas) {
                    textNotifikasiMotorAdmin.text = "Maaf, Ruang Parkir Telah Penuh"
                    // Mengubah warna teks notifikasi menjadi merah
                    textNotifikasiMotorAdmin.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                } else {
                    textNotifikasiMotorAdmin.text = "Ruang Parkir Tersedia"
                    // Mengubah warna teks notifikasi menjadi black
                    textNotifikasiMotorAdmin.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }

                // Tampilkan data ke UI berupa notifikasi untuk mobil
                kapasitasMobilAdmin.text = mobilKapasitas.toString()
                terisiMobilAdmin.text = mobilTerisi.toString()
                if (mobilTerisi >= mobilKapasitas) {
                    textNotifikasiMobilAdmin.text = "Maaf, Ruang Parkir Telah Penuh"
                    // Mengubah warna teks notifikasi menjadi merah
                    textNotifikasiMobilAdmin.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                } else {
                    textNotifikasiMobilAdmin.text = "Ruang Parkir Tersedia"
                    // Mengubah warna teks notifikasi menjadi black
                    textNotifikasiMobilAdmin.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }

                // Menghitung persentase kapasitas parkir motor dan mobil dengan progress bar
                val persentaseMotor = (motorTerisi.toFloat() / motorKapasitas.toFloat()) * 100
                val persentaseMobil = (mobilTerisi.toFloat() / mobilKapasitas.toFloat()) * 100
                progressBarMotorAdmin.progress = persentaseMotor.toInt()
                progressBarMobilAdmin.progress = persentaseMobil.toInt()
                // Mengubah warna progress bar berdasarkan persentase kapasitas
                setProgressBarColor(progressBarMotorAdmin, persentaseMotor.toInt())
                setProgressBarColor(progressBarMobilAdmin, persentaseMobil.toInt())
            }

            // Jika ada kesalahan saat mengambil data, tampilkan Toast
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fungsi untuk mengubah warna progress bar berdasarkan persentase kapasitas
    private fun setProgressBarColor(progressBar: ProgressBar, toInt: Int) {
        val warnaAdmin = when {
            toInt >= 80 -> ContextCompat.getColor(requireContext(), R.color.redLight)
            toInt >= 50 -> ContextCompat.getColor(requireContext(), R.color.yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.green)
        }
        val drawableAdmin = progressBar.progressDrawable.mutate()
        drawableAdmin.setColorFilter(warnaAdmin, android.graphics.PorterDuff.Mode.SRC_IN)
        progressBar.progressDrawable = drawableAdmin
    }

    // Menampilkan dialog untuk edit kapasitas parkir
    private fun showEditParkirDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Mencegah dialog ditutup dengan klik tombol kembali
        dialog.setCancelable(false)
        // Mencegah dialog ditutup dengan klik di luar dialog
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.dialog_edit_parkir) // Menggunakan layout XML dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // --- Inisialisasi Komponen dari dialog_edit_parkir.xml yang memiliki ID ---
        val editTextKapasitasMotor: EditText = dialog.findViewById(R.id.kolom_parkir_motor)
        val editTextKapasitasMobil: EditText = dialog.findViewById(R.id.kolom_parkir_mobil)
        val buttonBatal: Button = dialog.findViewById(R.id.btnCancel) // Teks: "Tidak"
        val buttonSimpan: Button = dialog.findViewById(R.id.btnSubmit)  // Teks: "Iya"

        // Ambil data saat ini dan isi EditText-nya agar tidak kosong
        database.child("parkir").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ambil data kapasitas dari Firebase
                val motorKapasitas = snapshot.child("motor/kapasitas").getValue(Int::class.java) ?: 0
                val mobilKapasitas = snapshot.child("mobil/kapasitas").getValue(Int::class.java) ?: 0

                // Tampilkan data ke EditText
                editTextKapasitasMotor.setText(motorKapasitas.toString())
                editTextKapasitasMobil.setText(mobilKapasitas.toString())
            }
            // Jika ada kesalahan saat mengambil data, tampilkan Toast
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data kapasitas", Toast.LENGTH_SHORT).show()
            }
        })

        // Tombol Batal
        buttonBatal.setOnClickListener {
            Toast.makeText(requireContext(), "Aksi dibatalkan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Tombol Simpan
        buttonSimpan.setOnClickListener {
            // Ambil data dari EditText dan konversi ke Int
            val kapasitasMotor = editTextKapasitasMotor.text.toString().toIntOrNull()
            val kapasitasMobil = editTextKapasitasMobil.text.toString().toIntOrNull()

            // Validasi input
            if (kapasitasMotor == null || kapasitasMobil == null) {
                Toast.makeText(requireContext(), "Isi angka yang valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simpan ke Firebase
            val updates = mapOf(
                "motor/kapasitas" to kapasitasMotor,
                "mobil/kapasitas" to kapasitasMobil
            )

            // Update data di Firebase dan tampilkan Toast
            database.child("parkir").updateChildren(updates).addOnSuccessListener {
                Toast.makeText(requireContext(), "Kapasitas diperbarui", Toast.LENGTH_SHORT).show()
                // Setelah diperbarui, tutup dialog dan refresh tampilan
                dialog.dismiss()
                loadParkirData() // Refresh tampilan
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show()
            }
        }
        // Tampilkan dialog
        dialog.show()
    }
}