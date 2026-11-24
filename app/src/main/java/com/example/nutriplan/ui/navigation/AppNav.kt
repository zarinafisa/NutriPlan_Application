package com.example.nutriplan.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.nutriplan.NutriApp
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.ui.auth.DaftarScreen
import com.example.nutriplan.ui.auth.MasukScreen
import com.example.nutriplan.ui.beranda.BerandaScreen
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.dasbor.DasborScreen
import com.example.nutriplan.ui.detail.DetailMenuScreen
import com.example.nutriplan.ui.ganti.FilterMenuScreen
import com.example.nutriplan.ui.ganti.GantiMenuScreen
import com.example.nutriplan.ui.ganti.KonfirmasiMenuScreen
import com.example.nutriplan.ui.preferensi.PreferensiScreen
import com.example.nutriplan.ui.profil.DataDiriScreen
import com.example.nutriplan.ui.profil.EditProfilScreen
import com.example.nutriplan.ui.profil.ProfilScreen
import com.example.nutriplan.ui.rencana.RencanaScreen
import com.example.nutriplan.ui.riwayat.RiwayatScreen
import com.example.nutriplan.ui.tambah.TambahMakananScreen
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight

object Routes {
    const val BERANDA = "beranda"
    const val MASUK = "masuk"
    const val DAFTAR = "daftar"
    const val DATA_DIRI = "data_diri"
    const val EDIT_PROFIL = "edit_profil"
    const val DASBOR = "dasbor"
    const val PREFERENSI = "preferensi"
    const val RENCANA = "rencana"
    const val RIWAYAT = "riwayat"
    const val PROFIL = "profil"
    const val RENCANA_LAMA = "rencana_lama/{planId}"
    const val DETAIL = "detail/{itemId}"
    const val TAMBAH = "tambah/{hari}/{waktu}"
    const val SWAP_GRAPH = "swap_graph"
    const val GANTI = "ganti/{itemId}"
    const val FILTER = "filter/{itemId}"
    const val KONFIRMASI = "konfirmasi/{itemId}"

    fun rencanaLama(planId: Long) = "rencana_lama/$planId"
    fun detail(itemId: Long) = "detail/$itemId"
    fun tambah(hari: Int, waktu: WaktuMakan, ganti: Boolean = false) = "tambah/$hari/${waktu.name}?ganti=$ganti"    fun ganti(itemId: Long) = "ganti/$itemId"
    fun filter(itemId: Long) = "filter/$itemId"
    fun konfirmasi(itemId: Long) = "konfirmasi/$itemId"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.DASBOR, "Dasbor", Icons.Filled.Dashboard),
    TabItem(Routes.PREFERENSI, "Preferensi", Icons.Filled.Tune),
    TabItem(Routes.RENCANA, "Rencana", Icons.Filled.CalendarMonth),
    TabItem(Routes.RIWAYAT, "Riwayat", Icons.Filled.History),
    TabItem(Routes.PROFIL, "Profil", Icons.Filled.Person)
)

@Composable
private fun BottomNavBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        tab.label,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenDark,
                    selectedTextColor = GreenDark,
                    indicatorColor = GreenLight,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Composable
fun AppNav() {
    val context = LocalContext.current
    val container = (context.applicationContext as NutriApp).container
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabRoutes = tabs.map { it.route }

    // Layar awal ditentukan dari sesi yang tersimpan.
    val startRoute by produceState<String?>(initialValue = null) {
        val user = container.authRepository.currentUser()
        value = when {
            user == null -> {
                container.authRepository.logout()
                Routes.BERANDA
            }
            container.profileRepository.get(user.id) == null -> Routes.DATA_DIRI
            else -> Routes.DASBOR
        }
    }

    val start = startRoute
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }

    val goTab: (String) -> Unit = { target ->
        nav.navigate(target) {
            popUpTo(Routes.DASBOR) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (route in tabRoutes) BottomNavBar(route, goTab) }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.BERANDA) {
                BerandaScreen(
                    onMulai = { nav.navigate(Routes.DAFTAR) },
                    onMasuk = { nav.navigate(Routes.MASUK) }
                )
            }
            composable(Routes.MASUK) {
                MasukScreen(
                    onBack = { nav.popBackStack() },
                    onSukses = { adaProfil ->
                        val tujuan = if (adaProfil) Routes.DASBOR else Routes.DATA_DIRI
                        nav.navigate(tujuan) { popUpTo(0) { inclusive = true } }
                    },
                    onDaftar = {
                        nav.navigate(Routes.DAFTAR) { popUpTo(Routes.BERANDA) }
                    }
                )
            }
            composable(Routes.DAFTAR) {
                DaftarScreen(
                    onBack = { nav.popBackStack() },
                    onSelesai = {
                        Toast.makeText(context, "Akun dibuat. Silakan masuk.", Toast.LENGTH_SHORT).show()
                        nav.navigate(Routes.MASUK) { popUpTo(Routes.BERANDA) }
                    },
                    onMasuk = {
                        nav.navigate(Routes.MASUK) { popUpTo(Routes.BERANDA) }
                    }
                )
            }
            composable(Routes.DATA_DIRI) {
                DataDiriScreen(
                    onSelesai = {
                        nav.navigate(Routes.DASBOR) { popUpTo(Routes.DATA_DIRI) { inclusive = true } }
                    }
                )
            }
            composable(Routes.EDIT_PROFIL) {
                EditProfilScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.DASBOR) {
                DasborScreen(
                    onLihatRencana = { goTab(Routes.RENCANA) },
                    onDetail = { nav.navigate(Routes.detail(it)) },
                    onSusunRencana = { goTab(Routes.PREFERENSI) },
                    onLonceng = { goTab(Routes.PROFIL) }
                )
            }
            composable(Routes.PREFERENSI) {
                PreferensiScreen(onPlanCreated = { goTab(Routes.RENCANA) })
            }
            composable(Routes.RENCANA) {
                RencanaScreen(
                    onBack = { nav.popBackStack() },
                    onDetail = { nav.navigate(Routes.detail(it)) },
                    onTambah = { hari, waktu -> nav.navigate(Routes.tambah(hari, waktu)) },
                    onGanti = { hari, waktu -> nav.navigate(Routes.tambah(hari, waktu, ganti = true)) },
                    onBuatRencana = { goTab(Routes.PREFERENSI) }
                )
            }
            composable(Routes.RIWAYAT) {
                RiwayatScreen(
                    onBuka = { planId, aktif ->
                        if (aktif) goTab(Routes.RENCANA) else nav.navigate(Routes.rencanaLama(planId))
                    },
                    onSusunRencana = { goTab(Routes.PREFERENSI) }
                )
            }
            composable(
                Routes.RENCANA_LAMA,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                RencanaScreen(
                    planId = entry.arguments?.getLong("planId"),
                    onBack = { nav.popBackStack() },
                    onDetail = { nav.navigate(Routes.detail(it)) },
                    onTambah = { _, _ -> },
                    onGanti = { _, _ -> },
                    onBuatRencana = { goTab(Routes.PREFERENSI) },
                    onGunakanLagiSelesai = { goTab(Routes.RENCANA) }
                )
            }
            composable(Routes.PROFIL) {
                ProfilScreen(
                    onEdit = { nav.navigate(Routes.EDIT_PROFIL) },
                    onLogout = {
                        nav.navigate(Routes.BERANDA) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entry ->
                DetailMenuScreen(
                    itemId = entry.arguments?.getLong("itemId") ?: 0L,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                Routes.TAMBAH,
                arguments = listOf(
                    navArgument("hari") { type = NavType.IntType },
                    navArgument("waktu") { type = NavType.StringType },
                    navArgument("ganti") { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val waktu = runCatching {
                    WaktuMakan.valueOf(entry.arguments?.getString("waktu") ?: "")
                }.getOrDefault(WaktuMakan.SARAPAN)
                TambahMakananScreen(
                    hari = entry.arguments?.getInt("hari") ?: 1,
                    waktuAwal = waktu,
                    ganti = entry.arguments?.getBoolean("ganti") ?: false,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}