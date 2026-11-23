package com.example.nutriplan.ui.ganti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.domain.Kandidat
import com.example.nutriplan.domain.KonteksTukar
import com.example.nutriplan.domain.MenuReplacementEngine
import com.example.nutriplan.domain.RentangHarga
import com.example.nutriplan.domain.RentangKalori
import com.example.nutriplan.domain.SwapFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SwapUiState(
    val loading: Boolean = true,
    val pesanTolak: String? = null,
    val item: MealPlanItemEntity? = null,
    val menuLama: MenuEntity? = null,
    val subjudul: String = "",
    val query: String = "",
    val filter: SwapFilter = SwapFilter(),
    val polaTerkunci: Set<String> = emptySet(),
    val alergiTerkunci: Set<String> = emptySet(),
    val hasilFilter: List<Kandidat> = emptyList(),
    val terpilihId: Long? = null,
    val totalKalori: Int = 0,
    val totalBiaya: Int = 0,
    val targetKalori: Int = 1800,
    val anggaran: Int = 50_000,
    val menyimpan: Boolean = false
) {
    /** Daftar yang tampil di layar Ganti Menu (filter + kolom cari). */
    val kandidat: List<Kandidat>
        get() = if (query.isBlank()) hasilFilter
        else hasilFilter.filter { it.menu.nama.contains(query.trim(), ignoreCase = true) }

    val terpilih: Kandidat? get() = hasilFilter.firstOrNull { it.menu.id == terpilihId }

    val kaloriBaru: Int get() = totalKalori + (terpilih?.deltaKalori ?: 0)
    val biayaBaru: Int get() = totalBiaya + (terpilih?.deltaHarga ?: 0)

    /** Waktu makan (1) + kategori lain yang aktif. */
    val jumlahFilterAktif: Int
        get() = 1 +
                (if (filter.pola.isNotEmpty()) 1 else 0) +
                (if (filter.alergi.isNotEmpty()) 1 else 0) +
                (if (filter.kalori != null) 1 else 0) +
                (if (filter.harga != null) 1 else 0)
}

/** Urutan tampil tetap, mengikuti daftar resmi. */
fun SwapFilter.polaUrut(): List<String> = Pola.semua.filter { it in pola }
fun SwapFilter.alergiUrut(): List<String> = Alergen.semua.filter { it in alergi }

/** ["kacang","udang"] -> "kacang & udang"; 3 item -> "a, b & c". */
fun gabung(daftar: List<String>, terakhir: String = " & "): String = when (daftar.size) {
    0 -> ""
    1 -> daftar[0]
    else -> daftar.dropLast(1).joinToString(", ") + terakhir + daftar.last()
}

class SwapMenuViewModel(
    private val planRepo: MealPlanRepository,
    private val menuRepo: MenuRepository,
    private val prefRepo: PreferenceRepository,
    private val itemId: Long,
    private val userId: Long
) : ViewModel() {

    private val _ui = MutableStateFlow(SwapUiState())
    val ui: StateFlow<SwapUiState> = _ui.asStateFlow()

    private var semuaMenu: List<MenuEntity> = emptyList()
    private var konteks: KonteksTukar? = null
    private var filterAwal = SwapFilter()

    init {
        viewModelScope.launch { muat() }
    }

    private suspend fun muat() {
        val item = planRepo.getItem(itemId)
        val lama = item?.let { menuRepo.getById(it.menuId) }
        if (item == null || lama == null) {
            _ui.value = SwapUiState(loading = false, pesanTolak = "Menu tidak ditemukan.")
            return
        }

        val pref = prefRepo.get(userId) ?: PreferenceEntity(userId = userId)
        val itemsHari = planRepo.getItems(item.planId).filter { it.hari == item.hari }
        semuaMenu = menuRepo.getAll()

        val polaKunci = pref.polaMakan.filter { it != Pola.TANPA_SUSU }.toSet()
        val alergiKunci = pref.alergi.toSet() +
                (if (Pola.TANPA_SUSU in pref.polaMakan) setOf(Alergen.SUSU) else emptySet())

        val dataLengkap = item.kaloriSnapshot > 0 && item.hargaSnapshot > 0
        filterAwal = SwapFilter(
            pola = polaKunci,
            alergi = alergiKunci,
            kalori = if (dataLengkap) RentangKalori.dari(item.kaloriSnapshot) else null,
            harga = if (dataLengkap) RentangHarga.dari(item.hargaSnapshot) else null
        )

        val k = KonteksTukar(
            menuLama = lama,
            kaloriLama = item.kaloriSnapshot,
            hargaLama = item.hargaSnapshot,
            totalKaloriHari = itemsHari.sumOf { it.kaloriSnapshot },
            totalBiayaHari = itemsHari.sumOf { it.hargaSnapshot },
            targetKalori = pref.targetKaloriHarian,
            anggaran = pref.anggaranHarian
        )
        konteks = k

        val format = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val subjudul = "${LocalDate.parse(item.tanggal).format(format)} • ${item.waktuMakan.jam}"

        _ui.value = SwapUiState(
            loading = false,
            pesanTolak = if (dataLengkap) null
            else "Penukaran ditolak: kalori atau harga menu semula tidak lengkap. Menu semula tetap tersimpan.",
            item = item,
            menuLama = lama,
            subjudul = subjudul,
            filter = filterAwal,
            polaTerkunci = polaKunci,
            alergiTerkunci = alergiKunci,
            hasilFilter = MenuReplacementEngine.findCandidates(k, semuaMenu, filterAwal),
            totalKalori = k.totalKaloriHari,
            totalBiaya = k.totalBiayaHari,
            targetKalori = k.targetKalori,
            anggaran = k.anggaran
        )
    }

    fun setQuery(q: String) {
        _ui.update { it.copy(query = q) }
    }

    /** Ketuk lagi untuk membatalkan pilihan. */
    fun pilih(id: Long) {
        _ui.update { it.copy(terpilihId = if (it.terpilihId == id) null else id) }
    }

    private fun terapkan(filter: SwapFilter) {
        val k = konteks ?: return
        val hasil = MenuReplacementEngine.findCandidates(k, semuaMenu, filter)
        _ui.update { s ->
            s.copy(
                filter = filter,
                hasilFilter = hasil,
                terpilihId = s.terpilihId?.takeIf { id -> hasil.any { it.menu.id == id } }
            )
        }
    }

    fun togglePola(p: String) {
        val s = _ui.value
        if (p in s.polaTerkunci) return
        terapkan(s.filter.copy(pola = if (p in s.filter.pola) s.filter.pola - p else s.filter.pola + p))
    }

    fun toggleAlergi(a: String) {
        val s = _ui.value
        if (a in s.alergiTerkunci) return
        terapkan(s.filter.copy(alergi = if (a in s.filter.alergi) s.filter.alergi - a else s.filter.alergi + a))
    }

    fun pilihKalori(r: RentangKalori) {
        val f = _ui.value.filter
        terapkan(f.copy(kalori = if (f.kalori == r) null else r))
    }

    fun pilihHarga(r: RentangHarga) {
        val f = _ui.value.filter
        terapkan(f.copy(harga = if (f.harga == r) null else r))
    }

    fun resetFilter() = terapkan(filterAwal)

    /** Simpan penggantian. onSelesai(true) bila berhasil. */
    fun konfirmasi(sudahDimakan: Boolean, onSelesai: (Boolean) -> Unit) {
        val s = _ui.value
        val menu = s.terpilih?.menu ?: return
        if (s.menyimpan) return
        viewModelScope.launch {
            _ui.update { it.copy(menyimpan = true) }
            val ok = planRepo.confirmSwap(itemId, menu, sudahDimakan)
            _ui.update { it.copy(menyimpan = false) }
            onSelesai(ok)
        }
    }
}