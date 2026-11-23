package com.example.nutriplan.ui.dasbor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
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
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.data.repository.ProfileRepository
import com.example.nutriplan.domain.ItemProgres
import com.example.nutriplan.domain.Level
import com.example.nutriplan.domain.ProgresHarian
import com.example.nutriplan.domain.ProgressTracker
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.SectionHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.BigNumberStyle
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.OrangeBar
import com.example.nutriplan.ui.theme.OrangeSoft
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---------------- State & ViewModel ----------------

data class MenuHariUi(val item: MealPlanItemEntity, val menu: MenuEntity)

data class Peringatan(val level: Level, val teks: String)

data class DasborUiState(
    val loading: Boolean = true,
    val ada: Boolean = false,
    val namaDepan: String = "",
    val inisial: String = "",
    val tanggalTeks: String = "",
    val progres: ProgresHarian? = null,
    val peringatan: List<Peringatan> = emptyList(),
    val berikutnya: MenuHariUi? = null,
    val menuLain: List<MenuHariUi> = emptyList(),
    val durasi: Int = 0,
    val rataKalori: Int = 0,
    val totalBiaya: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    planRepo: MealPlanRepository,
    menuRepo: MenuRepository,
    prefRepo: PreferenceRepository,
    profileRepo: ProfileRepository,
    userId: Long
) : ViewModel() {

    private val namaFlow = MutableStateFlow("")
    private val planFlow = planRepo.observeActivePlan(userId)
    private val itemsFlow = planFlow.flatMapLatest { p ->
        if (p == null) flowOf(emptyList<MealPlanItemEntity>()) else planRepo.observeItems(p.id)
    }
    private val prefNama = combine(prefRepo.observe(userId), namaFlow) { p, n -> p to n }

    val ui: StateFlow<DasborUiState> = combine(
        planFlow, itemsFlow, menuRepo.observeAll(), prefNama
    ) { plan, items, menus, pn ->
        bangun(plan, items, menus, pn.first, pn.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DasborUiState())

    init {
        viewModelScope.launch { namaFlow.value = profileRepo.getUser(userId)?.nama.orEmpty() }
    }

    private fun bangun(
        plan: MealPlanEntity?,
        items: List<MealPlanItemEntity>,
        menus: List<MenuEntity>,
        pref: PreferenceEntity?,
        nama: String
    ): DasborUiState {
        val namaDepan = nama.trim().substringBefore(" ")
        val inisial = nama.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
        if (plan == null) {
            return DasborUiState(loading = false, ada = false, namaDepan = namaDepan, inisial = inisial)
        }

        val mulai = LocalDate.parse(plan.tanggalMulai)
        val hariIni = (ChronoUnit.DAYS.between(mulai, LocalDate.now()) + 1).toInt().coerceIn(1, plan.durasi)
        val peta = menus.associateBy { it.id }
        val itemHari = items.filter { it.hari == hariIni }.sortedBy { it.waktuMakan.ordinal }
        val daftar = itemHari.mapNotNull { i -> peta[i.menuId]?.let { MenuHariUi(i, it) } }

        val target = pref?.targetKaloriHarian ?: 1800
        val anggaran = pref?.anggaranHarian ?: 50_000
        val progres = ProgressTracker.hitung(
            itemHari.map { ItemProgres(it.kaloriSnapshot, it.hargaSnapshot, it.sudahDimakan) },
            target, anggaran
        )

        val berikutnya = daftar.firstOrNull { !it.item.sudahDimakan }
        val menuLain = daftar.filter { it.item.id != berikutnya?.item?.id }

        val peringatan = buildList {
            when (progres.levelKalori) {
                Level.HAMPIR -> add(
                    Peringatan(Level.HAMPIR, "Kalori hari ini hampir mencapai target (${progres.persenKalori}%).")
                )
                Level.MELEWATI -> add(
                    Peringatan(Level.MELEWATI, "Melebihi target ${Formatter.kkal(-progres.sisaKalori)}.")
                )
                Level.AMAN -> Unit
            }
            when (progres.levelBiaya) {
                Level.HAMPIR -> add(
                    Peringatan(Level.HAMPIR, "Biaya hari ini hampir mencapai anggaran (${progres.persenBiaya}%).")
                )
                Level.MELEWATI -> add(
                    Peringatan(Level.MELEWATI, "Melebihi anggaran ${Formatter.rupiahRapat(-progres.sisaAnggaran)}")
                )
                Level.AMAN -> Unit
            }
        }

        val formatHari = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id-ID"))
        return DasborUiState(
            loading = false,
            ada = true,
            namaDepan = namaDepan,
            inisial = inisial,
            tanggalTeks = "${LocalDate.now().format(formatHari)} • Hari $hariIni",
            progres = progres,
            peringatan = peringatan,
            berikutnya = berikutnya,
            menuLain = menuLain,
            durasi = plan.durasi,
            rataKalori = items.sumOf { it.kaloriSnapshot } / plan.durasi.coerceAtLeast(1),
            totalBiaya = items.sumOf { it.hargaSnapshot }
        )
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun DasborScreen(
    onLihatRencana: () -> Unit,
    onDetail: (Long) -> Unit,
    onSusunRencana: () -> Unit
) {
    val context = LocalContext.current
    val vm = nutriViewModel(key = "dasbor-${Session.userId}") { c ->
        DashboardViewModel(
            c.mealPlanRepository, c.menuRepository, c.preferenceRepository,
            c.profileRepository, Session.userId
        )
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    DasborContent(
        state = state,
        onLihatRencana = onLihatRencana,
        onDetail = onDetail,
        onSusunRencana = onSusunRencana,
        onLonceng = {
            Toast.makeText(context, "Pengingat dibuat di tahap polesan.", Toast.LENGTH_SHORT).show()
        }
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun DasborContent(
    state: DasborUiState,
    onLihatRencana: () -> Unit,
    onDetail: (Long) -> Unit,
    onSusunRencana: () -> Unit,
    onLonceng: () -> Unit
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dasbor", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onLonceng) { Icon(Icons.Filled.Notifications, "Pengingat") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Halo, ${state.namaDepan}! 👋", style = MaterialTheme.typography.headlineMedium)
                if (state.ada) Text(state.tanggalTeks, color = TextMuted)
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(GreenLight),
                contentAlignment = Alignment.Center
            ) { Text(state.inisial, color = GreenDark, fontWeight = FontWeight.Bold) }
        }

        val p = state.progres
        if (!state.ada || p == null) {
            Spacer(Modifier.height(16.dp))
            Text("Belum ada rencana aktif.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Susun rencana makan baru dari preferensimu. Rencana lama ada di Riwayat.",
                color = TextMuted
            )
            NutriButton("Susun rencana", onClick = onSusunRencana)
            return@Column
        }

        KartuProgres(p)

        state.peringatan.forEach {
            InfoBanner(
                it.teks,
                type = if (it.level == Level.MELEWATI) BannerType.ERROR else BannerType.WARNING
            )
        }
        if (p.hariSelesai) {
            InfoBanner("Semua menu hari ini sudah dimakan. Kerja bagus!", type = BannerType.INFO)
        }

        Card(
            onClick = onLihatRencana,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = OrangeSoft)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.CalendarMonth, null, tint = OrangeBar) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rencana makan ${state.durasi} hari", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Rata-rata ${Formatter.kkal(state.rataKalori)} • ${Formatter.rupiah(state.totalBiaya)}",
                        style = MaterialTheme.typography.bodyMedium, color = TextMuted
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
            }
        }

        SectionHeader("Menu hari ini", actionText = "Lihat semua", onAction = onLihatRencana)

        state.berikutnya?.let { next ->
            Card(
                onClick = { onDetail(next.item.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(150.dp)) {
                        MenuPhoto(
                            next.menu.fotoRes, Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            shape = RoundedCornerShape(50), color = GreenDark
                        ) {
                            Text(
                                "${next.item.waktuMakan.label.uppercase()} • ${next.item.waktuMakan.jam}",
                                color = Color.White, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(next.menu.nama, style = MaterialTheme.typography.titleLarge)
                        Text(
                            Formatter.kkalHarga(next.item.kaloriSnapshot, next.item.hargaSnapshot),
                            color = TextMuted
                        )
                        Text("Detail →", color = GreenDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        state.menuLain.forEach { m ->
            Card(
                onClick = { onDetail(m.item.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MenuPhoto(m.menu.fotoRes, Modifier.size(56.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${m.item.waktuMakan.label.uppercase()} • ${m.item.waktuMakan.jam}",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted
                        )
                        Text(m.menu.nama, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Formatter.kkalHarga(m.item.kaloriSnapshot, m.item.hargaSnapshot),
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted
                        )
                    }
                    if (m.item.sudahDimakan) {
                        Icon(Icons.Filled.CheckCircle, "Sudah dimakan", tint = GreenDark)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
                    }
                }
            }
        }

        InspirasiSection()
        Text("Biaya adalah estimasi per porsi.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KartuProgres(p: ProgresHarian) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GreenDark)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kalori menu hari ini", color = Color.White.copy(alpha = 0.85f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(Formatter.angka(p.konsumsiKalori), style = BigNumberStyle, color = Color.White)
                    Text(
                        " / ${Formatter.kkal(p.targetKalori)}",
                        style = UnitStyle, color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.LocalFireDepartment, null, tint = OrangeBar) }
            }
            Text(
                "Rencana hari ini: ${Formatter.kkal(p.rencanaKalori)}",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)
            )
            LinearProgressIndicator(
                progress = { (p.konsumsiKalori.toFloat() / p.targetKalori.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = GreenLight,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Biaya menu hari ini", color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${Formatter.rupiah(p.konsumsiBiaya)} / ${Formatter.angka(p.anggaran)}",
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { (p.konsumsiBiaya.toFloat() / p.anggaran.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = OrangeBar,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Text(
                "Rencana hari ini: ${Formatter.rupiah(p.rencanaBiaya)}",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}