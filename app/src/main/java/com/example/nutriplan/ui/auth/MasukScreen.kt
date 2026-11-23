package com.example.nutriplan.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.nutriplan.data.repository.AuthRepository
import com.example.nutriplan.data.repository.ProfileRepository
import com.example.nutriplan.domain.Validator
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.BannerType
import com.example.nutriplan.ui.components.InfoBanner
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.NutriTextField
import com.example.nutriplan.ui.components.ScreenHeader
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- State & ViewModel ----------------

data class MasukUiState(
    val email: String = "",
    val sandi: String = "",
    val errEmail: String? = null,
    val errSandi: String? = null,
    val pesanError: String? = null,
    val loading: Boolean = false
)

class MasukViewModel(
    private val auth: AuthRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MasukUiState())
    val ui: StateFlow<MasukUiState> = _ui.asStateFlow()

    fun onEmail(v: String) { _ui.update { it.copy(email = v, errEmail = null, pesanError = null) } }
    fun onSandi(v: String) { _ui.update { it.copy(sandi = v, errSandi = null, pesanError = null) } }

    /** onSukses(adaProfil): false berarti akun baru yang belum mengisi Data diri. */
    fun masuk(onSukses: (Boolean) -> Unit) {
        val s = _ui.value
        if (s.loading) return
        val eEmail = Validator.email(s.email)
        val eSandi = if (s.sandi.isEmpty()) "Kata sandi wajib diisi." else null
        if (eEmail != null || eSandi != null) {
            _ui.update { it.copy(errEmail = eEmail, errSandi = eSandi) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, pesanError = null) }
            auth.login(s.email, s.sandi).fold(
                onSuccess = { user ->
                    val adaProfil = profileRepo.get(user.id) != null
                    _ui.update { it.copy(loading = false) }
                    onSukses(adaProfil)
                },
                onFailure = { e ->
                    _ui.update { it.copy(loading = false, pesanError = e.message) }
                }
            )
        }
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun MasukScreen(onBack: (() -> Unit)?, onSukses: (Boolean) -> Unit, onDaftar: () -> Unit) {
    val context = LocalContext.current
    val vm = nutriViewModel { c -> MasukViewModel(c.authRepository, c.profileRepository) }
    val s by vm.ui.collectAsStateWithLifecycle()
    MasukContent(
        s = s,
        onBack = onBack,
        onEmail = vm::onEmail,
        onSandi = vm::onSandi,
        onMasuk = { vm.masuk(onSukses) },
        onLupa = { Toast.makeText(context, "Lupa kata sandi: segera hadir.", Toast.LENGTH_SHORT).show() },
        onGoogle = { Toast.makeText(context, "Masuk dengan Google: segera hadir.", Toast.LENGTH_SHORT).show() },
        onDaftar = onDaftar
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun MasukContent(
    s: MasukUiState,
    onBack: (() -> Unit)?,
    onEmail: (String) -> Unit,
    onSandi: (String) -> Unit,
    onMasuk: () -> Unit,
    onLupa: () -> Unit,
    onGoogle: () -> Unit,
    onDaftar: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Masuk", onBack = onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GreenDark),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Restaurant, null, tint = Color.White) }
            Spacer(Modifier.width(10.dp))
            Text("NutriPlan", style = MaterialTheme.typography.titleLarge, color = GreenDark)
        }

        Text("Selamat datang kembali!", style = MaterialTheme.typography.headlineMedium)
        Text("Lanjutkan kebiasaan sehatmu. Rencana makanmu sudah menunggu.", color = TextMuted)

        NutriTextField(
            s.email, onEmail, "Email", error = s.errEmail,
            keyboardType = KeyboardType.Email, leadingIcon = Icons.Filled.Email
        )
        NutriTextField(s.sandi, onSandi, "Kata sandi", error = s.errSandi, isPassword = true)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Lupa kata sandi?", color = GreenDark, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onLupa)
            )
        }

        s.pesanError?.let { InfoBanner(it, type = BannerType.ERROR) }

        NutriButton(
            text = if (s.loading) "Memeriksa..." else "Masuk",
            onClick = onMasuk,
            enabled = !s.loading
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD0D0D0)))
            Text("  atau  ", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD0D0D0)))
        }

        NutriButton("G   Lanjutkan dengan Google", onClick = onGoogle, outline = true)

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = GreenLight) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, null, tint = GreenDark)
                Spacer(Modifier.width(12.dp))
                Text("Data dan preferensi gizimu tersimpan dengan aman.", color = GreenDark)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Belum punya akun? ", color = TextMuted)
            Text(
                "Daftar sekarang", color = GreenDark, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onDaftar)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}