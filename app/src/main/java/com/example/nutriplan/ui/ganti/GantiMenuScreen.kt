package com.example.nutriplan.ui.ganti

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.domain.Kandidat
import com.example.nutriplan.domain.SwapFilter
import com.example.nutriplan.ui.common.nutriViewModelOwner
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.util.Formatter

// ---------------- Screen (stateful) ----------------

@Composable
fun GantiMenuScreen(
    itemId: Long,
    owner: ViewModelStoreOwner,
    onBack: () -> Unit,
    onFilter: () -> Unit,
    onLanjut: () -> Unit
) {
    val vm = nutriViewModelOwner(owner) { c ->
        SwapMenuViewModel(
            c.mealPlanRepository, c.menuRepository, c.preferenceRepository,
            itemId, Session.userId
        )
    }
    val s by vm.ui.collectAsStateWithLifecycle()
    GantiMenuContent(
        s = s,
        onBack = onBack,
        onQuery = vm::setQuery,
        onPilih = vm::pilih,
        onFilter = onFilter,
        onLanjut = onLanjut
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun GantiMenuContent(
    s: SwapUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onPilih: (Long) -> Unit,
    onFilter: () -> Unit,
    onLanjut: () -> Unit
) {
    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    val item = s.item
    val lama = s.menuLama
    if (item == null || lama == null || s.pesanTolak != null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Ganti Menu", onBack = onBack)
            InfoBanner(s.pesanTolak ?: "Menu tidak ditemukan.", type = BannerType.ERROR)
            NutriButton("Kembali", onClick = onBack, outline = true)
        }
        return
    }

    val terpilih = s.terpilih
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("Ganti Menu", onBack = onBack) }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EFE6))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "MENU SAAT INI • ${item.waktuMakan.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MenuPhoto(lama.fotoRes, Modifier.size(56.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(lama.nama, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    Formatter.kkalHarga(item.kaloriSnapshot, item.hargaSnapshot),
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = s.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari menu pengganti") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item { RingkasanFilter(s, onFilter) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (terpilih != null) "Pilih Alternatif" else "Rekomendasi untukmu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (terpilih != null) "1 terpilih" else "${s.kandidat.size} menu",
                        color = GreenDark, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (s.hasilFilter.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoBanner(
                            "Menu pengganti tidak tersedia untuk pilihan ini. Menu semula tetap tersimpan.",
                            type = BannerType.WARNING
                        )
                        NutriButton("Ubah filter", onClick = onFilter, outline = true)
                    }
                }
            } else if (s.kandidat.isEmpty()) {
                item { Text("Tidak ada menu dengan nama itu.", color = TextMuted) }
            }

            items(s.kandidat, key = { it.menu.id }) { k ->
                KartuKandidat(
                    k = k,
                    filter = s.filter,
                    terpilih = k.menu.id == s.terpilihId,
                    onPilih = { onPilih(k.menu.id) }
                )
            }

            item {
                Text(
                    "Menu semula tetap tersimpan hingga dikonfirmasi.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
            }
        }

        if (terpilih != null) {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(ringkasSelisih(terpilih), color = GreenDark, fontWeight = FontWeight.SemiBold)
                    NutriButton("Lanjut ke Konfirmasi", onClick = onLanjut)
                }
            }
        }
    }
}

@Composable
private fun RingkasanFilter(s: SwapUiState, onFilter: () -> Unit) {
    val f = s.filter
    val waktu = s.item?.waktuMakan?.label?.removePrefix("Makan ").orEmpty()
    val baris1 = buildList {
        add(waktu)
        addAll(f.polaUrut())
        f.kalori?.let { add(it.label) }
    }.joinToString(" • ")
    val baris2 = buildList {
        if (f.alergi.isNotEmpty()) add("Tanpa ${gabung(f.alergiUrut().map { it.lowercase() })}")
        f.harga?.let { add(it.ringkas) }
    }.joinToString(" • ")

    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = GreenLight) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(baris1, color = GreenDark, fontWeight = FontWeight.SemiBold)
                if (baris2.isNotEmpty()) {
                    Text(baris2, color = GreenDark, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onFilter,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Tune, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Filter • ${s.jumlahFilterAktif}")
            }
        }
    }
}

@Composable
private fun KartuKandidat(k: Kandidat, filter: SwapFilter, terpilih: Boolean, onPilih: () -> Unit) {
    val m = k.menu
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (terpilih) BorderStroke(2.dp, GreenDark) else null
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MenuPhoto(m.fotoRes, Modifier.size(68.dp))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.nama, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Formatter.kkalHarga(m.kalori, m.harga),
                        color = GreenDark, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "P ${m.protein} g • K ${m.karbo} g • L ${m.lemak} g",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
            }
            Text(bahanSingkat(m, filter), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            NutriButton(
                text = if (terpilih) "Menu Terpilih" else "Pilih Menu Ini",
                onClick = onPilih,
                outline = !terpilih,
                icon = if (terpilih) Icons.Filled.Check else null
            )
        }
    }
}

/** "Ayam, nasi merah, brokoli, wortel. Tanpa kacang & udang." */
private fun bahanSingkat(menu: MenuEntity, filter: SwapFilter): String {
    val bahan = menu.bahan.take(4).joinToString(", ") { it.replace(Regex("\\s+\\d.*$"), "") }
    val tanpa = filter.alergiUrut().map { it.lowercase() }
    return if (tanpa.isEmpty()) "$bahan." else "$bahan. Tanpa ${gabung(tanpa)}."
}

private fun ringkasSelisih(k: Kandidat): String {
    val harga = when {
        k.deltaHarga < 0 -> "Hemat ${Formatter.rupiahRapat(-k.deltaHarga)}"
        k.deltaHarga > 0 -> "Lebih mahal ${Formatter.rupiahRapat(k.deltaHarga)}"
        else -> "Harga sama"
    }
    val kalori = when {
        k.deltaKalori < 0 -> "${Formatter.kkal(-k.deltaKalori)} lebih ringan"
        k.deltaKalori > 0 -> "${Formatter.kkal(k.deltaKalori)} lebih berat"
        else -> "Kalori sama"
    }
    return "$harga • $kalori. Menu semula belum berubah."
}