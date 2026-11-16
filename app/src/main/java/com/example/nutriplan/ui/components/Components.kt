package com.example.nutriplan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.GreenLight
import com.example.nutriplan.ui.theme.OrangeSoft
import com.example.nutriplan.ui.theme.PriceAmber
import com.example.nutriplan.ui.theme.RedError
import java.time.LocalDate

val TextMuted = Color(0xFF6B6B6B)

@Composable
fun NutriButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outline: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
    val m = modifier.fillMaxWidth().height(52.dp)
    if (outline) {
        OutlinedButton(
            onClick = onClick, modifier = m, enabled = enabled, shape = shape,
            border = BorderStroke(1.dp, GreenDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDark),
            content = content
        )
    } else {
        Button(
            onClick = onClick, modifier = m, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark, contentColor = Color.White),
            content = content
        )
    }
}

/** Foto menu dari drawable (nama di MenuEntity.fotoRes). Kalau belum ada, tampil kotak pengganti. */
@Composable
fun MenuPhoto(
    fotoRes: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val context = LocalContext.current
    val resId = remember(fotoRes) {
        if (fotoRes.isBlank()) 0
        else context.resources.getIdentifier(fotoRes, "drawable", context.packageName)
    }
    Box(modifier.clip(shape).background(GreenLight), contentAlignment = Alignment.Center) {
        if (resId != 0) {
            Image(
                painter = painterResource(resId), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Filled.Restaurant, contentDescription = null,
                tint = GreenDark.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionText != null) {
            TextButton(onClick = onAction) {
                Text(actionText, color = GreenDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

enum class BannerType { INFO, WARNING, ERROR }

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, type: BannerType = BannerType.INFO) {
    val (bg, fg) = when (type) {
        BannerType.INFO -> GreenLight to GreenDark
        BannerType.WARNING -> OrangeSoft to PriceAmber
        BannerType.ERROR -> RedError.copy(alpha = 0.10f) to RedError
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = bg) {
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
    }
}

private val NAMA_HARI = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/** Tab tanggal. selected dimulai dari 1. */
@Composable
fun DayTabs(dates: List<LocalDate>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dates.forEachIndexed { i, d ->
            val aktif = (i + 1) == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (aktif) GreenDark else Color.White)
                    .clickable { onSelect(i + 1) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    NAMA_HARI[d.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (aktif) Color.White else TextMuted
                )
                Text(
                    "${d.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (aktif) Color.White else Color.Black
                )
            }
        }
    }
}