package com.ranp.appparkmasjid.admin.kegiatan

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.ranp.appparkmasjid.R
import com.ranp.appparkmasjid.model.AcaraModel
import java.util.Calendar

// Fragment untuk sisi Admin mengelola (CRUD) acara
class AdminAcaraFragment : Fragment() {

    // Komponen UI
    private lateinit var rv: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: AdminAcaraAdapter
    private val list = mutableListOf<AcaraModel>() // List untuk menampung data acara

    // Referensi Firebase Realtime Database
    private val dbRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("acara")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_admin_acara, container, false)

        // Inisialisasi view
        rv = v.findViewById(R.id.rvAcaraAdmin)
        fab = v.findViewById(R.id.fabAddAcara)

        // Set adapter untuk RecyclerView
        adapter = AdminAcaraAdapter(list,
            onEdit = { showAddEditDialog(it) },    // Saat edit ditekan
            onDelete = { confirmDelete(it) }       // Saat hapus ditekan
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Event klik tombol tambah
        fab.setOnClickListener { showAddEditDialog(null) }

        // Ambil data dari Firebase
        observeData()
        seedAcaraKosongJikaBelumAda()
        return v
    }

    // Mengamati perubahan data di Firebase dan mengisi RecyclerView
    private fun observeData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = mutableListOf<AcaraModel>()
                for (child in snapshot.children) {
                    val data = child.getValue(AcaraModel::class.java)
                    data?.id = child.key
                    if (data != null) temp.add(data)
                }
                // Urutkan berdasarkan tanggal & jam (terbaru di atas)
                val sorted = temp.sortedWith(
                    compareByDescending<AcaraModel> { it.tanggal ?: "" }
                        .thenByDescending { it.jam ?: "" }
                )
                adapter.setData(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Menampilkan dialog form tambah/edit acara
    private fun showAddEditDialog(existing: AcaraModel?) {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_admin_acara, null)

        // Ambil view dari layout
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etNama = view.findViewById<EditText>(R.id.etNama)
        val etDesk = view.findViewById<EditText>(R.id.etDeskripsi)
        val etTanggal = view.findViewById<EditText>(R.id.etTanggal)
        val etJam = view.findViewById<EditText>(R.id.etJam)
        val btnBatal = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatal)
        val btnSimpanUpdate = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSimpanUpdate)

        // Mode: Tambah vs Edit
        val isEdit = existing != null
        tvTitle.text = if (isEdit) "Edit Acara" else "Tambah Acara"
        btnSimpanUpdate.text = if (isEdit) "Update" else "Simpan"

        // Prefill jika edit
        etNama.setText(existing?.nama.orEmpty())
        etDesk.setText(existing?.deskripsi.orEmpty())
        etTanggal.setText(existing?.tanggal.orEmpty())
        etJam.setText(existing?.jam.orEmpty())

        // DatePicker - pilih tanggal
        etTanggal.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (!etTanggal.text.isNullOrBlank()) {
                try {
                    val p = etTanggal.text.toString().split("-")
                    if (p.size == 3) cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
                } catch (_: Exception) {}
            }
            val y = cal.get(java.util.Calendar.YEAR)
            val m = cal.get(java.util.Calendar.MONTH)
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)

            val dp = android.app.DatePickerDialog(ctx, { _, year, month, dayOfMonth ->
                val mm = String.format("%02d", month + 1)
                val dd = String.format("%02d", dayOfMonth)
                etTanggal.setText("$year-$mm-$dd")
            }, y, m, d)
            // (Opsional) blok tanggal lampau
            dp.datePicker.minDate = System.currentTimeMillis() - 1000
            dp.show()
        }

        // TimePicker - pilih jam (24 jam)
        etJam.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (!etJam.text.isNullOrBlank()) {
                try {
                    val p = etJam.text.toString().split(":")
                    if (p.size == 2) {
                        cal.set(java.util.Calendar.HOUR_OF_DAY, p[0].toInt())
                        cal.set(java.util.Calendar.MINUTE, p[1].toInt())
                    }
                } catch (_: Exception) {}
            }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)

            val tp = android.app.TimePickerDialog(ctx, { _, h, m ->
                etJam.setText(String.format("%02d:%02d", h, m))
            }, hour, minute, true)
            tp.show()
        }

        // Buat dialog tanpa tombol bawaan (karena tombol ada di layout)
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setView(view)
            .create()

        dialog.setCanceledOnTouchOutside(false) // tap di luar tidak menutup
        dialog.setCancelable(false)             // tombol back tidak menutup

        // Hapus dim & bikin background transparan
        dialog.setOnShowListener {
            dialog.window?.apply {
                // transparan
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }

        // (Tambahan safety) Block tombol back manual kalau diperlukan
        dialog.setOnKeyListener { _, keyCode, _ ->
            keyCode == android.view.KeyEvent.KEYCODE_BACK // true = consume, back tidak menutup
        }

        // Aksi tombol
        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpanUpdate.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val desk = etDesk.text.toString().trim()
            val tgl = etTanggal.text.toString().trim()
            val jam = etJam.text.toString().trim()

            // Validasi wajib isi
            if (nama.isEmpty() || desk.isEmpty() || tgl.isEmpty() || jam.isEmpty()) {
                Toast.makeText(ctx, "Mohon lengkapi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Validasi format sederhana
            val validTanggal = Regex("""\d{4}-\d{2}-\d{2}""").matches(tgl)
            val validJam = Regex("""\d{2}:\d{2}""").matches(jam)
            if (!validTanggal || !validJam) {
                Toast.makeText(ctx, "Format tanggal/jam tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simpan / update ke Firebase
            if (isEdit) {
                updateAcara(existing!!.id ?: "", nama, desk, tgl, jam)
            } else {
                addAcara(nama, desk, tgl, jam)
            }
            dialog.dismiss()
        }

        dialog.show()
    }


    // Tambah acara baru ke Firebase
    private fun addAcara(nama: String, desk: String, tgl: String, jam: String) {
        val key = dbRef.push().key ?: return
        val data = AcaraModel(key, nama, desk, tgl, jam)
        dbRef.child(key).setValue(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Berhasil menambahkan acara", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    // Update data acara di Firebase
    private fun updateAcara(id: String, nama: String, desk: String, tgl: String, jam: String) {
        val updates = mapOf(
            "nama" to nama,
            "deskripsi" to desk,
            "tanggal" to tgl,
            "jam" to jam
        )
        dbRef.child(id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Acara berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun seedAcaraKosongJikaBelumAda() {
        val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
            .reference.child("acara")

        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) return@addOnSuccessListener
            // isi contoh minimal TANPA startAt/createdAt
            val sample = mapOf(
                ref.push().key!! to mapOf(
                    "nama" to "Sholawatan Akbar",
                    "deskripsi" to "Sholawatan bersama hadroh Al-Falah.",
                    "tanggal" to "2025-09-01",
                    "jam" to "19:30"
                ),
                ref.push().key!! to mapOf(
                    "nama" to "Kajian Tafsir",
                    "deskripsi" to "Kajian ba’da Maghrib.",
                    "tanggal" to "2025-09-02",
                    "jam" to "18:15"
                )
            )
            ref.updateChildren(sample)
        }
    }


    // Konfirmasi hapus acara
    private fun confirmDelete(data: AcaraModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Acara")
            .setMessage("Yakin hapus \"${data.nama}\"?")
            .setPositiveButton("Hapus") { d, _ ->
                val id = data.id ?: return@setPositiveButton
                dbRef.child(id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Acara berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    }
                d.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
