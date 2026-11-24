package com.example.nutriplan.ui.beranda

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.nutriplan.R
import com.example.nutriplan.ui.components.MenuPhoto
import com.example.nutriplan.ui.components.NutriButton
import com.example.nutriplan.ui.components.TextMuted
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight

@Composable
fun BerandaScreen(onMulai: () -> Unit, onMasuk: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_nutriplan),
            contentDescription = "NutriPlan",
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentScale = ContentScale.Fit
        )

        // Taruh gambar bernama beranda_hero di res/drawable agar foto tampil di sini.
        Box(Modifier.fillMaxWidth().height(260.dp)) {
            MenuPhoto("beranda_hero", Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp))
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                shape = RoundedCornerShape(50),
                color = Color.White
            ) {
                Text(
                    "Sehat, enak, sesuai anggaran",
                    style = MaterialTheme.typography.labelLarge, color = GreenDark,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Text(
            buildAnnotatedString {
                append("Makan lebih baik,\n")
                withStyle(SpanStyle(color = GreenDark)) { append("mulai hari ini.") }
            },
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "Rencana makan personal untuk kebutuhan gizi dan anggaran harianmu.",
            color = TextMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipInfo(Icons.Filled.CalendarMonth, "Menu 7 hari")
            ChipInfo(Icons.Filled.AccountBalanceWallet, "Lebih hemat")
        }

        Spacer(Modifier.height(4.dp))
        NutriButton("Mulai • buat akun", onClick = onMulai)
        NutriButton("Sudah punya akun? Masuk", onClick = onMasuk, outline = true)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChipInfo(ikon: ImageVector, teks: String) {
    Surface(shape = RoundedCornerShape(50), color = GreenLight) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ikon, null, Modifier.size(18.dp), tint = GreenDark)
            Spacer(Modifier.width(6.dp))
            Text(teks, color = GreenDark, style = MaterialTheme.typography.labelLarge)
        }
    }
}