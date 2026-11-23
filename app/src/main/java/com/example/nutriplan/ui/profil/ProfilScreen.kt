package com.example.nutriplan.ui.profil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.nutriplan.reminder.MealReminder
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.nutriplan.data.repository.AuthRepository
import com.example.nutriplan.data.repository.ProfileRepository
import com.example.nutriplan.domain.CalorieCalculator
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.RedError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

// ---------------- State & ViewModel ----------------

data class ProfilUiState(
    val loading: Boolean = true,
    val nama: String = "",
    val email: String = "",
    val inisial: String = "",
    val berat: Int? = null,
    val tinggi: Int? = null,
    val usia: Int? = null
)

private fun inisialDari(nama: String): String =
    nama.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

class ProfilViewModel(
    private val profileRepo: ProfileRepository,
    private val auth: AuthRepository,
    private val userId: Long
) : ViewModel() {

    val ui: StateFlow<ProfilUiState> = profileRepo.observe(userId)
        .map { p ->
            val user = profileRepo.getUser(userId)
            val nama = user?.nama.orEmpty()
            ProfilUiState(
                loading = false,
                nama = nama,
                email = user?.email.orEmpty(),
                inisial = inisialDari(nama),
                berat = p?.beratKg,
                tinggi = p?.tinggiCm,
                usia = p?.let { runCatching { CalorieCalculator.umur(LocalDate.parse(it.tanggalLahir)) }.getOrNull() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilUiState())

    fun logout() = auth.logout()
}

// ---------------- Screen (stateful) ----------------

@Composable
fun ProfilScreen(onEdit: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val vm = nutriViewModel(key = "profil-${Session.userId}") { c ->
        ProfilViewModel(c.profileRepository, c.authRepository, Session.userId)
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    var pengingat by remember { mutableStateOf(MealReminder.isEnabled(context)) }

    val aktifkan = {
        MealReminder.enable(context)
        pengingat = true
        Toast.makeText(context, "Pengingat aktif: 07.00, 12.30, dan 19.00.", Toast.LENGTH_SHORT).show()
    }
    val izin = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) aktifkan()
        else {
            pengingat = false
            Toast.makeText(context, "Izin notifikasi ditolak. Pengingat tidak aktif.", Toast.LENGTH_SHORT).show()
        }
    }

    ProfilContent(
        state = state,
        pengingat = pengingat,
        onPengingat = { nyala ->
            if (!nyala) {
                MealReminder.disable(context)
                pengingat = false
            } else if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                izin.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                aktifkan()
            }
        },
        onEdit = onEdit,
        onSegera = { Toast.makeText(context, "$it: segera hadir.", Toast.LENGTH_SHORT).show() },
        onLogout = {
            vm.logout()
            onLogout()
        }
    )
}

// ---------------- Content (stateless) ----------------

@Composable
fun ProfilContent(
    state: ProfilUiState,
    pengingat: Boolean,
    onPengingat: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onSegera: (String) -> Unit,
    onLogout: () -> Unit
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text(state.inisial, style = MaterialTheme.typography.headlineMedium, color = GreenDark)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.nama, style = MaterialTheme.typography.titleLarge)
            Text(state.email, color = TextMuted)
            TextButton(onClick = onEdit) {
                Text("Edit profil", color = GreenDark, fontWeight = FontWeight.SemiBold)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Statistik(state.berat?.let { "$it kg" } ?: "-", "Berat badan", Modifier.weight(1f))
                Statistik(state.tinggi?.let { "$it cm" } ?: "-", "Tinggi badan", Modifier.weight(1f))
                Statistik(state.usia?.let { "$it tahun" } ?: "-", "Usia", Modifier.weight(1f))
            }
        }

        Text(
            "AKUN", style = MaterialTheme.typography.labelLarge, color = TextMuted,
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                BarisMenu("Data pribadi", onEdit)
                HorizontalDivider(color = Color(0xFFEFEFEF))
                BarisMenu("Ubah kata sandi") { onSegera("Ubah kata sandi") }
                HorizontalDivider(color = Color(0xFFEFEFEF))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pengingat waktu makan", Modifier.weight(1f))
                    Switch(
                        checked = pengingat, onCheckedChange = onPengingat,
                        colors = SwitchDefaults.colors(checkedTrackColor = GreenDark)
                    )
                }
                HorizontalDivider(color = Color(0xFFEFEFEF))
                BarisMenu("Bantuan & dukungan") { onSegera("Bantuan & dukungan") }
                HorizontalDivider(color = Color(0xFFEFEFEF))
                BarisMenu("Kebijakan privasi") { onSegera("Kebijakan privasi") }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, RedError),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
        ) { Text("Keluar akun", fontWeight = FontWeight.Bold) }

        Text("NutriPlan • Versi 1.0.0", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun Statistik(nilai: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nilai, style = MaterialTheme.typography.titleLarge, color = GreenDark)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun BarisMenu(judul: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(judul, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
    }
}