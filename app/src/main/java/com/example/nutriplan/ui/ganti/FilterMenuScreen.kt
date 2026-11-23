package com.example.nutriplan.ui.ganti

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.domain.RentangHarga
import com.example.nutriplan.domain.RentangKalori
import com.example.nutriplan.ui.common.nutriViewModelOwner
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.RedError
import com.example.nutriplan.util.Formatter

// ---------------- Screen (stateful) ----------------

@Composable
fun FilterMenuScreen(itemId: Long, owner: ViewModelStoreOwner, onBack: () -> Unit) {
    val vm = nutriViewModelOwner(owner) { c ->
        SwapMenuViewModel(
            c.mealPlanRepository, c.menuRepository, c.preferenceRepository,
            itemId, Session.userId
        )
    }
    val s by vm.ui.collectAsStateWithLifecycle()
    FilterMenuContent(
        s = s,
        onBack = onBack,
        onReset = vm::resetFilter,
        onPola = vm::togglePola,
        onAlergi = vm::toggleAlergi,
        onKalori = vm::pilihKalori,
        onHarga = vm::pilihHarga
    )
}

// ---------------- Content (stateless) ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterMenuContent(
    s: SwapUiState,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onPola: (String) -> Unit,
    onAlergi: (String) -> Unit,
    onKalori: (RentangKalori) -> Unit,
    onHarga: (RentangHarga) -> Unit
) {
    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    val waktu = s.item?.waktuMakan
    val jumlah = s.hasilFilter.size
    val f = s.filter

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("Filter Menu", onBack = onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sesuaikan menu penggantimu", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${s.jumlahFilterAktif} kategori aktif untuk ${waktu?.label?.lowercase().orEmpty()}.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
            }
            TextButton(onClick = onReset) {
                Text("Reset", color = GreenDark, fontWeight = FontWeight.SemiBold)
            }
        }

        KartuGrup("Waktu makan", "Penukaran hanya untuk waktu makan yang sama.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WaktuMakan.entries.forEach { w ->
                    val aktif = w == waktu
                    Pil(label = w.label, terpilih = aktif, aktif = aktif, onClick = {})
                }
            }
        }

        KartuGrup("Pola makan", if (s.polaTerkunci.isNotEmpty()) "Pola dari Preferensi tidak bisa dimatikan di sini." else null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pola.semua.filter { it != Pola.TANPA_SUSU }.forEach { p ->
                    Pil(
                        label = p,
                        terpilih = p in f.pola,
                        aktif = p !in s.polaTerkunci,
                        onClick = { onPola(p) }
                    )
                }
            }
        }

        val alergiTeks = f.alergiUrut().map { it.lowercase() }
        KartuGrup(
            "Alergi makanan",
            if (alergiTeks.isEmpty()) "Belum ada bahan yang dihindari."
            else "Bahan yang dihindari. Menu tanpa ${gabung(alergiTeks, " dan ")} saja yang ditampilkan."
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Alergen.semua.forEach { a ->
                    Pil(
                        label = a,
                        terpilih = a in f.alergi,
                        aktif = a !in s.alergiTerkunci,
                        onClick = { onAlergi(a) }
                    )
                }
            }
        }

        KartuGrup("Rentang kalori", "Per porsi • target harian tetap ${Formatter.kkal(s.targetKalori)}.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RentangKalori.entries.forEach { r ->
                    Pil(label = r.label, terpilih = f.kalori == r, onClick = { onKalori(r) })
                }
            }
        }

        KartuGrup("Rentang harga", "Per porsi • anggaran harian ${Formatter.rupiahRapat(s.anggaran)}.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RentangHarga.entries.forEach { r ->
                    Pil(label = r.label, terpilih = f.harga == r, onClick = { onHarga(r) })
                }
            }
        }

        if (jumlah > 0) {
            Text("✓ $jumlah menu sesuai semua preferensimu.", color = GreenDark, fontWeight = FontWeight.SemiBold)
        } else {
            Text("Tidak ada menu yang sesuai. Coba longgarkan filter.", color = RedError, fontWeight = FontWeight.SemiBold)
        }

        NutriButton(
            text = "Tampilkan $jumlah Menu",
            onClick = onBack,
            enabled = jumlah > 0
        )

        InfoBanner(
            "Pilihan alergi membantu menyaring menu, tetapi tidak menjamin bebas alergen. Konfirmasi bahan kepada penyedia makanan.",
            type = BannerType.WARNING
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KartuGrup(judul: String, catatan: String?, isi: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(judul, style = MaterialTheme.typography.titleMedium)
            isi()
            if (catatan != null) {
                Text(catatan, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun Pil(label: String, terpilih: Boolean, aktif: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = terpilih,
        onClick = onClick,
        enabled = aktif,
        label = { Text(label) },
        leadingIcon = if (terpilih) {
            { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GreenLight,
            selectedLabelColor = GreenDark,
            selectedLeadingIconColor = GreenDark
        )
    )
}