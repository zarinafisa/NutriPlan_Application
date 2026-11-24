package com.example.nutriplan.ui.tambah

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------- State & ViewModel ----------------

data class TambahUiState(
    val waktu: WaktuMakan,
    val query: String = "",
    val daftar: List<MenuEntity> = emptyList()
)

class TambahMakananViewModel(
    menuRepo: MenuRepository,
    private val planRepo: MealPlanRepository,
    private val userId: Long,
    private val hari: Int,
    private val waktuAwal: WaktuMakan,
    private val ganti: Boolean
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val waktu = MutableStateFlow(waktuAwal)

    val ui: StateFlow<TambahUiState> = combine(menuRepo.observeAll(), query, waktu) { menus, q, w ->
        TambahUiState(
            waktu = w,
            query = q,
            daftar = menus.filter {
                it.dataComplete && it.waktuMakan == w && it.nama.contains(q.trim(), ignoreCase = true)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TambahUiState(waktuAwal))

    fun setQuery(q: String) { query.value = q }
    fun setWaktu(w: WaktuMakan) { waktu.value = w }

    /**
     * Mode tambah: menu masuk ke slot sesuai chip yang dipilih.
     * Mode ganti: menu selalu menimpa slot asal; chip hanya untuk menjelajah kategori.
     * Menu yang diganti otomatis jadi "belum dimakan", jadi tidak terhitung ganda.
     */
    fun tambah(menu: MenuEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val plan = planRepo.getActivePlan(userId)
            val slot = if (ganti) waktuAwal else waktu.value
            if (plan != null) planRepo.setItem(plan.id, hari, slot, menu)
            onDone()
        }
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun TambahMakananScreen(
    hari: Int,
    waktuAwal: WaktuMakan,
    ganti: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm = nutriViewModel(key = "tambah-$hari-${waktuAwal.name}-$ganti") { c ->
        TambahMakananViewModel(
            c.menuRepository, c.mealPlanRepository, Session.userId, hari, waktuAwal, ganti
        )
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    TambahMakananContent(
        state = state,
        ganti = ganti,
        onBack = onBack,
        onQuery = vm::setQuery,
        onWaktu = vm::setWaktu,
        onPilih = { menu ->
            vm.tambah(menu) {
                Toast.makeText(
                    context,
                    if (ganti) "Menu diganti." else "Makanan ditambahkan.",
                    Toast.LENGTH_SHORT
                ).show()
                onBack()
            }
        }
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun TambahMakananContent(
    state: TambahUiState,
    ganti: Boolean,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onWaktu: (WaktuMakan) -> Unit,
    onPilih: (MenuEntity) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenHeader(if (ganti) "Ganti Menu" else "Tambah Makanan", onBack = onBack)
                Text(
                    if (ganti) "Pilih menu pengganti untuk jadwal ini"
                    else "Pilih makanan yang ingin kamu tambahkan ke rencana makan",
                    color = TextMuted, style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari makanan...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(WaktuMakan.SARAPAN, WaktuMakan.SIANG, WaktuMakan.CAMILAN, WaktuMakan.MALAM).forEach { w ->
                        FilterChip(
                            selected = w == state.waktu,
                            onClick = { onWaktu(w) },
                            label = { Text(w.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenLight,
                                selectedLabelColor = GreenDark
                            )
                        )
                    }
                }
                Text("Rekomendasi", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (state.daftar.isEmpty()) {
            item { Text("Menu tidak ditemukan.", color = TextMuted) }
        }

        items(state.daftar, key = { it.id }) { menu ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MenuPhoto(menu.fotoRes, Modifier.size(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(menu.nama, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Formatter.kkalHarga(menu.kalori, menu.harga),
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted
                        )
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(GreenDark).clickable { onPilih(menu) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, contentDescription = "Tambahkan ${menu.nama}", tint = Color.White) }
                }
            }
        }

        item {
            Text(
                "Biaya adalah estimasi per porsi.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
        }
    }
}