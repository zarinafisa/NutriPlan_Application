package com.example.nutriplan.ui.profil

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import com.example.nutriplan.domain.Batas
import com.example.nutriplan.domain.BeratIdeal
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.local.Aktivitas
import com.example.nutriplan.data.local.JenisKelamin
import com.example.nutriplan.data.local.Tujuan
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.local.entity.ProfileEntity
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.data.repository.ProfileRepository
import com.example.nutriplan.domain.CalorieCalculator
import com.example.nutriplan.domain.Validator
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.DropdownPickerField
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.NutriTextField
import com.example.nutriplan.ui.components.PickerField
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------------- State ----------------

data class ProfileFormState(
    val loading: Boolean = true,
    val edit: Boolean = false,
    val nama: String = "",
    val email: String = "",
    val jenisKelamin: JenisKelamin? = null,
    val tanggalLahir: LocalDate? = null,
    val tinggi: String = "",
    val berat: String = "",
    val targetBerat: String = "",
    val aktivitas: Aktivitas? = null,
    val dikunjungi: Set<String> = emptySet(),
    val saving: Boolean = false,
    val errEmailServer: String? = null
) {
    /** Error hanya tampil untuk kolom yang sudah disentuh. */
    fun error(kunci: String): String? {
        val targetTampil = kunci == "target" && targetBerat.isNotBlank() && tinggi.isNotBlank()
        if (kunci !in dikunjungi && !targetTampil) return null
        return when (kunci) {
            "nama" -> Validator.nama(nama)
            "email" -> errEmailServer ?: Validator.email(email)
            "jk" -> if (jenisKelamin == null) "Pilih jenis kelamin." else null
            "tgl" -> Validator.tanggalLahir(tanggalLahir)
            "tinggi" -> Validator.tinggi(tinggi)
            "berat" -> Validator.berat(berat)
            "target" -> Validator.berat(targetBerat) ?: BeratIdeal.cekTarget(targetBerat, tinggi)
            "aktivitas" -> if (aktivitas == null) "Pilih tingkat aktivitas." else null
            else -> null
        }
    }

    val adaErrorTampil: Boolean
        get() = listOf("nama", "email", "jk", "tgl", "tinggi", "berat", "target", "aktivitas")
            .any { error(it) != null }

    val semuaValid: Boolean
        get() = Validator.nama(nama) == null &&
                (!edit || Validator.email(email) == null) &&
                jenisKelamin != null &&
                Validator.tanggalLahir(tanggalLahir) == null &&
                Validator.tinggi(tinggi) == null &&
                Validator.berat(berat) == null &&
                Validator.berat(targetBerat) == null &&
                BeratIdeal.cekTarget(targetBerat, tinggi) == null &&
                aktivitas != null
}

class FormAksi(
    val onNama: (String) -> Unit,
    val onEmail: (String) -> Unit,
    val onJk: (JenisKelamin) -> Unit,
    val onTanggal: (LocalDate) -> Unit,
    val onTinggi: (String) -> Unit,
    val onBerat: (String) -> Unit,
    val onTargetBerat: (String) -> Unit,
    val onAktivitas: (Aktivitas) -> Unit,
    val onSimpan: () -> Unit,
    val onBatal: () -> Unit
)

// ---------------- ViewModel ----------------

class ProfileViewModel(
    private val profileRepo: ProfileRepository,
    private val prefRepo: PreferenceRepository,
    private val userId: Long,
    private val edit: Boolean
) : ViewModel() {

    private val _form = MutableStateFlow(ProfileFormState(edit = edit))
    val form: StateFlow<ProfileFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val user = profileRepo.getUser(userId)
            val p = profileRepo.get(userId)
            _form.value = ProfileFormState(
                loading = false,
                edit = edit,
                nama = user?.nama.orEmpty(),
                email = user?.email.orEmpty(),
                jenisKelamin = p?.jenisKelamin,
                tanggalLahir = p?.tanggalLahir?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                tinggi = p?.tinggiCm?.toString().orEmpty(),
                berat = p?.beratKg?.toString().orEmpty(),
                targetBerat = p?.targetBeratKg?.toString().orEmpty(),
                aktivitas = p?.aktivitas
            )
        }
    }

    private fun ubah(kunci: String, f: (ProfileFormState) -> ProfileFormState) {
        _form.update { cur ->
            f(cur).copy(
                dikunjungi = cur.dikunjungi + kunci,
                errEmailServer = if (kunci == "email") null else cur.errEmailServer
            )
        }
    }

    private fun angka(v: String) = v.filter { it.isDigit() }.take(3)

    fun setNama(v: String) = ubah("nama") { it.copy(nama = v) }
    fun setEmail(v: String) = ubah("email") { it.copy(email = v) }
    fun setJk(v: JenisKelamin) = ubah("jk") { it.copy(jenisKelamin = v) }
    fun setTanggal(v: LocalDate) = ubah("tgl") { it.copy(tanggalLahir = v) }
    fun setTinggi(v: String) = ubah("tinggi") { it.copy(tinggi = angka(v)) }
    fun setBerat(v: String) = ubah("berat") { it.copy(berat = angka(v)) }
    fun setTargetBerat(v: String) = ubah("target") { it.copy(targetBerat = angka(v)) }
    fun setAktivitas(v: Aktivitas) = ubah("aktivitas") { it.copy(aktivitas = v) }

    fun simpan(onSuccess: () -> Unit) {
        val f = _form.value
        val jk = f.jenisKelamin
        val lahir = f.tanggalLahir
        val aktivitas = f.aktivitas
        if (!f.semuaValid || f.saving || jk == null || lahir == null || aktivitas == null) return

        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val emailBaru = if (f.edit) f.email else profileRepo.getUser(userId)?.email.orEmpty()

            val hasilUser = profileRepo.updateUser(userId, f.nama, emailBaru)
            if (hasilUser.isFailure) {
                _form.update {
                    it.copy(
                        saving = false,
                        errEmailServer = hasilUser.exceptionOrNull()?.message,
                        dikunjungi = it.dikunjungi + "email"
                    )
                }
                return@launch
            }

            val lama = profileRepo.get(userId)
            val pref = prefRepo.get(userId) ?: PreferenceEntity(userId = userId)
            val hasil = CalorieCalculator.hitung(
                jk, f.berat.toDouble(), f.tinggi.toDouble(),
                CalorieCalculator.umur(lahir), aktivitas, pref.tujuan
            )
            val manual = lama?.targetKaloriManual   // target manual disimpan terpisah, tidak ditimpa

            profileRepo.save(
                ProfileEntity(
                    userId = userId, jenisKelamin = jk, tanggalLahir = lahir.toString(),
                    tinggiCm = f.tinggi.toInt(), beratKg = f.berat.toInt(),
                    targetBeratKg = f.targetBerat.toInt(), aktivitas = aktivitas,
                    tujuan = pref.tujuan, targetKalori = hasil.target,
                    targetKaloriManual = manual
                )
            )
            // Kontrak PRD-2 -> PRD-1: target tampil di Preferensi dan dipakai generator.
            prefRepo.save(pref.copy(targetKaloriHarian = manual ?: hasil.target))

            _form.update { it.copy(saving = false) }
            onSuccess()
        }
    }
}

// ---------------- Screens (stateful) ----------------

@Composable
fun DataDiriScreen(onSelesai: () -> Unit) {
    val vm = nutriViewModel(key = "data-diri-${Session.userId}") { c ->
        ProfileViewModel(c.profileRepository, c.preferenceRepository, Session.userId, edit = false)
    }
    val state by vm.form.collectAsStateWithLifecycle()
    ProfileFormContent(state, aksiDari(vm, onSimpan = { vm.simpan(onSelesai) }, onBatal = {}))
}

@Composable
fun EditProfilScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = nutriViewModel(key = "edit-profil-${Session.userId}") { c ->
        ProfileViewModel(c.profileRepository, c.preferenceRepository, Session.userId, edit = true)
    }
    val state by vm.form.collectAsStateWithLifecycle()
    ProfileFormContent(
        state,
        aksiDari(
            vm,
            onSimpan = {
                vm.simpan {
                    Toast.makeText(context, "Data tersimpan. Target kalori diperbarui.", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            onBatal = onBack
        ),
        onBack = onBack
    )
}

private fun aksiDari(vm: ProfileViewModel, onSimpan: () -> Unit, onBatal: () -> Unit) = FormAksi(
    onNama = vm::setNama, onEmail = vm::setEmail, onJk = vm::setJk, onTanggal = vm::setTanggal,
    onTinggi = vm::setTinggi, onBerat = vm::setBerat, onTargetBerat = vm::setTargetBerat,
    onAktivitas = vm::setAktivitas, onSimpan = onSimpan, onBatal = onBatal
)

// ---------------- Content (stateless) ----------------

private data class OpsiAktivitas(val nilai: Aktivitas, val judul: String, val deskripsi: String)

private val opsiAktivitas = listOf(
    OpsiAktivitas(Aktivitas.SEDENTARI, "Sedentari", "Banyak duduk, hampir tidak berolahraga."),
    OpsiAktivitas(Aktivitas.RINGAN, "Ringan", "Aktivitas ringan atau olahraga 1–3 hari/minggu."),
    OpsiAktivitas(Aktivitas.SEDANG, "Sedang", "Olahraga sedang 3–5 hari/minggu."),
    OpsiAktivitas(Aktivitas.AKTIF, "Aktif", "Olahraga berat 6–7 hari/minggu."),
    OpsiAktivitas(Aktivitas.SANGAT_AKTIF, "Sangat aktif", "Pekerjaan fisik berat atau latihan intensif.")
)

@Composable
fun ProfileFormContent(state: ProfileFormState, aksi: FormAksi, onBack: (() -> Unit)? = null) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    var sheetTanggal by remember { mutableStateOf(false) }
    var sheetAktivitas by remember { mutableStateOf(false) }
    val adaError = state.adaErrorTampil

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.edit) {
            ScreenHeader("Edit data pribadi", onBack = onBack)
            if (adaError) InfoBanner("Periksa tanggal lahir dan ukuran tubuh.", type = BannerType.ERROR)
        } else {
            ScreenHeader("Data diri")
            Text(
                if (adaError) "Periksa kembali datamu" else "Kenali dirimu lebih baik",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                if (adaError) "Lengkapi nama dan gunakan angka yang valid untuk tinggi serta berat badan."
                else "Lengkapi data untuk membantu menyusun rencana makan yang sesuai denganmu.",
                color = TextMuted
            )
        }

        NutriTextField(state.nama, aksi.onNama, "Nama lengkap", error = state.error("nama"))
        if (state.edit) {
            NutriTextField(
                state.email, aksi.onEmail, "Email",
                error = state.error("email"), keyboardType = KeyboardType.Email
            )
        }
        DropdownPickerField(
            label = "Jenis kelamin",
            value = when (state.jenisKelamin) {
                JenisKelamin.PEREMPUAN -> "Perempuan"
                JenisKelamin.LAKI_LAKI -> "Laki-laki"
                null -> ""
            },
            options = listOf(
                JenisKelamin.PEREMPUAN to "Perempuan",
                JenisKelamin.LAKI_LAKI to "Laki-laki"
            ),
            onSelect = aksi.onJk,
            error = state.error("jk")
        )
        PickerField(
            label = "Tanggal lahir",
            value = state.tanggalLahir?.let { Formatter.tanggalLahir(it) }.orEmpty(),
            onClick = { sheetTanggal = true },
            error = state.error("tgl"),
            trailingIcon = Icons.Filled.CalendarMonth
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NutriTextField(
                value = state.tinggi, onValueChange = aksi.onTinggi, label = "Tinggi badan (cm)",
                modifier = Modifier.weight(1f), error = state.error("tinggi"),
                keyboardType = KeyboardType.Number
            )
            NutriTextField(
                value = state.berat, onValueChange = aksi.onBerat, label = "Berat badan (kg)",
                modifier = Modifier.weight(1f), error = state.error("berat"),
                keyboardType = KeyboardType.Number
            )
        }
        NutriTextField(
            state.targetBerat, aksi.onTargetBerat, "Target berat badan (kg)",
            error = state.error("target"),
            helper = "Harus dalam rentang berat ideal sesuai tinggimu.",
            keyboardType = KeyboardType.Number
        )
        InfoBeratIdeal(state.tinggi, state.berat)

        PickerField(
            label = "Tingkat aktivitas",
            value = opsiAktivitas.firstOrNull { it.nilai == state.aktivitas }?.judul.orEmpty(),
            onClick = { sheetAktivitas = true },
            error = state.error("aktivitas"),
            helper = opsiAktivitas.firstOrNull { it.nilai == state.aktivitas }?.deskripsi
        )

        if (adaError) {
            Text(
                "Perbaiki kolom yang ditandai untuk melanjutkan.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall
            )
        }

        NutriButton(
            text = when {
                state.saving -> "Menyimpan..."
                state.edit -> "Simpan Perubahan"
                else -> "Simpan & Lanjutkan"
            },
            onClick = aksi.onSimpan,
            enabled = state.semuaValid && !state.saving
        )
        if (state.edit) {
            NutriButton("Batal", onClick = aksi.onBatal, outline = true)
        } else {
            Text(
                "Data dapat diperbarui kapan saja di Profil.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Angka kebutuhan kalori adalah panduan dan bukan diagnosis medis.",
            color = TextMuted, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
    }

    if (sheetTanggal) {
        TanggalLahirSheet(
            awal = state.tanggalLahir,
            onDismiss = { sheetTanggal = false },
            onPilih = { aksi.onTanggal(it); sheetTanggal = false }
        )
    }
    if (sheetAktivitas) {
        AktivitasSheet(
            awal = state.aktivitas,
            onDismiss = { sheetAktivitas = false },
            onPilih = { aksi.onAktivitas(it); sheetAktivitas = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TanggalLahirSheet(awal: LocalDate?, onDismiss: () -> Unit, onPilih: (LocalDate) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val default = awal ?: LocalDate.of(2000, 1, 1)
    val tahunIni = LocalDate.now().year
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = default.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = (tahunIni - 80)..tahunIni
    )
    val terpilih = picker.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pilih tanggal lahir", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Tutup") }
            }
            Text(
                if (terpilih != null) "Tanggal terpilih: ${Formatter.tanggalLahir(terpilih)}"
                else "Belum ada tanggal dipilih",
                color = GreenDark, fontWeight = FontWeight.SemiBold
            )
            // Tombol di atas kalender supaya tidak terpotong di layar kecil.
            NutriButton(
                "Gunakan tanggal",
                onClick = { terpilih?.let(onPilih) },
                enabled = terpilih != null
            )
            DatePicker(
                state = picker,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    selectedDayContainerColor = GreenDark,
                    todayDateBorderColor = GreenDark,
                    todayContentColor = GreenDark
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AktivitasSheet(awal: Aktivitas?, onDismiss: () -> Unit, onPilih: (Aktivitas) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pilihan by remember { mutableStateOf(awal) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tingkat aktivitas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Tutup") }
            }
            Text("Pilih yang paling menggambarkan kebiasaanmu dalam seminggu.", color = TextMuted)

            opsiAktivitas.forEach { o ->
                val aktif = o.nilai == pilihan
                val bentuk = RoundedCornerShape(16.dp)
                Surface(
                    shape = bentuk,
                    color = if (aktif) GreenLight else Color.White,
                    border = BorderStroke(if (aktif) 2.dp else 1.dp, if (aktif) GreenDark else Color(0xFFD0D0D0)),
                    modifier = Modifier.fillMaxWidth().clip(bentuk).clickable { pilihan = o.nilai }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = aktif, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = GreenDark)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(o.judul, style = MaterialTheme.typography.titleMedium)
                            Text(o.deskripsi, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(
                            "×" + o.nilai.faktor.toString().replace('.', ','),
                            style = MaterialTheme.typography.labelLarge, color = GreenDark
                        )
                    }
                }
            }
            NutriButton(
                "Gunakan pilihan",
                onClick = { pilihan?.let(onPilih) },
                enabled = pilihan != null
            )
        }
    }
}

@Composable
private fun InfoBeratIdeal(tinggi: String, berat: String) {
    val cm = tinggi.toIntOrNull()
    if (cm == null || cm !in Batas.TINGGI) return
    val r = BeratIdeal.rentang(cm)
    val kg = berat.toIntOrNull()?.takeIf { it in Batas.BERAT }

    val baris = buildList {
        add("Berat badan ideal untuk tinggi $cm cm: ${r.min}–${r.max} kg (IMT 18,5–25,0).")
        if (kg != null) {
            val bmi = BeratIdeal.bmi(kg, cm)
            add("Berat kamu sekarang $kg kg • IMT ${BeratIdeal.format(bmi)} (${BeratIdeal.kategori(bmi).label}).")
            add(
                when {
                    kg > r.max -> "Untuk mencapai rentang ideal, turunkan minimal ${kg - r.max} kg."
                    kg < r.min -> "Untuk mencapai rentang ideal, naikkan minimal ${r.min - kg} kg."
                    else -> "Berat kamu sudah berada di rentang ideal."
                }
            )
        }
    }
    InfoBanner(baris.joinToString("\n"), type = BannerType.INFO)
}