package com.example.nutriplan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NutriColors = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = OrangeBar,
    secondaryContainer = OrangeSoft,
    onSecondaryContainer = PriceAmber,
    background = Cream,
    surface = Color.White,
    surfaceVariant = GreenLight,
    error = RedError,
    onError = Color.White
)

@Composable
fun NutriPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NutriColors,
        typography = Typography,
        content = content
    )
}