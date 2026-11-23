package com.example.nutriplan.data.remote

/** Bentuk mentah JSON TheMealDB: {"meals":[{...}]}. */
data class MealsResponse(val meals: List<Map<String, String?>>?)

/** Data resep yang sudah rapi dan siap ditampilkan di layar. */
data class InspirasiResep(
    val id: String,
    val nama: String,
    val kategori: String,
    val daerah: String,
    val fotoUrl: String,
    val bahan: List<String>,
    val langkah: List<String>
)

private const val MAKS_BAHAN = 20

/** Mengubah satu resep mentah menjadi InspirasiResep. Hasilnya null bila id atau nama kosong. */
fun Map<String, String?>.toInspirasi(): InspirasiResep? {
    val id = this["idMeal"]?.trim().orEmpty()
    val nama = this["strMeal"]?.trim().orEmpty()
    if (id.isEmpty() || nama.isEmpty()) return null

    val bahan = (1..MAKS_BAHAN).mapNotNull { i ->
        val b = this["strIngredient$i"]?.trim().orEmpty()
        if (b.isEmpty()) null
        else "${this["strMeasure$i"]?.trim().orEmpty()} $b".trim()
    }

    val langkah = this["strInstructions"].orEmpty()
        .split(Regex("\\r?\\n+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return InspirasiResep(
        id = id,
        nama = nama,
        kategori = this["strCategory"]?.trim().orEmpty(),
        daerah = this["strArea"]?.trim().orEmpty(),
        fotoUrl = this["strMealThumb"]?.trim().orEmpty(),
        bahan = bahan,
        langkah = langkah
    )
}