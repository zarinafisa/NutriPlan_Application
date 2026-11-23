package com.example.nutriplan.ui.ganti

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutriplan.data.Session
import com.example.nutriplan.ui.common.nutriViewModelOwner
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.OrangeBar
import com.example.nutriplan.ui.theme.RedError
import com.example.nutriplan.util.Formatter
import kotlin.math.abs

// ---------------- Screen (stateful) ----------------

@Composable
fun KonfirmasiMenuScreen(
    itemId: Long,
    owner: ViewModelStoreOwner,
    onBack: () -> Unit,
    onBatal: () -> Unit,
    onSelesai: () -> Unit
) {
    val context = LocalContext.current
    val vm = nutriViewModelOwner(owner) { c ->
        SwapMenuViewModel(
            c.mealPlanRepository, c.menuRepository, c.preferenceRepository,
            itemId, Session.userId
        )
    }
    val s by vm.ui.collectAsStateWithLifecycle()
    KonfirmasiContent(
        s = s,
        onBack = onBack,
        onBatal = onBatal,
        onKonfirmasi = { dimakan ->
            vm.konfirmasi(dimakan) { ok ->
                if (ok) {
                    Toast.makeText(context, "Menu berhasil diganti.", Toast.LENGTH_SHORT).show()
                    onSelesai()
                } else {
                    Toast.makeText(context, "Penggantian gagal. Menu semula tidak berubah.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun KonfirmasiContent(
    s: SwapUiState,
    onBack: () -> Unit,
    onBatal: () -> Unit,
    onKonfirmasi: (Boolean) -> Unit
) {
    var tanya by remember { mutableStateOf(false) }

    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    val item = s.item
    val lama = s.menuLama
    val baru = s.terpilih
    if (item == null || lama == null || baru == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Konfirmasi", onBack = onBack)
            Text("Belum ada menu pengganti yang dipilih.", color = TextMuted)
            NutriButton("Kembali", onClick = onBack, outline = true)
        }
        return
    }
    val m = baru.menu

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("Konfirmasi Menu Pengganti", onBack = onBack)
        Text(s.subjudul, style = MaterialTheme.typography.titleMedium)
        Text(
            "Hanya menu ${item.waktuMakan.label.lowercase()} yang akan diganti.",
            color = TextMuted
        )

        KartuMenu(
            label = "MENU SEMULA",
            nama = lama.nama,
            ringkas = Formatter.kkalHarga(item.kaloriSnapshot, item.hargaSnapshot),
            foto = lama.fotoRes,
            warna = Color.White,
            border = null
        )
        KartuMenu(
            label = "MENU PENGGANTI",
            nama = m.nama,
            ringkas = Formatter.kkalHarga(m.kalori, m.harga),
            foto = m.fotoRes,
            warna = GreenLight,
            border = BorderStroke(2.dp, GreenDark)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KartuSelisih(
                Icons.Filled.LocalFireDepartment, selisihKkal(baru.deltaKalori), Modifier.weight(1f)
            )
            KartuSelisih(
                Icons.Filled.AccountBalanceWallet, selisihRupiah(baru.deltaHarga), Modifier.weight(1f)
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dampak pada rencana harian", style = MaterialTheme.typography.titleMedium)
                BarisDampak(
                    "Total kalori",
                    "${Formatter.angka(s.totalKalori)} → ${Formatter.kkal(s.kaloriBaru)}"
                )
                BarisDampak(
                    "Total biaya",
                    "${Formatter.rupiahRapat(s.totalBiaya)} → ${Formatter.rupiahRapat(s.biayaBaru)}"
                )
                val lewat = s.kaloriBaru > s.targetKalori || s.biayaBaru > s.anggaran
                Text(
                    keteranganDampak(s),
                    color = if (lewat) RedError else GreenDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        NutriButton(
            text = if (s.menyimpan) "Menyimpan..." else "Konfirmasi Penggantian",
            onClick = { if (item.sudahDimakan) tanya = true else onKonfirmasi(false) },
            enabled = !s.menyimpan
        )
        NutriButton("Batal", onClick = onBatal, outline = true)
        Text(
            "Biaya adalah estimasi per porsi.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
    }

    if (tanya) {
        AlertDialog(
            onDismissRequest = { tanya = false },
            title = { Text("Menu semula sudah dimakan") },
            text = {
                Text(
                    "Menu pengganti dianggap sebagai apa? Menu semula dan menu pengganti tidak akan dihitung bersamaan."
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { tanya = false; onKonfirmasi(true) }) {
                        Text("Sudah dimakan", color = GreenDark)
                    }
                    TextButton(onClick = { tanya = false; onKonfirmasi(false) }) {
                        Text("Belum dimakan", color = GreenDark)
                    }
                    TextButton(onClick = { tanya = false }) { Text("Batal") }
                }
            }
        )
    }
}

@Composable
private fun KartuMenu(
    label: String, nama: String, ringkas: String, foto: String,
    warna: Color, border: BorderStroke?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = warna),
        border = border
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MenuPhoto(foto, Modifier.size(60.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(nama, style = MaterialTheme.typography.titleMedium)
                    Text(ringkas, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun KartuSelisih(ikon: ImageVector, teks: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ikon, null, tint = OrangeBar)
            Spacer(Modifier.width(8.dp))
            Text(teks, style = MaterialTheme.typography.titleMedium, color = GreenDark)
        }
    }
}

@Composable
private fun BarisDampak(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = TextMuted)
        Text(nilai, fontWeight = FontWeight.Bold)
    }
}

private fun tanda(n: Int) = when {
    n > 0 -> "+"
    n < 0 -> "−"
    else -> ""
}

private fun selisihKkal(n: Int) = "${tanda(n)}${Formatter.kkal(abs(n))}"
private fun selisihRupiah(n: Int) = "${tanda(n)}${Formatter.rupiahRapat(abs(n))}"

private fun keteranganDampak(s: SwapUiState): String {
    val sisaKalori = s.targetKalori - s.kaloriBaru
    val sisaBiaya = s.anggaran - s.biayaBaru
    val a = if (sisaKalori >= 0)
        "Sisa target ${Formatter.kkal(sisaKalori)} dari ${Formatter.kkal(s.targetKalori)}."
    else "Melebihi target ${Formatter.kkal(-sisaKalori)}."
    val b = if (sisaBiaya >= 0)
        "Sisa anggaran ${Formatter.rupiahRapat(sisaBiaya)} dari ${Formatter.rupiahRapat(s.anggaran)}."
    else "Melebihi anggaran ${Formatter.rupiahRapat(-sisaBiaya)}."
    return "$a $b"
}