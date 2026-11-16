package com.example.nutriplan.data.local

object Pola {
    const val TINGGI_PROTEIN = "Tinggi protein"
    const val RENDAH_KARBO = "Rendah karbohidrat"
    const val SEIMBANG = "Seimbang"
    const val VEGETARIAN = "Vegetarian"
    const val TANPA_SUSU = "Tanpa susu"

    val semua = listOf(TINGGI_PROTEIN, RENDAH_KARBO, SEIMBANG, VEGETARIAN, TANPA_SUSU)
}

object Alergen {
    const val KACANG = "Kacang"
    const val UDANG = "Udang"
    const val SUSU = "Susu"
    const val TELUR = "Telur"

    val semua = listOf(KACANG, UDANG, SUSU, TELUR)
}