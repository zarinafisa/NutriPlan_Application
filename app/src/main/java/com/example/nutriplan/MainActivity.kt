package com.example.nutriplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nutriplan.ui.navigation.AppNav
import com.example.nutriplan.ui.theme.NutriPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NutriPlanTheme {
                AppNav()
            }
        }
    }
}