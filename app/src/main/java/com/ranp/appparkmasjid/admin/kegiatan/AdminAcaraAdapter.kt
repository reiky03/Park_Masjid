package com.ranp.appparkmasjid.admin.kegiatan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ranp.appparkmasjid.R
import com.ranp.appparkmasjid.model.AcaraModel

// Adapter untuk RecyclerView di sisi Admin
// Menampilkan daftar acara dengan tombol Edit & Hapus
class AdminAcaraAdapter(
    private val items: MutableList<AcaraModel>,         // List data acara
    private val onEdit: (AcaraModel) -> Unit,           // Callback saat tombol edit ditekan
    private val onDelete: (AcaraModel) -> Unit          // Callback saat tombol hapus ditekan
) : RecyclerView.Adapter<AdminAcaraAdapter.VH>() {

    // ViewHolder: mereferensikan elemen-elemen view di item_admin_acara.xml
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvNamaAcaraAdmin)
        val tvDesk: TextView = itemView.findViewById(R.id.tvDeskripsiAcaraAdmin)
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktuAcaraAdmin)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnHapus: ImageButton = itemView.findViewById(R.id.btnHapus)
    }

    // Membuat ViewHolder dari layout item_admin_acara.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_acara, parent, false)
        return VH(v)
    }

    // Mengisi data ke dalam view pada setiap item
    override fun onBindViewHolder(holder: VH, position: Int) {
        val data = items[position]
        // Bind judul & deskripsi
        holder.tvNama.text = data.nama.orEmpty().ifBlank { "-" }
        holder.tvDesk.text = data.deskripsi.orEmpty().ifBlank { "-" }

        // Bind waktu (tanggal • jam) -> contoh: "Selasa, 19 Agu 2025 • 17:00 WIB"
        holder.tvWaktu.text = formatWaktu(data.tanggal, data.jam)

        // Event tombol edit & hapus
        holder.btnEdit.setOnClickListener { onEdit(data) }
        holder.btnHapus.setOnClickListener { onDelete(data) }
    }

    override fun getItemCount(): Int = items.size

    // Memperbarui data list dan me-refresh tampilan
    fun setData(newItems: List<AcaraModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * Format final yang ditampilkan pada baris waktu.
     * Input: tanggal "yyyy-MM-dd" dan jam "HH:mm"
     * Output: "Selasa, 19 Agu 2025 • 17:00 WIB"
     */
    private fun formatWaktu(tanggal: String?, jam: String?): String {
        val prettyDate = prettyDate(tanggal)
        val prettyTime = jam?.takeIf { it.isNotBlank() } ?: "-"
        return "$prettyDate • $prettyTime WIB"
    }

    /**
     * Ubah "yyyy-MM-dd" jadi "Hari, dd Mon yyyy" (locale sederhana Indonesia).
     * Kalau parsing gagal, kembalikan string asal.
     */
    private fun prettyDate(yyyyMMdd: String?): String {
        val raw = yyyyMMdd ?: return "-"
        return try {
            val parts = raw.split("-")
            if (parts.size != 3) return raw

            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()

            val cal = java.util.Calendar.getInstance().apply { set(y, m - 1, d) }

            val dayNames = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Agu", "Sep", "Okt", "Nov", "Des")

            val dayName = dayNames[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            val monthName = monthNames[m - 1]

            "$dayName, $d $monthName $y"
        } catch (_: Exception) {
            raw
        }
    }
}


