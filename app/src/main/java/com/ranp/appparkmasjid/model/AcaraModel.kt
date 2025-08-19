package com.ranp.appparkmasjid.model

// Data class untuk merepresentasikan 1 acara di Firebase
// Semua properti dibuat nullable dan punya nilai default biar aman saat parsing dari Firebase
data class AcaraModel(
    var id: String? = null,        // ID unik acara (key dari Firebase)
    var nama: String? = null,      // Nama/judul acara
    var deskripsi: String? = null, // Deskripsi singkat acara
    var tanggal: String? = null,   // Tanggal acara (format yyyy-MM-dd)
    var jam: String? = null        // Jam acara (format HH:mm)
)
