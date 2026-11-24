package com.example.nutriplan.ui.profil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.nutriplan.data.Session
import com.example.nutriplan.data.repository.AuthRepository
import com.example.nutriplan.data.repository.ProfileRepository
import com.example.nutriplan.domain.CalorieCalculator
import com.example.nutriplan.domain.Validator
import com.example.nutriplan.reminder.MealReminder
import com.example.nutriplan.ui.common.nutriViewModel
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.RedError
import com.example.nutriplan.util.FotoProfil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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

    /** onHasil(null) = berhasil, selain itu berisi pesan galat. */
    fun ubahSandi(lama: String, baru: String, onHasil: (String?) -> Unit) {
        viewModelScope.launch {
            onHasil(auth.ubahSandi(lama, baru).exceptionOrNull()?.message)
        }
    }
}

// ---------------- Screen (stateful) ----------------

@Composable
fun ProfilScreen(onEdit: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm = nutriViewModel(key = "profil-${Session.userId}") { c ->
        ProfilViewModel(c.profileRepository, c.authRepository, Session.userId)
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    var pengingat by remember { mutableStateOf(MealReminder.isEnabled(context)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var foto by remember { mutableStateOf(FotoProfil.ambil(context, Session.userId)) }

    val pilihFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val hasil = FotoProfil.simpan(context, Session.userId, uri)
                if (hasil != null) foto = hasil
                else Toast.makeText(context, "Foto gagal disimpan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        foto = foto,
        pengingat = pengingat,
        onGantiFoto = {
            pilihFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onHapusFoto = {
            FotoProfil.hapus(context, Session.userId)
            foto = null
        },
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
        onUbahSandi = { dialog = "sandi" },
        onBantuan = { dialog = "bantuan" },
        onPrivasi = { dialog = "privasi" },
        onLogout = {
            vm.logout()
            onLogout()
        }
    )

    when (dialog) {
        "sandi" -> UbahSandiDialog(
            onTutup = { dialog = null },
            onSimpan = { lama, baru, hasil -> vm.ubahSandi(lama, baru, hasil) }
        )
        "bantuan" -> InfoDialog(
            judul = "Bantuan & dukungan",
            isi = "1. Atur preferensimu di tab Preferensi, lalu simpan untuk membuat rencana makan.\n" +
                    "2. Lihat jadwal di tab Rencana. Tekan \"Sudah makan\" setiap selesai makan.\n" +
                    "3. Pantau kalori dan anggaran harianmu di Dasbor.\n\n" +
                    "Ada kendala? Hubungi tim pengembang NutriPlan (Kelompok 8).",
            onTutup = { dialog = null }
        )
        "privasi" -> InfoDialog(
            judul = "Kebijakan privasi",
            isi = "Data profil, preferensi, dan rencana makanmu disimpan di perangkat ini saja. " +
                    "Kata sandi disimpan dalam bentuk hash, bukan teks asli. " +
                    "Internet hanya dipakai untuk menampilkan inspirasi resep dari TheMealDB.",
            onTutup = { dialog = null }
        )
    }
}

// ---------------- Content (stateless) ----------------

@Composable
fun ProfilContent(
    state: ProfilUiState,
    foto: File?,
    pengingat: Boolean,
    onGantiFoto: () -> Unit,
    onHapusFoto: () -> Unit,
    onPengingat: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onUbahSandi: () -> Unit,
    onBantuan: () -> Unit,
    onPrivasi: () -> Unit,
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
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(GreenLight)
                    .clickable(onClick = onGantiFoto),
                contentAlignment = Alignment.Center
            ) {
                if (foto != null) {
                    AsyncImage(
                        model = foto,
                        contentDescription = "Foto profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(state.inisial, style = MaterialTheme.typography.headlineMedium, color = GreenDark)
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GreenDark)
                    .clickable(onClick = onGantiFoto),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PhotoCamera, contentDescription = "Ganti foto profil",
                    tint = Color.White, modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.nama, style = MaterialTheme.typography.titleLarge)
            Text(state.email, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit profil", color = GreenDark, fontWeight = FontWeight.SemiBold)
                }
                if (foto != null) {
                    TextButton(onClick = onHapusFoto) {
                        Text("Hapus foto", color = RedError, fontWeight = FontWeight.SemiBold)
                    }
                }
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
                BarisMenu("Ubah kata sandi", onUbahSandi)
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
                BarisMenu("Bantuan & dukungan", onBantuan)
                HorizontalDivider(color = Color(0xFFEFEFEF))
                BarisMenu("Kebijakan privasi", onPrivasi)
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

// ---------------- Dialog ----------------

@Composable
private fun UbahSandiDialog(
    onTutup: () -> Unit,
    onSimpan: (String, String, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var lama by remember { mutableStateOf("") }
    var baru by remember { mutableStateOf("") }
    var ulang by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Ubah kata sandi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lama, onValueChange = { lama = it },
                    label = { Text("Kata sandi lama") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = baru, onValueChange = { baru = it },
                    label = { Text("Kata sandi baru") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = ulang, onValueChange = { ulang = it },
                    label = { Text("Ulangi kata sandi baru") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                galat?.let { Text(it, color = RedError, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pesan = Validator.sandi(baru) ?: Validator.konfirmasi(baru, ulang)
                galat = pesan
                if (pesan == null) {
                    onSimpan(lama, baru) { hasil ->
                        if (hasil == null) {
                            Toast.makeText(context, "Kata sandi diubah.", Toast.LENGTH_SHORT).show()
                            onTutup()
                        } else {
                            galat = hasil
                        }
                    }
                }
            }) { Text("Simpan", color = GreenDark) }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

@Composable
private fun InfoDialog(judul: String, isi: String, onTutup: () -> Unit) {
    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text(judul) },
        text = { Text(isi) },
        confirmButton = { TextButton(onClick = onTutup) { Text("Tutup", color = GreenDark) } }
    )
}