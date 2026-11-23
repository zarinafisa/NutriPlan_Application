package com.example.nutriplan.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.repository.AuthRepository
import com.example.nutriplan.domain.Validator
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.NutriTextField
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.RedError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- State & ViewModel ----------------

data class DaftarUiState(
    val nama: String = "",
    val email: String = "",
    val sandi: String = "",
    val konfirmasi: String = "",
    val setuju: Boolean = false,
    val errNama: String? = null,
    val errEmail: String? = null,
    val errSandi: String? = null,
    val errKonfirmasi: String? = null,
    val errSetuju: String? = null,
    val loading: Boolean = false
)

class DaftarViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(DaftarUiState())
    val ui: StateFlow<DaftarUiState> = _ui.asStateFlow()

    fun onNama(v: String) { _ui.update { it.copy(nama = v, errNama = null) } }
    fun onEmail(v: String) { _ui.update { it.copy(email = v, errEmail = null) } }
    fun onSandi(v: String) { _ui.update { it.copy(sandi = v, errSandi = null) } }
    fun onKonfirmasi(v: String) { _ui.update { it.copy(konfirmasi = v, errKonfirmasi = null) } }
    fun onSetuju(v: Boolean) { _ui.update { it.copy(setuju = v, errSetuju = null) } }

    fun daftar(onSuccess: () -> Unit) {
        val s = _ui.value
        if (s.loading) return
        val eNama = Validator.nama(s.nama)
        val eEmail = Validator.email(s.email)
        val eSandi = Validator.sandi(s.sandi)
        val eKonf = Validator.konfirmasi(s.sandi, s.konfirmasi)
        val eSetuju = if (s.setuju) null else "Centang persetujuan untuk melanjutkan."

        if (listOf(eNama, eEmail, eSandi, eKonf, eSetuju).any { it != null }) {
            _ui.update {
                it.copy(
                    errNama = eNama, errEmail = eEmail, errSandi = eSandi,
                    errKonfirmasi = eKonf, errSetuju = eSetuju
                )
            }
            return   // form tetap terisi
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            auth.register(s.nama, s.email, s.sandi).fold(
                onSuccess = {
                    _ui.update { it.copy(loading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _ui.update { it.copy(loading = false, errEmail = e.message) }
                }
            )
        }
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun DaftarScreen(onBack: (() -> Unit)?, onSelesai: () -> Unit, onMasuk: () -> Unit) {
    val vm = nutriViewModel { c -> DaftarViewModel(c.authRepository) }
    val s by vm.ui.collectAsStateWithLifecycle()
    DaftarContent(
        s = s, onBack = onBack,
        onNama = vm::onNama, onEmail = vm::onEmail, onSandi = vm::onSandi,
        onKonfirmasi = vm::onKonfirmasi, onSetuju = vm::onSetuju,
        onDaftar = { vm.daftar(onSelesai) }, onMasuk = onMasuk
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun DaftarContent(
    s: DaftarUiState,
    onBack: (() -> Unit)?,
    onNama: (String) -> Unit,
    onEmail: (String) -> Unit,
    onSandi: (String) -> Unit,
    onKonfirmasi: (String) -> Unit,
    onSetuju: (Boolean) -> Unit,
    onDaftar: () -> Unit,
    onMasuk: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Daftar akun", onBack = onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GreenDark),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Restaurant, null, tint = Color.White) }
            Spacer(Modifier.width(10.dp))
            Text("NutriPlan", style = MaterialTheme.typography.titleLarge, color = GreenDark)
        }

        Text("Langkah kecil, hidup sehat.", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Buat akun untuk menyimpan rencana makan yang sesuai denganmu.",
            color = TextMuted
        )

        NutriTextField(s.nama, onNama, "Nama lengkap", error = s.errNama)
        NutriTextField(
            s.email, onEmail, "Email", error = s.errEmail,
            keyboardType = KeyboardType.Email, leadingIcon = Icons.Filled.Email
        )
        NutriTextField(
            s.sandi, onSandi, "Kata sandi", error = s.errSandi,
            helper = "Minimal 8 karakter, huruf dan angka.", isPassword = true
        )
        NutriTextField(
            s.konfirmasi, onKonfirmasi, "Konfirmasi kata sandi",
            error = s.errKonfirmasi, isPassword = true
        )

        Column {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = s.setuju, onCheckedChange = onSetuju,
                    colors = CheckboxDefaults.colors(checkedColor = GreenDark)
                )
                Text(
                    "Saya menyetujui Syarat penggunaan dan Kebijakan privasi NutriPlan.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            s.errSetuju?.let {
                Text(it, color = RedError, style = MaterialTheme.typography.bodySmall)
            }
        }

        NutriButton(
            text = if (s.loading) "Membuat akun..." else "Buat akun",
            onClick = onDaftar,
            enabled = !s.loading
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Sudah punya akun? ", color = TextMuted)
            Text(
                "Masuk", color = GreenDark, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onMasuk)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}