package com.example.nutriplan.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.detail.DetailMenuScreen
import com.example.nutriplan.ui.preferensi.PreferensiScreen
import com.example.nutriplan.ui.rencana.RencanaScreen
import com.example.nutriplan.ui.tambah.TambahMakananScreen
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight

object Routes {
    const val DASBOR = "dasbor"
    const val PREFERENSI = "preferensi"
    const val RENCANA = "rencana"
    const val RIWAYAT = "riwayat"
    const val PROFIL = "profil"
    const val DETAIL = "detail/{itemId}"
    const val TAMBAH = "tambah/{hari}/{waktu}"

    fun detail(itemId: Long) = "detail/$itemId"
    fun tambah(hari: Int, waktu: WaktuMakan) = "tambah/$hari/${waktu.name}"
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
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabRoutes = tabs.map { it.route }

    val goTab: (String) -> Unit = { target ->
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
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
            startDestination = Routes.PREFERENSI,   // sementara; nanti Beranda (Tahap 8)
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.DASBOR) { PlaceholderScreen("Dasbor", "Dibuat di Tahap 6") }
            composable(Routes.PREFERENSI) {
                PreferensiScreen(onPlanCreated = { goTab(Routes.RENCANA) })
            }
            composable(Routes.RENCANA) {
                RencanaScreen(
                    onBack = { nav.popBackStack() },
                    onDetail = { nav.navigate(Routes.detail(it)) },
                    onTambah = { hari, waktu -> nav.navigate(Routes.tambah(hari, waktu)) },
                    onBuatRencana = { goTab(Routes.PREFERENSI) }
                )
            }
            composable(Routes.RIWAYAT) { PlaceholderScreen("Riwayat", "Dibuat di Tahap 8") }
            composable(Routes.PROFIL) { PlaceholderScreen("Profil", "Dibuat di Tahap 5") }
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
                    navArgument("waktu") { type = NavType.StringType }
                )
            ) { entry ->
                val waktu = runCatching {
                    WaktuMakan.valueOf(entry.arguments?.getString("waktu") ?: "")
                }.getOrDefault(WaktuMakan.SARAPAN)
                TambahMakananScreen(
                    hari = entry.arguments?.getInt("hari") ?: 1,
                    waktuAwal = waktu,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, note: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(note, color = TextMuted)
    }
}