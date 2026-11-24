package com.example.nutriplan.ui.rencana

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.local.StatusRencana
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.DayTabs
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.BigNumberStyle
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.OrangeBar
import com.example.nutriplan.ui.theme.OrangeSoft
import com.example.nutriplan.ui.theme.RedError
import com.example.nutriplan.ui.theme.UnitStyle
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------------- State & ViewModel ----------------

data class SlotUi(val waktu: WaktuMakan, val item: MealPlanItemEntity?, val menu: MenuEntity?)

data class PlanUiState(
    val loading: Boolean = true,
    val ada: Boolean = false,
    val status: StatusRencana = StatusRencana.AKTIF,
    val namaRencana: String = "",
    val durasi: Int = 0,
    val rentang: String = "",
    val hariIni: Int = 1,
    val hari: Int = 1,
    val tanggal: List<LocalDate> = emptyList(),
    val slots: List<SlotUi> = emptyList(),
    val totalKalori: Int = 0,      // yang SUDAH DIMAKAN
    val rencanaKalori: Int = 0,    // total semua menu di rencana hari itu
    val targetKalori: Int = 1800,
    val totalBiaya: Int = 0,       // yang SUDAH DIMAKAN
    val rencanaBiaya: Int = 0,
    val anggaran: Int = 50_000,
    val protein: Int = 0,          // dari menu yang sudah dimakan
    val karbo: Int = 0,
    val lemak: Int = 0
) {
    /** Rencana yang sudah selesai hanya dilihat (tidak bisa tambah atau ganti menu). */
    val bacaSaja: Boolean get() = status == StatusRencana.SELESAI
}

@OptIn(ExperimentalCoroutinesApi::class)
class MealPlanViewModel(
    private val planRepo: MealPlanRepository,
    menuRepo: MenuRepository,
    prefRepo: PreferenceRepository,
    private val userId: Long,
    private val planId: Long?          // null = rencana aktif
) : ViewModel() {

    private val hariDipilih = MutableStateFlow<Int?>(null)
    private val planFlow = if (planId == null) planRepo.observeActivePlan(userId)
    else planRepo.observePlan(planId)
    private val itemsFlow = planFlow.flatMapLatest { p ->
        if (p == null) flowOf(emptyList<MealPlanItemEntity>()) else planRepo.observeItems(p.id)
    }

    val ui: StateFlow<PlanUiState> = combine(
        planFlow, itemsFlow, menuRepo.observeAll(), prefRepo.observe(userId), hariDipilih
    ) { plan, items, menus, pref, dipilih ->
        bangun(plan, items, menus, pref, dipilih)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun pilihHari(h: Int) { hariDipilih.value = h }

    /** Tandai / batalkan "sudah makan" langsung dari kartu jadwal. */
    fun tandaiDimakan(itemId: Long, dimakan: Boolean) {
        viewModelScope.launch { planRepo.markEaten(itemId, dimakan) }
    }

    /** Salin rencana lama menjadi rencana aktif baru. */
    fun gunakanLagi(onSelesai: (Boolean) -> Unit) {
        val id = planId ?: return
        viewModelScope.launch {
            val baru = planRepo.reusePlan(id, userId, LocalDate.now())
            onSelesai(baru != null)
        }
    }

    private fun bangun(
        plan: MealPlanEntity?,
        items: List<MealPlanItemEntity>,
        menus: List<MenuEntity>,
        pref: PreferenceEntity?,
        dipilih: Int?
    ): PlanUiState {
        val target = pref?.targetKaloriHarian ?: 1800
        val anggaran = pref?.anggaranHarian ?: 50_000
        if (plan == null) {
            return PlanUiState(loading = false, ada = false, targetKalori = target, anggaran = anggaran)
        }
        val mulai = LocalDate.parse(plan.tanggalMulai)
        val hariIni = (ChronoUnit.DAYS.between(mulai, LocalDate.now()) + 1).toInt().coerceIn(1, plan.durasi)
        val awal = if (plan.status == StatusRencana.SELESAI) 1 else hariIni
        val hari = (dipilih ?: awal).coerceIn(1, plan.durasi)

        val peta = menus.associateBy { it.id }
        val itemHari = items.filter { it.hari == hari }
        val slots = listOf(WaktuMakan.SARAPAN, WaktuMakan.SIANG, WaktuMakan.CAMILAN, WaktuMakan.MALAM).map { w ->
            val item = itemHari.firstOrNull { it.waktuMakan == w }
            SlotUi(w, item, item?.let { peta[it.menuId] })
        }
        val dimakan = itemHari.filter { it.sudahDimakan }
        return PlanUiState(
            loading = false,
            ada = true,
            status = plan.status,
            namaRencana = plan.nama,
            durasi = plan.durasi,
            rentang = Formatter.rentangTanggal(mulai, mulai.plusDays(plan.durasi - 1L)),
            hariIni = hariIni,
            hari = hari,
            tanggal = (0 until plan.durasi).map { mulai.plusDays(it.toLong()) },
            slots = slots,
            totalKalori = dimakan.sumOf { it.kaloriSnapshot },
            rencanaKalori = itemHari.sumOf { it.kaloriSnapshot },
            targetKalori = target,
            totalBiaya = dimakan.sumOf { it.hargaSnapshot },
            rencanaBiaya = itemHari.sumOf { it.hargaSnapshot },
            anggaran = anggaran,
            protein = dimakan.sumOf { peta[it.menuId]?.protein ?: 0 },
            karbo = dimakan.sumOf { peta[it.menuId]?.karbo ?: 0 },
            lemak = dimakan.sumOf { peta[it.menuId]?.lemak ?: 0 }
        )
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun RencanaScreen(
    planId: Long? = null,
    onBack: () -> Unit,
    onDetail: (Long) -> Unit,
    onTambah: (Int, WaktuMakan) -> Unit,
    onGanti: (Int, WaktuMakan) -> Unit,
    onBuatRencana: () -> Unit,
    onGunakanLagiSelesai: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm = nutriViewModel(key = "rencana-${Session.userId}-${planId ?: 0L}") { c ->
        MealPlanViewModel(
            c.mealPlanRepository, c.menuRepository, c.preferenceRepository,
            Session.userId, planId
        )
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    RencanaContent(
        state = state,
        onBack = onBack,
        onPilihHari = vm::pilihHari,
        onDetail = onDetail,
        onTambah = onTambah,
        onGanti = onGanti,
        onBuatRencana = onBuatRencana,
        onTandai = vm::tandaiDimakan,
        onGunakanLagi = {
            vm.gunakanLagi { ok ->
                if (ok) {
                    Toast.makeText(context, "Rencana disalin dan dijadikan rencana aktif.", Toast.LENGTH_SHORT).show()
                    onGunakanLagiSelesai()
                } else {
                    Toast.makeText(context, "Rencana gagal disalin.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun RencanaContent(
    state: PlanUiState,
    onBack: () -> Unit,
    onPilihHari: (Int) -> Unit,
    onDetail: (Long) -> Unit,
    onTambah: (Int, WaktuMakan) -> Unit,
    onGanti: (Int, WaktuMakan) -> Unit,
    onTandai: (Long, Boolean) -> Unit,
    onBuatRencana: () -> Unit,
    onGunakanLagi: () -> Unit
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    if (!state.ada) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Rencana Makan", onBack = onBack)
            Spacer(Modifier.height(24.dp))
            Text("Belum ada rencana aktif.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Atur preferensimu, lalu simpan untuk menyusun rencana makan.",
                color = TextMuted
            )
            NutriButton("Buat rencana", onClick = onBuatRencana)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Rencana Makan", onBack = onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.rentang, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = GreenLight) {
                Text(
                    if (state.bacaSaja) "SELESAI • ${state.durasi} HARI" else "AKTIF • HARI ${state.hariIni}",
                    style = MaterialTheme.typography.labelSmall, color = GreenDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (state.bacaSaja) {
            Text(state.namaRencana, style = MaterialTheme.typography.titleMedium)
        }

        DayTabs(state.tanggal, state.hari, onPilihHari)
        SummaryCard(state)
        BudgetCard(state)

        if (state.rencanaKalori > state.targetKalori) {
            InfoBanner(
                "Rencana hari ini melebihi target ${Formatter.kkal(state.rencanaKalori - state.targetKalori)}.",
                type = BannerType.WARNING
            )
        }
        if (state.rencanaBiaya > state.anggaran) {
            InfoBanner(
                "Rencana hari ini melebihi anggaran ${Formatter.rupiahRapat(state.rencanaBiaya - state.anggaran)}.",
                type = BannerType.WARNING
            )
        }

        state.slots.forEach { slot ->
            SlotCard(
                slot = slot,
                bacaSaja = state.bacaSaja,
                onTambah = { onTambah(state.hari, slot.waktu) },
                onGanti = { onGanti(state.hari, slot.waktu) },
                onDetail = onDetail,
                onTandai = onTandai
            )
        }

        if (state.bacaSaja) {
            NutriButton("Gunakan rencana ini lagi", onClick = onGunakanLagi)
        }

        Text(
            "Biaya adalah estimasi per porsi.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryCard(s: PlanUiState) {
    val target = s.targetKalori.coerceAtLeast(1)
    val sisa = if (s.totalKalori <= s.targetKalori)
        "Sisa ${Formatter.kkal(s.targetKalori - s.totalKalori)}"
    else "Melebihi ${Formatter.kkal(s.totalKalori - s.targetKalori)}"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GreenDark)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total kalori harian", color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                Text(sisa, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Formatter.angka(s.totalKalori), style = BigNumberStyle, color = Color.White)
                Text(
                    " / ${Formatter.kkal(s.targetKalori)}",
                    style = UnitStyle, color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                "Rencana hari ini: ${Formatter.kkal(s.rencanaKalori)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            LinearProgressIndicator(
                progress = { (s.totalKalori.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = GreenLight,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Row(Modifier.fillMaxWidth()) {
                Makro("${s.protein} g", "Protein", Modifier.weight(1f))
                Makro("${s.karbo} g", "Karbohidrat", Modifier.weight(1f))
                Makro("${s.lemak} g", "Lemak", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Makro(nilai: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(nilai, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BudgetCard(s: PlanUiState) {
    val anggaran = s.anggaran.coerceAtLeast(1)
    val melebihi = s.totalBiaya > s.anggaran
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OrangeSoft)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Anggaran Makan Hari ini", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Formatter.rupiah(s.totalBiaya),
                    style = MaterialTheme.typography.headlineMedium, color = OrangeBar,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Target anggaran harian ${Formatter.rupiah(s.anggaran)}",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
            }
            Text(
                "Rencana hari ini: ${Formatter.rupiah(s.rencanaBiaya)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            LinearProgressIndicator(
                progress = { (s.totalBiaya.toFloat() / anggaran).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = OrangeBar,
                trackColor = Color.White
            )
            if (melebihi) {
                Text(
                    "Melebihi anggaran ${Formatter.rupiahRapat(s.totalBiaya - s.anggaran)}",
                    color = RedError, fontWeight = FontWeight.Bold
                )
            } else {
                Text("Sisa ${Formatter.rupiahRapat(s.anggaran - s.totalBiaya)}", color = TextMuted)
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: SlotUi,
    bacaSaja: Boolean,
    onTambah: () -> Unit,
    onGanti: () -> Unit,
    onDetail: (Long) -> Unit,
    onTandai: (Long, Boolean) -> Unit
) {
    val item = slot.item
    val menu = slot.menu
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${slot.waktu.label} ${slot.waktu.jam}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (item != null && menu != null) {
                    if (!bacaSaja) {
                        TombolSudahMakan(
                            sudah = item.sudahDimakan,
                            onClick = { onTandai(item.id, !item.sudahDimakan) }
                        )
                    } else if (item.sudahDimakan) {
                        Text("✓ Sudah dimakan", color = GreenDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (item != null && menu != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MenuPhoto(menu.fotoRes, Modifier.size(76.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(menu.nama, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Formatter.kkalHarga(item.kaloriSnapshot, item.hargaSnapshot),
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted
                        )
                        Row {
                            TextButton(onClick = { onDetail(item.id) }, contentPadding = PaddingValues(end = 12.dp)) {
                                Text("Detail →", color = GreenDark, fontWeight = FontWeight.SemiBold)
                            }
                            if (!bacaSaja) {
                                TextButton(onClick = onGanti, contentPadding = PaddingValues(end = 12.dp)) {
                                    Text("Ganti Menu →", color = GreenDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else if (bacaSaja) {
                Text("Tidak ada menu di slot ini.", color = TextMuted)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(GreenLight).clickable { onTambah() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, contentDescription = "Tambah makanan", tint = GreenDark) }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Belum ada ${slot.waktu.label.lowercase()}. Yuk, tambahkan makanan sehat!",
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TombolSudahMakan(sudah: Boolean, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
    if (sudah) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, GreenDark),
            contentPadding = padding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDark)
        ) {
            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Sudah dimakan", fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Sudah makan", fontWeight = FontWeight.SemiBold)
        }
    }
}