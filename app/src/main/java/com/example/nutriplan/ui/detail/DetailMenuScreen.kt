package com.example.nutriplan.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.PriceAmber
import com.example.nutriplan.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------- State & ViewModel ----------------

data class DetailUiState(
    val loading: Boolean = true,
    val item: MealPlanItemEntity? = null,
    val menu: MenuEntity? = null
)

class DetailMenuViewModel(
    private val planRepo: MealPlanRepository,
    private val menuRepo: MenuRepository,
    private val itemId: Long
) : ViewModel() {

    private val _ui = MutableStateFlow(DetailUiState())
    val ui: StateFlow<DetailUiState> = _ui.asStateFlow()

    init { viewModelScope.launch { muat() } }

    private suspend fun muat() {
        val item = planRepo.getItem(itemId)
        val menu = item?.let { menuRepo.getById(it.menuId) }
        _ui.value = DetailUiState(loading = false, item = item, menu = menu)
    }

    /** Logika lengkap penandaan (peringatan 90%) dikerjakan PRD-3 di Tahap 6. */
    fun setEaten(eaten: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            planRepo.markEaten(itemId, eaten)
            muat()
            onDone()
        }
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun DetailMenuScreen(itemId: Long, onBack: () -> Unit) {
    val vm = nutriViewModel(key = "detail-$itemId") { c ->
        DetailMenuViewModel(c.mealPlanRepository, c.menuRepository, itemId)
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    DetailMenuContent(
        state = state,
        onBack = onBack,
        onTandai = { vm.setEaten(true, onDone = onBack) },
        onBatal = { vm.setEaten(false) }
    )
}

// ---------------- Content (stateless) ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailMenuContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onTandai: () -> Unit,
    onBatal: () -> Unit
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }
    val item = state.item
    val menu = state.menu
    if (item == null || menu == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader("Detail menu", onBack = onBack)
            Text("Menu tidak ditemukan.", color = TextMuted, modifier = Modifier.padding(16.dp))
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("Detail menu", onBack = onBack)

        Box(Modifier.fillMaxWidth().height(200.dp)) {
            MenuPhoto(menu.fotoRes, Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(50),
                color = GreenDark
            ) {
                Text(
                    "${item.waktuMakan.label.uppercase()} • HARI ${item.hari}",
                    color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (!item.sudahDimakan) {
            NutriButton("Tandai sudah dimakan", onClick = onTandai, icon = Icons.Filled.Check)
        } else {
            NutriButton("Sudah dimakan • Batalkan", onClick = onBatal, outline = true, icon = Icons.Filled.Check)
        }

        Text(menu.nama, style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = TextMuted)
                Spacer(Modifier.width(4.dp))
                Text("${menu.waktuMasakMenit} menit", color = TextMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, Modifier.size(16.dp), tint = TextMuted)
                Spacer(Modifier.width(4.dp))
                Text("${menu.porsi} porsi", color = TextMuted)
            }
            Text(Formatter.rupiah(item.hargaSnapshot), color = PriceAmber, fontWeight = FontWeight.Bold)
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
                Gizi("${item.kaloriSnapshot}", "kkal", "Energi", Modifier.weight(1f))
                Gizi("${menu.protein}", "g", "Protein", Modifier.weight(1f))
                Gizi("${menu.karbo}", "g", "Karbo", Modifier.weight(1f))
                Gizi("${menu.lemak}", "g", "Lemak", Modifier.weight(1f))
            }
        }

        Text("Bahan-bahan", style = MaterialTheme.typography.titleLarge)
        val separuh = (menu.bahan.size + 1) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                menu.bahan.take(separuh).forEach { Text("• $it") }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                menu.bahan.drop(separuh).forEach { Text("• $it") }
            }
        }

        Text("Cara membuat", style = MaterialTheme.typography.titleLarge)
        menu.langkah.forEachIndexed { i, langkah ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(GreenLight),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", color = GreenDark, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Text(langkah, Modifier.weight(1f))
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            menu.tagPola.forEach { tag ->
                Surface(shape = RoundedCornerShape(50), color = GreenLight) {
                    Text(tag, color = GreenDark, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        if (menu.alergen.isNotEmpty()) {
            Text("Mengandung: ${menu.alergen.joinToString(", ")}", color = TextMuted,
                style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Biaya adalah estimasi per porsi. Harga bahan dapat berbeda di setiap daerah.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Gizi(nilai: String, satuan: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(nilai, style = MaterialTheme.typography.titleLarge, color = GreenDark)
            Text(" $satuan", style = MaterialTheme.typography.bodySmall, color = GreenDark)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}