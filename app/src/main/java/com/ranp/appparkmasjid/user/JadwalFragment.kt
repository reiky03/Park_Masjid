package com.ranp.appparkmasjid.user

import android.Manifest // Untuk permission notifikasi (Android 13+)
import android.annotation.SuppressLint
import android.app.AlertDialog // Untuk dialog penjelasan
import android.content.Intent // Untuk membuka pengaturan aplikasi
import android.content.pm.PackageManager // Untuk memeriksa status permission
import android.net.Uri // Untuk URI pengaturan aplikasi
import android.os.Build // Untuk memeriksa versi Android
import android.os.Bundle
import android.provider.Settings // Untuk intent ke pengaturan aplikasi
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts // Untuk meminta permission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.database.*
import com.ranp.appparkmasjid.R // Impor R dari package utama aplikasi
import com.ranp.appparkmasjid.user.util.AlarmUtils
import com.ranp.appparkmasjid.user.util.PreferenceHelper

class JadwalFragment : Fragment() {

    // --- Deklarasi View untuk Menampilkan Waktu Shalat ---
    private lateinit var shubuh: TextView
    private lateinit var dzuhur: TextView
    private lateinit var ashar: TextView
    private lateinit var maghrib: TextView
    private lateinit var isya: TextView

    // Checkbox untuk mengaktifkan notifikasi adzan
    private lateinit var checkboxNotifikasiAzan: MaterialCheckBox

    // --- Referensi Firebase ---
    private val database = FirebaseDatabase.getInstance()
    // Referensi ke Firebase Database untuk path salat
    private val salatRef = database.getReference("salat")

    // --- SharedPreferences Helper untuk Menyimpan Status Checkbox ---
    private lateinit var preferenceHelper: PreferenceHelper

    // --- Penyimpanan Jadwal Salat Terkini untuk Alarm ---
    private var currentJadwalSalat: Map<String, String> = emptyMap()

    // --- ActivityResultLauncher untuk Permintaan Izin Notifikasi (Android 13+) ---
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            // Callback ini akan dipanggil setelah pengguna memberikan izin ke aplikasi
            if (isGranted) {
                Log.i("JadwalFragment", "Izin notifikasi POST_NOTIFICATIONS DIBERIKAN oleh pengguna.")
                // Update centang checkbox untuk notifikasi jika izin diberikan
                checkboxNotifikasiAzan.isChecked = true
                // Simpan ke SharedPreferences
                preferenceHelper.setAzanNotificationEnabled(true)
                // Jika jadwal salat sudah ada, set ulang alarm
                if (currentJadwalSalat.isNotEmpty()) {
                    AlarmUtils.setAlarmForAllSalat(requireContext(), currentJadwalSalat)
                    Toast.makeText(requireContext(), "Notifikasi Adzan Aktif", Toast.LENGTH_SHORT).show()
                } else {
                    // Jika jadwal belum ada, load dulu, alarm akan diset di onDataChange
                    loadJadwalSalat(true)
                }
            } else {
                Log.w("JadwalFragment", "Izin notifikasi POST_NOTIFICATIONS DITOLAK oleh pengguna.")
                Toast.makeText(requireContext(), "Izin notifikasi ditolak. Notifikasi adzan tidak akan muncul.", Toast.LENGTH_LONG).show()
                // Pastikan checkbox tidak tercentang dan simpan statusnya
                checkboxNotifikasiAzan.isChecked = false
                // Simpan ke SharedPreferences
                preferenceHelper.setAzanNotificationEnabled(false)
                // Batalkan semua alarm
                AlarmUtils.cancelAllAlarms(requireContext())
            }
        }

    // --- Fungsi untuk Menampilkan Layout Fragment Jadwal ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_jadwal, container, false) // Ganti dengan layout fragment jadwalmu

        // --- Inisialisasi View untuk Menampilkan Waktu Shalat ---
        shubuh = view.findViewById(R.id.shubuh)
        dzuhur = view.findViewById(R.id.dzuhur)
        ashar = view.findViewById(R.id.ashar)
        maghrib = view.findViewById(R.id.maghrib)
        isya = view.findViewById(R.id.isya)

        // --- Checkbox untuk mengaktifkan notifikasi adzan ---
        checkboxNotifikasiAzan = view.findViewById(R.id.checkboxNotifikasiAzan)

        // --- Inisialisasi SharedPreferences Helper ---
        preferenceHelper = PreferenceHelper(requireContext())

        // --- Atur Status Awal Checkbox Berdasarkan SharedPreferences ---
        // dan periksa juga izin aktual jika di Android 13+
        val initialCheckboxState = preferenceHelper.isAzanNotificationEnabled() && hasNotificationPermission()
        checkboxNotifikasiAzan.isChecked = initialCheckboxState
        Log.d("JadwalFragment", "Status awal checkbox dari SharedPreferences & izin: $initialCheckboxState")

        // --- Listener untuk Perubahan Status Checkbox ---
        checkboxNotifikasiAzan.setOnCheckedChangeListener { _, isChecked ->
            Log.d("JadwalFragment", "Checkbox diubah menjadi: $isChecked")
            if (isChecked) {
                // --- ALUR KETIKA CHECKBOX DICENTANG (User ingin mengaktifkan notifikasi) ---
                handleNotificationActivation()
            } else {
                // --- ALUR KETIKA CHECKBOX TIDAK DICENTANG (User ingin mematikan notifikasi) ---
                Log.i("JadwalFragment", "Checkbox tidak dicentang. Mematikan notifikasi adzan.")
                // Simpan status ke SharedPreferences
                preferenceHelper.setAzanNotificationEnabled(false)
                // Batalkan semua alarm
                AlarmUtils.cancelAllAlarms(requireContext())
                // Tampilkan Toast
                Toast.makeText(requireContext(), "Notifikasi Adzan Dimatikan", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Muat Jadwal Salat dari Firebase ---
        // Saat fragment pertama kali dimuat, set alarm jika checkbox sudah tercentang DAN izin ada
        loadJadwalSalat(initialCheckboxState)

        return view
    }

    // --- Fungsi untuk menangani aktivasi notifikasi saat checkbox dicentang ---
    private fun handleNotificationActivation() {
        Log.d("JadwalFragment", "handleNotificationActivation dipanggil.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Cek untuk Android 13 (API 33) ke atas
            when {
                // 1. Cek apakah izin notifikasi SUDAH DIBERIKAN
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("JadwalFragment", "Izin notifikasi SUDAH ADA (Android 13+). Mengaktifkan notifikasi.")
                    activateNotificationsAndSetAlarms()
                }
                // 2. Cek apakah kita PERLU MENAMPILKAN PENJELASAN (rationale)
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.i("JadwalFragment", "Perlu menampilkan RATIONALE untuk izin notifikasi (Android 13+).")
                    showNotificationPermissionRationaleDialog()
                }
                // 3. Jika izin belum ada dan rationale tidak perlu ditampilkan
                else -> {
                    Log.i("JadwalFragment", "Meminta izin POST_NOTIFICATIONS untuk pertama kali atau setelah 'Jangan tanya lagi'.")
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Status checkbox akan dihandle di callback requestNotificationPermissionLauncher
                }
            }
        } else {
            // Untuk Android DI BAWAH 13 (API < 33)
            Log.i("JadwalFragment", "Android di bawah 13. Mengaktifkan notifikasi tanpa meminta izin runtime POST_NOTIFICATIONS.")
            activateNotificationsAndSetAlarms()
        }
    }

    // --- Fungsi untuk mengaktifkan notifikasi dan menyetel alarm ---
    private fun activateNotificationsAndSetAlarms() {
        // Simpan status ke SharedPreferences dan aktifkan notifikasi
        preferenceHelper.setAzanNotificationEnabled(true)
        // Jika jadwal salat sudah ada, set ulang alarm
        if (currentJadwalSalat.isNotEmpty()) {
            // Kirim semua jadwal ke fungsi alarm
            AlarmUtils.setAlarmForAllSalat(requireContext(), currentJadwalSalat)
        } else {
            // Jika jadwal belum ada, load dulu, alarm akan diset di onDataChange
            loadJadwalSalat(true)
        }
        Toast.makeText(requireContext(), "Notifikasi Adzan Aktif", Toast.LENGTH_SHORT).show()
    }

    // --- Fungsi untuk menampilkan dialog penjelasan (rationale) untuk mengaktifkan izin notifikasi ---
    private fun showNotificationPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Izin Notifikasi Dibutuhkan")
            .setMessage("Aplikasi ini membutuhkan izin notifikasi untuk dapat menampilkan pengingat waktu adzan. Izinkan?")
            .setPositiveButton("Izinkan") { _, _ ->
                Log.d("JadwalFragment", "Pengguna SETUJU dari dialog rationale. Meminta izin POST_NOTIFICATIONS.")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Jangan Sekarang") { dialog, _ ->
                Log.d("JadwalFragment", "Pengguna MENOLAK dari dialog rationale.")
                checkboxNotifikasiAzan.isChecked = false // Kembalikan status checkbox
                preferenceHelper.setAzanNotificationEnabled(false) // Simpan status
                dialog.dismiss()
                Toast.makeText(requireContext(), "Notifikasi adzan tidak diaktifkan.", Toast.LENGTH_SHORT).show()
            }
            .setOnCancelListener {
                Log.d("JadwalFragment", "Dialog rationale DI-CANCEL.")
                checkboxNotifikasiAzan.isChecked = false // Kembalikan status checkbox
                preferenceHelper.setAzanNotificationEnabled(false) // Simpan status
                Toast.makeText(requireContext(), "Notifikasi adzan tidak diaktifkan.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // --- Fungsi untuk memuat data jadwal salat dari Firebase ---
    private fun loadJadwalSalat(setAlarmIfEnabledAndPermitted: Boolean) {
        Log.d("JadwalFragment", "loadJadwalSalat dipanggil. setAlarmIfEnabledAndPermitted: $setAlarmIfEnabledAndPermitted")
        salatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Jika fragment tidak aktif atau context null, batalkan.
                if (!isAdded || context == null) {
                    Log.w("JadwalFragment", "onDataChange: Fragment tidak terpasang atau context null. Batal update UI.")
                    return
                }
                // Ambil waktu salat dari Firebase
                val subuhWaktu = snapshot.child("subuh").child("waktu").getValue(String::class.java) ?: "-"
                val dzuhurWaktu = snapshot.child("dzuhur").child("waktu").getValue(String::class.java) ?: "-"
                val asharWaktu = snapshot.child("ashar").child("waktu").getValue(String::class.java) ?: "-"
                val maghribWaktu = snapshot.child("maghrib").child("waktu").getValue(String::class.java) ?: "-"
                val isyaWaktu = snapshot.child("isya").child("waktu").getValue(String::class.java) ?: "-"

                // Tampilkan waktu salat ke layar
                this@JadwalFragment.shubuh.text = subuhWaktu
                this@JadwalFragment.dzuhur.text = dzuhurWaktu
                this@JadwalFragment.ashar.text = asharWaktu
                this@JadwalFragment.maghrib.text = maghribWaktu
                this@JadwalFragment.isya.text = isyaWaktu
                Log.d("JadwalFragment", "Jadwal salat berhasil dimuat dan ditampilkan.")

                // Siapkan jadwal salat untuk disimpan ke SharedPreferences
                currentJadwalSalat = mapOf(
                    "Subuh" to subuhWaktu,
                    "Dzuhur" to dzuhurWaktu,
                    "Ashar" to asharWaktu,
                    "Maghrib" to maghribWaktu,
                    "Isya" to isyaWaktu
                )
                // Simpan jadwal ke SharedPreferences
                preferenceHelper.saveJadwalSalat(currentJadwalSalat)
                Log.d("JadwalFragment", "Jadwal salat disimpan ke SharedPreferences.")

                // Setel alarm jika diminta, notifikasi diaktifkan di prefs, DAN izin ada.
                if (setAlarmIfEnabledAndPermitted && preferenceHelper.isAzanNotificationEnabled() && hasNotificationPermission()) {
                    if (currentJadwalSalat.any { it.value == "-" }) {
                        Log.w("JadwalFragment", "Beberapa waktu salat belum ada ('-'). Alarm mungkin tidak disetel sepenuhnya.")
                        Toast.makeText(requireContext(), "Beberapa waktu salat belum tersedia, alarm mungkin tidak lengkap.", Toast.LENGTH_LONG).show()
                        val validJadwal = currentJadwalSalat.filter { it.value != "-" }
                        if (validJadwal.isNotEmpty()) {
                            AlarmUtils.setAlarmForAllSalat(requireContext(), validJadwal)
                            Log.i("JadwalFragment", "Alarm disetel untuk waktu salat yang valid setelah data diterima.")
                        }
                    } else {
                        AlarmUtils.setAlarmForAllSalat(requireContext(), currentJadwalSalat)
                        Log.i("JadwalFragment", "Semua jadwal salat valid. Alarm disetel setelah data diterima.")
                    }
                } else {
                    Log.d("JadwalFragment", "Alarm tidak disetel setelah data diterima. Kondisi: setAlarm=$setAlarmIfEnabledAndPermitted, isAzanEnabled=${preferenceHelper.isAzanNotificationEnabled()}, hasPermission=${hasNotificationPermission()}")
                }
            }
            // Jika ada kesalahan saat memuat data, tampilkan Toast
            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || context == null) {
                    Log.w("JadwalFragment", "onCancelled: Fragment tidak terpasang atau context null.")
                    return
                }
                Toast.makeText(requireContext(), "Gagal memuat data jadwal: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("JadwalFragment", "Firebase onCancelled: ${error.message}")
            }
        })
    }

    // --- Fungsi helper untuk memeriksa apakah izin notifikasi (POST_NOTIFICATIONS) sudah ada ---
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Di bawah Android 13, izin ini tidak diminta secara runtime, dianggap ada jika di manifest.
        }
    }

    // --- (Opsional) Fungsi untuk membuka pengaturan aplikasi jika izin diblokir permanen ---
    @Suppress("unused")
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        try {
            startActivity(intent)
            Toast.makeText(requireContext(), "Silakan aktifkan izin notifikasi di pengaturan aplikasi.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("JadwalFragment", "Gagal membuka pengaturan aplikasi", e)
            Toast.makeText(requireContext(), "Tidak dapat membuka pengaturan aplikasi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Saat fragment kembali aktif (misalnya setelah dari pengaturan aplikasi),
        // perbarui status checkbox dan setel alarm jika diperlukan.
        // Ini penting jika pengguna mengubah izin dari pengaturan sistem.
        Log.d("JadwalFragment", "onResume dipanggil.")
        val isNotificationEnabledFromPrefs = preferenceHelper.isAzanNotificationEnabled()
        val currentNotificationPermissionStatus = hasNotificationPermission()

        // Update status checkbox berdasarkan preferensi dan izin aktual
        val shouldBeChecked = isNotificationEnabledFromPrefs && currentNotificationPermissionStatus
        if (checkboxNotifikasiAzan.isChecked != shouldBeChecked) {
            checkboxNotifikasiAzan.isChecked = shouldBeChecked
        }

        // Kode untuk mengatur alarm jika diperlukan
        if (isNotificationEnabledFromPrefs && !currentNotificationPermissionStatus) {
            // Jika preferensi mengatakan aktif, tapi izin sekarang tidak ada (misalnya dicabut dari pengaturan)
            Log.w("JadwalFragment", "onResume: Notifikasi diaktifkan di Prefs, tapi izin POST_NOTIFICATIONS sekarang tidak ada. Nonaktifkan.")
            preferenceHelper.setAzanNotificationEnabled(false) // Sinkronkan SharedPreferences
            AlarmUtils.cancelAllAlarms(requireContext()) // Pastikan alarm dibatalkan
            Toast.makeText(requireContext(), "Izin notifikasi dicabut. Notifikasi Adzan dimatikan.", Toast.LENGTH_LONG).show()
        } else if (isNotificationEnabledFromPrefs && currentNotificationPermissionStatus && currentJadwalSalat.isNotEmpty()) {
            // Jika preferensi aktif, izin ada, dan jadwal sudah dimuat, pastikan alarm disetel.
            // Ini berguna jika alarm mungkin terhapus atau belum disetel karena fragment tidak aktif.
            Log.d("JadwalFragment", "onResume: Memastikan alarm disetel sesuai preferensi dan izin.")
            AlarmUtils.setAlarmForAllSalat(requireContext(), currentJadwalSalat.filter { it.value != "-" })
        }
    }
}
