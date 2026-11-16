package com.example.nutriplan.ui.preferensi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
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
import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.Tujuan
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.domain.MealPlanGenerator
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.OrangeBar
import com.example.nutriplan.ui.theme.OrangeSoft
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

// ---------------- State & ViewModel ----------------

data class PrefUiState(
    val tujuan: Tujuan = Tujuan.JAGA,
    val pola: Set<String> = emptySet(),
    val alergi: Set<String> = emptySet(),
    val anggaran: Int = 50_000,
    val targetKalori: Int = 1800,
    val porsi: Int = 1,
    val durasi: Int = 7,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val pesanError: String? = null
)

private fun PreferenceEntity.toUi() = PrefUiState(
    tujuan = tujuan, pola = polaMakan.toSet(), alergi = alergi.toSet(),
    anggaran = anggaranHarian, targetKalori = targetKaloriHarian,
    porsi = porsi, durasi = durasiHari, loading = false
)

class PreferenceViewModel(
    private val prefRepo: PreferenceRepository,
    private val menuRepo: MenuRepository,
    private val planRepo: MealPlanRepository,
    private val userId: Long
) : ViewModel() {

    private val _ui = MutableStateFlow(PrefUiState())
    val ui: StateFlow<PrefUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val p = prefRepo.get(userId) ?: PreferenceEntity(userId = userId)
            _ui.value = p.toUi()
        }
    }

    fun setTujuan(t: Tujuan) { _ui.update { it.copy(tujuan = t) } }
    fun togglePola(p: String) {
        _ui.update { it.copy(pola = if (p in it.pola) it.pola - p else it.pola + p) }
    }
    fun toggleAlergi(a: String) {
        _ui.update { it.copy(alergi = if (a in it.alergi) it.alergi - a else it.alergi + a) }
    }
    fun kosongkanAlergi() { _ui.update { it.copy(alergi = emptySet()) } }
    fun setAnggaran(v: Int) { _ui.update { it.copy(anggaran = v) } }
    fun setPorsi(v: Int) { _ui.update { it.copy(porsi = v.coerceIn(1, 4)) } }
    fun setDurasi(v: Int) { _ui.update { it.copy(durasi = v.coerceIn(3, 7)) } }

    /** Simpan preferensi, lalu susun rencana. Jika mustahil, tampil pesan (bukan crash). */
    fun simpan(onSuccess: () -> Unit) {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, pesanError = null) }
            val pref = PreferenceEntity(
                userId = userId, tujuan = s.tujuan, polaMakan = s.pola.toList(),
                alergi = s.alergi.toList(), anggaranHarian = s.anggaran,
                targetKaloriHarian = s.targetKalori, porsi = s.porsi, durasiHari = s.durasi
            )
            prefRepo.save(pref)

            val hasil = MealPlanGenerator().generate(pref, menuRepo.getAll())
            val plan = hasil.getOrNull()
            if (plan != null) {
                planRepo.createPlan(userId, namaRencana(pref), LocalDate.now(), plan)
                _ui.update { it.copy(saving = false) }
                onSuccess()
            } else {
                val pesan = hasil.exceptionOrNull()?.message
                    ?: "Rencana belum bisa disusun. Coba ubah preferensi."
                _ui.update { it.copy(saving = false, pesanError = pesan) }
            }
        }
    }

    private fun namaRencana(p: PreferenceEntity): String =
        if (p.polaMakan.isEmpty()) "Rencana seimbang"
        else p.polaMakan.mapIndexed { i, s ->
            if (i == 0) s else s.replaceFirstChar { c -> c.lowercase() }
        }.joinToString(", ")
}

// ---------------- Screen (stateful) ----------------

class PrefActions(
    val onTujuan: (Tujuan) -> Unit,
    val onPola: (String) -> Unit,
    val onAlergi: (String) -> Unit,
    val onAlergiKosong: () -> Unit,
    val onAnggaran: (Int) -> Unit,
    val onPorsi: (Int) -> Unit,
    val onDurasi: (Int) -> Unit,
    val onSimpan: () -> Unit
)

@Composable
fun PreferensiScreen(onPlanCreated: () -> Unit) {
    val vm = nutriViewModel { c ->
        PreferenceViewModel(c.preferenceRepository, c.menuRepository, c.mealPlanRepository, Session.userId)
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    PreferensiContent(
        state = state,
        actions = PrefActions(
            onTujuan = vm::setTujuan,
            onPola = vm::togglePola,
            onAlergi = vm::toggleAlergi,
            onAlergiKosong = vm::kosongkanAlergi,
            onAnggaran = vm::setAnggaran,
            onPorsi = vm::setPorsi,
            onDurasi = vm::setDurasi,
            onSimpan = { vm.simpan(onPlanCreated) }
        )
    )
}

// ---------------- Content (stateless) ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferensiContent(state: PrefUiState, actions: PrefActions) {
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Preferensi makan", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Rencana yang pas dimulai dari kebutuhanmu.",
                style = MaterialTheme.typography.bodyMedium, color = TextMuted
            )
        }

        Text("Tujuanmu", style = MaterialTheme.typography.titleMedium)
        SegmentedTujuan(state.tujuan, actions.onTujuan)

        Text("Pola makan", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pola.semua.forEach { p ->
                val sel = p in state.pola
                FilterChip(
                    selected = sel,
                    onClick = { actions.onPola(p) },
                    label = { Text(p) },
                    leadingIcon = if (sel) {
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
        }

        DropdownField(
            label = "Alergi / bahan yang dihindari",
            value = if (state.alergi.isEmpty()) "Tidak ada" else state.alergi.joinToString(", ")
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text("Tidak ada") },
                onClick = { actions.onAlergiKosong(); dismiss() }
            )
            Alergen.semua.forEach { a ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = a in state.alergi, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(a)
                        }
                    },
                    onClick = { actions.onAlergi(a) }
                )
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Anggaran per hari", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    Formatter.rupiah(state.anggaran),
                    color = OrangeBar, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Slider(
                value = state.anggaran.toFloat(),
                onValueChange = { actions.onAnggaran(it.roundToInt()) },
                valueRange = 20_000f..100_000f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeBar, activeTrackColor = OrangeBar,
                    inactiveTrackColor = OrangeSoft
                )
            )
            Row {
                Text(Formatter.rupiah(20_000), style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f))
                Text(Formatter.rupiah(100_000), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                Modifier.weight(1f).height(104.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenLight)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Target harian", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            Formatter.angka(state.targetKalori),
                            style = MaterialTheme.typography.headlineMedium, color = GreenDark
                        )
                        Text(
                            " kkal", style = MaterialTheme.typography.bodyMedium,
                            color = GreenDark, modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
            Card(
                Modifier.weight(1f).height(104.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Porsi per menu", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { actions.onPorsi(state.porsi - 1) },
                            enabled = state.porsi > 1,
                            modifier = Modifier.size(32.dp)
                        ) { Icon(Icons.Filled.Remove, "Kurangi porsi") }
                        Text(
                            "${state.porsi} orang",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = { actions.onPorsi(state.porsi + 1) },
                            enabled = state.porsi < 4,
                            modifier = Modifier.size(32.dp)
                        ) { Icon(Icons.Filled.Add, "Tambah porsi") }
                    }
                }
            }
        }

        DropdownField(
            label = "Durasi rencana",
            value = "${state.durasi} hari • 3 kali makan sehari"
        ) { dismiss ->
            (3..7).forEach { n ->
                DropdownMenuItem(
                    text = { Text("$n hari • 3 kali makan sehari") },
                    onClick = { actions.onDurasi(n); dismiss() }
                )
            }
        }

        state.pesanError?.let { InfoBanner(it, type = BannerType.ERROR) }

        NutriButton(
            text = if (state.saving) "Menyusun rencana..." else "Simpan preferensi",
            onClick = actions.onSimpan,
            enabled = !state.saving,
            icon = Icons.Filled.Check
        )
        Text(
            "Biaya adalah estimasi per porsi.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted
        )
    }
}

@Composable
private fun SegmentedTujuan(selected: Tujuan, onSelect: (Tujuan) -> Unit) {
    val opsi = listOf(
        Tujuan.JAGA to "Jaga berat",
        Tujuan.TURUN to "Turun berat",
        Tujuan.NAIK to "Naik berat"
    )
    val bentuk = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(bentuk)
            .background(Color.White)
            .border(1.dp, GreenLight, bentuk)
            .padding(4.dp)
    ) {
        opsi.forEach { (t, label) ->
            val aktif = t == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(bentuk)
                    .background(if (aktif) GreenLight else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (aktif) FontWeight.Bold else FontWeight.Normal,
                    color = if (aktif) GreenDark else TextMuted
                )
            }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    menu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD0D0D0))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { open = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                menu { open = false }
            }
        }
    }
}