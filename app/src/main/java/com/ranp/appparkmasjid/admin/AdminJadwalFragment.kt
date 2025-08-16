package com.ranp.appparkmasjid.admin

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.ranp.appparkmasjid.R
import java.util.*

class AdminJadwalFragment : Fragment() {

    // Deklarasikan Firebase Database
    private lateinit var database: DatabaseReference
    // Deklarasikan TextView pemicu dialog
    private lateinit var textEditSalat: TextView
    // Deklarasikan TextView untuk waktu salat
    private lateinit var shubuhAdmin: TextView
    private lateinit var dzuhurAdmin: TextView
    private lateinit var asharAdmin: TextView
    private lateinit var maghribAdmin: TextView
    private lateinit var isyaAdmin: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_jadwal, container, false)

        // Inisialisasi Firebase Database dan referensi ke tabel "salat"
        database = FirebaseDatabase.getInstance().getReference("salat")

        // Inisialisasi TextView untuk menampilkan waktu salat
        shubuhAdmin = view.findViewById(R.id.shubuhAdmin)
        dzuhurAdmin = view.findViewById(R.id.dzuhurAdmin)
        asharAdmin = view.findViewById(R.id.asharAdmin)
        maghribAdmin = view.findViewById(R.id.maghribAdmin)
        isyaAdmin = view.findViewById(R.id.isyaAdmin)
        textEditSalat = view.findViewById(R.id.textEditSalat)

        // Memuat jadwal salat dari Firebase
        loadJadwalSalat()

        // Tambahkan OnClickListener ke TextView untuk membuka dialog
        textEditSalat.setOnClickListener {
            showEditSalatDialog()
        }

        // Inflate the layout for this fragment
        return view
    }

    // Fungsi untuk memuat jadwal salat dari Firebase
    private fun loadJadwalSalat() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Jika data ditemukan, tampilkan di TextView
                // Jika data tidak ditemukan, tampilkan "-"
                // snapshot.child("subuh").child("waktu") adalah mengambil data dari tabel salat yang ada di firebase
                // di mana subuh adalah nama salat, dan waktu adalah nama kolom yang ada di firebase tabel salat
                shubuhAdmin.text = snapshot.child("subuh").child("waktu").getValue(String::class.java) ?: "-"
                dzuhurAdmin.text = snapshot.child("dzuhur").child("waktu").getValue(String::class.java) ?: "-"
                asharAdmin.text = snapshot.child("ashar").child("waktu").getValue(String::class.java) ?: "-"
                maghribAdmin.text = snapshot.child("maghrib").child("waktu").getValue(String::class.java) ?: "-"
                isyaAdmin.text = snapshot.child("isya").child("waktu").getValue(String::class.java) ?: "-"
            }
            // Jika ada kesalahan saat mengambil data, tampilkan Toast
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fungsi untuk menampilkan dialog edit jadwal salat
    private fun showEditSalatDialog() {
        // Membuat dialog
        val dialog = Dialog(requireContext())
        // Mengatur tampilan dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Agar dialog tidak bisa ditutup dengan klik di luar dialog
        dialog.setCancelable(false)
        // set content view untuk dialog
        dialog.setContentView(R.layout.dialog_edit_salat)
        // backround diluar dialog menjadi transparant
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Sesuaikan dengan layout dialog yang sesuai
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // inisialisasi komponen kolom dialog untuk edit waktu salat
        val kolomShubuh: EditText = dialog.findViewById(R.id.kolom_shubuh)
        val kolomDzuhur: EditText = dialog.findViewById(R.id.kolom_dzuhur)
        val kolomAshar: EditText = dialog.findViewById(R.id.kolom_ashar)
        val kolomMaghrib: EditText = dialog.findViewById(R.id.kolom_maghrib)
        val kolomIsya: EditText = dialog.findViewById(R.id.kolom_isya)
        val btnTidak: Button = dialog.findViewById(R.id.btnTidak)
        val btnIya: Button = dialog.findViewById(R.id.btnIya)

        // isi kolom dialog dengan data awal yang sudah ada dari Firebase
        kolomShubuh.setText(shubuhAdmin.text)
        kolomDzuhur.setText(dzuhurAdmin.text)
        kolomAshar.setText(asharAdmin.text)
        kolomMaghrib.setText(maghribAdmin.text)
        kolomIsya.setText(isyaAdmin.text)

        // Fungsi untuk menampilkan TimePicker untuk masing-masing kolom
        fun showTimePicker(editText: EditText) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            // Membuat TimePickerDialog
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                // Format waktu yang dipilih menjadi "HH:MM"
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                editText.setText(formattedTime)
            }, hour, minute, true).show()
        }
        // Set onclick ke masing-masing EditText untuk tampilkan TimePicker
        kolomShubuh.setOnClickListener { showTimePicker(kolomShubuh) }
        kolomDzuhur.setOnClickListener { showTimePicker(kolomDzuhur) }
        kolomAshar.setOnClickListener { showTimePicker(kolomAshar) }
        kolomMaghrib.setOnClickListener { showTimePicker(kolomMaghrib) }
        kolomIsya.setOnClickListener { showTimePicker(kolomIsya) }

        // aksi tombol "Tidak"
        btnTidak.setOnClickListener {
            Toast.makeText(requireContext(), "Perubahan dibatalkan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // aksi tombol "Iya"
        btnIya.setOnClickListener {
            // Simpan perubahan ke Firebase
            val data = mapOf(
                // Menambahkan data ke dalam map sesuai dengan struktur Firebase
                "subuh/waktu" to kolomShubuh.text.toString(),
                "dzuhur/waktu" to kolomDzuhur.text.toString(),
                "ashar/waktu" to kolomAshar.text.toString(),
                "maghrib/waktu" to kolomMaghrib.text.toString(),
                "isya/waktu" to kolomIsya.text.toString()
            )
            // Update data di Firebase
            database.updateChildren(data).addOnCompleteListener { task ->
                // Jika berhasil atau gagal, tampilkan Toast
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Jadwal salat berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadJadwalSalat()
                } else {
                    Toast.makeText(requireContext(), "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show()
                }
                // Tutup dialog
                dialog.dismiss()
            }
        }
        // Tampilkan dialog
        dialog.show()
    }
}