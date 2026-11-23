package com.example.nutriplan.ui.riwayat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.local.StatusRencana
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------------- State & ViewModel ----------------

enum class FilterRiwayat(val label: String) { SEMUA("Semua"), AKTIF("Aktif"), SELESAI("Selesai") }

data class RiwayatItemUi(
    val planId: Long,
    val nama: String,
    val mulai: LocalDate,
    val selesai: LocalDate,
    val durasi: Int,
    val status: StatusRencana,
    val hariKe: Int,
    val rataKalori: Int,
    val totalBiaya: Int
)

data class BulanGrup(val judul: String, val items: List<RiwayatItemUi>)

data class RiwayatUiState(
    val loading: Boolean = true,
    val filter: FilterRiwayat = FilterRiwayat.SEMUA,
    val query: String = "",
    val jumlahTotal: Int = 0,
    val jumlahSelesai: Int = 0,
    val grup: List<BulanGrup> = emptyList()
)

class HistoryViewModel(planRepo: MealPlanRepository, userId: Long) : ViewModel() {

    private val filter = MutableStateFlow(FilterRiwayat.SEMUA)
    private val query = MutableStateFlow("")

    val ui: StateFlow<RiwayatUiState> = combine(
        planRepo.observePlans(userId), planRepo.observeAllItems(userId), filter, query
    ) { plans, items, f, q -> bangun(plans, items, f, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiwayatUiState())

    fun setFilter(f: FilterRiwayat) { filter.value = f }
    fun setQuery(q: String) { query.value = q }

    private fun bangun(
        plans: List<MealPlanEntity>,
        items: List<MealPlanItemEntity>,
        f: FilterRiwayat,
        q: String
    ): RiwayatUiState {
        val perPlan = items.groupBy { it.planId }
        val semua = plans.map { p ->
            val mulai = LocalDate.parse(p.tanggalMulai)
            val its = perPlan[p.id].orEmpty()
            RiwayatItemUi(
                planId = p.id,
                nama = p.nama,
                mulai = mulai,
                selesai = mulai.plusDays(p.durasi - 1L),
                durasi = p.durasi,
                status = p.status,
                hariKe = (ChronoUnit.DAYS.between(mulai, LocalDate.now()) + 1).toInt().coerceIn(1, p.durasi),
                rataKalori = its.sumOf { it.kaloriSnapshot } / p.durasi.coerceAtLeast(1),
                totalBiaya = its.sumOf { it.hargaSnapshot }
            )
        }
        val tampil = semua.filter {
            val cocokStatus = when (f) {
                FilterRiwayat.SEMUA -> true
                FilterRiwayat.AKTIF -> it.status == StatusRencana.AKTIF
                FilterRiwayat.SELESAI -> it.status == StatusRencana.SELESAI
            }
            cocokStatus && (q.isBlank() || it.nama.contains(q.trim(), ignoreCase = true))
        }
        return RiwayatUiState(
            loading = false,
            filter = f,
            query = q,
            jumlahTotal = semua.size,
            jumlahSelesai = semua.count { it.status == StatusRencana.SELESAI },
            grup = tampil.groupBy { Formatter.bulanTahun(it.mulai) }
                .map { (judul, list) -> BulanGrup(judul, list) }
        )
    }
}

// ---------------- Screen (stateful) ----------------

/** onBuka(planId, aktif): aktif = true bila rencana yang dibuka adalah rencana aktif. */
@Composable
fun RiwayatScreen(onBuka: (Long, Boolean) -> Unit, onSusunRencana: () -> Unit) {
    val vm = nutriViewModel(key = "riwayat-${Session.userId}") { c ->
        HistoryViewModel(c.mealPlanRepository, Session.userId)
    }
    val s by vm.ui.collectAsStateWithLifecycle()
    RiwayatContent(
        s = s,
        onFilter = vm::setFilter,
        onQuery = vm::setQuery,
        onBuka = onBuka,
        onSusunRencana = onSusunRencana
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun RiwayatContent(
    s: RiwayatUiState,
    onFilter: (FilterRiwayat) -> Unit,
    onQuery: (String) -> Unit,
    onBuka: (Long, Boolean) -> Unit,
    onSusunRencana: () -> Unit
) {
    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    var cari by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Riwayat rencana", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { cari = !cari; if (!cari) onQuery("") }) {
                    Icon(if (cari) Icons.Filled.Close else Icons.Filled.Search, "Cari rencana")
                }
            }
        }
        if (cari) {
            item {
                OutlinedTextField(
                    value = s.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari nama rencana") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = GreenLight) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        when {
                            s.jumlahTotal == 0 -> "Mulai kebiasaan teraturmu"
                            s.jumlahSelesai == 0 -> "Rencana pertamamu sedang berjalan"
                            else -> "${s.jumlahSelesai} rencana selesai, terus semangat!"
                        },
                        style = MaterialTheme.typography.titleMedium, color = GreenDark
                    )
                    Text(
                        "${s.jumlahTotal} rencana tersimpan • ${s.jumlahSelesai} selesai",
                        color = GreenDark, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterRiwayat.entries.forEach { f ->
                    FilterChip(
                        selected = f == s.filter,
                        onClick = { onFilter(f) },
                        label = { Text(f.label) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenDark,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (s.grup.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (s.jumlahTotal == 0) "Belum ada rencana tersimpan."
                        else "Tidak ada rencana yang cocok dengan filter ini.",
                        color = TextMuted
                    )
                    if (s.jumlahTotal == 0) NutriButton("Susun rencana", onClick = onSusunRencana)
                }
            }
        }

        s.grup.forEach { g ->
            item(key = "bulan-${g.judul}") {
                Text(g.judul, style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }
            items(g.items, key = { it.planId }) { r ->
                if (r.status == StatusRencana.AKTIF) KartuAktif(r) { onBuka(r.planId, true) }
                else KartuSelesai(r) { onBuka(r.planId, false) }
            }
        }

        item {
            Text(
                "Buka rencana lama untuk melihat menu dan menggunakannya kembali.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
        }
    }
}

@Composable
private fun KartuAktif(r: RiwayatItemUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, GreenDark)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatter.rentangTanggal(r.mulai, r.selesai),
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(50), color = GreenLight) {
                    Text(
                        "AKTIF • HARI ${r.hariKe}",
                        style = MaterialTheme.typography.labelSmall, color = GreenDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Text(r.nama, style = MaterialTheme.typography.titleLarge)
            Text(
                "Rata-rata ${Formatter.kkal(r.rataKalori)} • Total ${Formatter.rupiah(r.totalBiaya)}",
                color = TextMuted
            )
            Text("Rencana makan ${r.durasi} hari", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text("Buka rencana →", color = GreenDark, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KartuSelesai(r: RiwayatItemUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(GreenLight),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.CheckCircle, null, tint = GreenDark) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Formatter.rentangTanggal(r.mulai, r.selesai),
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
                Text(r.nama, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Formatter.kkal(r.rataKalori)} • ${Formatter.rupiah(r.totalBiaya)}",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted
                )
                Text(
                    "✓ Selesai • ${r.durasi} hari",
                    style = MaterialTheme.typography.bodySmall, color = GreenDark, fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
    }
}