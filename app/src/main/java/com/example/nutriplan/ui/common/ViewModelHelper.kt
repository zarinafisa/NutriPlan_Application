package com.example.nutriplan.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriplan.NutriApp
import com.example.nutriplan.data.AppContainer

/** Membuat ViewModel dengan akses ke repository. */
@Composable
inline fun <reified VM : ViewModel> nutriViewModel(
    key: String? = null,
    crossinline builder: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as NutriApp).container
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder(container) as T
        }
    )
}

/** Versi yang memakai pemilik tertentu (dipakai untuk berbagi ViewModel antar-layar dalam satu graph). */
@Composable
inline fun <reified VM : ViewModel> nutriViewModelOwner(
    owner: ViewModelStoreOwner,
    crossinline builder: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as NutriApp).container
    return viewModel(
        viewModelStoreOwner = owner,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder(container) as T
        }
    )
}