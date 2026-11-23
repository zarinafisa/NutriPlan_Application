package com.example.nutriplan.ui.dasbor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nutriplan.data.remote.InspirasiResep
import com.example.nutriplan.data.remote.MealDbRepository
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------- State & ViewModel ----------------

sealed interface InspirasiState {
    data object Loading : InspirasiState
    data class Sukses(val resep: InspirasiResep) : InspirasiState
    data class Error(val pesan: String) : InspirasiState
}

class InspirasiViewModel(
    private val repo: MealDbRepository = MealDbRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<InspirasiState>(InspirasiState.Loading)
    val state: StateFlow<InspirasiState> = _state.asStateFlow()

    init { muat() }

    fun muat() {
        _state.value = InspirasiState.Loading
        viewModelScope.launch {
            repo.resepAcak().fold(
                onSuccess = { _state.value = InspirasiState.Sukses(it) },
                onFailure = {
                    _state.value = InspirasiState.Error(
                        "Tidak ada koneksi internet. Bagian lain aplikasi tetap bisa dipakai."
                    )
                }
            )
        }
    }
}

// ---------------- Tampilan ----------------

@Composable
fun InspirasiSection() {
    val vm: InspirasiViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Inspirasi resep hari ini", style = MaterialTheme.typography.titleLarge)

        when (val s = state) {
            InspirasiState.Loading -> {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenDark)
                }
            }
            is InspirasiState.Error -> {
                InfoBanner(s.pesan, type = BannerType.WARNING)
                NutriButton("Coba lagi", onClick = vm::muat, outline = true)
            }
            is InspirasiState.Sukses -> KartuResep(s.resep, onLain = vm::muat)
        }
    }
}

@Composable
private fun KartuResep(r: InspirasiResep, onLain: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = r.fotoUrl,
                contentDescription = r.nama,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Text(r.nama, style = MaterialTheme.typography.titleLarge)
            Text(
                listOf(r.kategori, r.daerah).filter { it.isNotBlank() }.joinToString(" • "),
                color = GreenDark, style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Bahan: " + r.bahan.take(5).joinToString(", ") +
                        if (r.bahan.size > 5) ", dan ${r.bahan.size - 5} lainnya" else "",
                color = TextMuted, style = MaterialTheme.typography.bodyMedium
            )
            NutriButton("Resep lain", onClick = onLain, outline = true)
            Text(
                "Sumber: TheMealDB. Hanya inspirasi, tidak dihitung dalam kalori dan anggaran.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
        }
    }
}