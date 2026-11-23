package com.example.nutriplan.data.local

import com.example.nutriplan.data.local.Alergen.KACANG
import com.example.nutriplan.data.local.Alergen.SUSU
import com.example.nutriplan.data.local.Alergen.TELUR
import com.example.nutriplan.data.local.Alergen.UDANG
import com.example.nutriplan.data.local.Pola.RENDAH_KARBO as RK
import com.example.nutriplan.data.local.Pola.SEIMBANG as SB
import com.example.nutriplan.data.local.Pola.TINGGI_PROTEIN as TP
import com.example.nutriplan.data.local.Pola.VEGETARIAN as VG
import com.example.nutriplan.data.local.WaktuMakan.CAMILAN
import com.example.nutriplan.data.local.WaktuMakan.MALAM
import com.example.nutriplan.data.local.WaktuMakan.SARAPAN
import com.example.nutriplan.data.local.WaktuMakan.SIANG
import com.example.nutriplan.data.local.dao.MenuDao
import com.example.nutriplan.data.local.entity.MenuEntity

object MenuSeeder {
    /** REPLACE pada id tetap 1..30: menu bawaan selalu mutakhir. Menu tambahan (id > 30) tidak tersentuh. */
    suspend fun seedIfEmpty(dao: MenuDao) {
        dao.insertAll(MenuSeed.menus)
    }
}

/**
 * Seed awal. Nilai gizi dan harga adalah data contoh untuk tugas
 * (acuan gizi: TKPI Kemenkes, harga: estimasi pasar). Ganti dengan data survei kelompok.
 */
object MenuSeed {

    private fun menu(
        nama: String, waktu: WaktuMakan, kal: Int, p: Int, k: Int, l: Int,
        harga: Int, menit: Int, pola: List<String>, alergen: List<String>,
        bahan: List<String>, langkah: List<String>
    ) = MenuEntity(
        nama = nama, waktuMakan = waktu, kalori = kal, protein = p, karbo = k, lemak = l,
        harga = harga, bahan = bahan, langkah = langkah, alergen = alergen,
        tagPola = pola, waktuMasakMenit = menit,
        fotoRes = "menu_" + nama.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    )

    val menus: List<MenuEntity> = listOf(
        // ---------- SARAPAN ----------
        menu("Oatmeal Pisang Telur", SARAPAN, 380, 14, 52, 12, 15000, 10, listOf(SB, VG), listOf(TELUR),
            listOf("Oat 50 g", "Pisang 1 buah", "Telur rebus 1 butir", "Air secukupnya"),
            listOf("Masak oat dengan air hingga lembut.", "Tambahkan irisan pisang.", "Sajikan dengan telur rebus.")),
        menu("Nasi Uduk Telur Dadar", SARAPAN, 450, 16, 58, 16, 12000, 15, listOf(SB, VG), listOf(TELUR),
            listOf("Nasi uduk 1 porsi", "Telur 1 butir", "Timun", "Bawang goreng"),
            listOf("Dadar telur hingga matang.", "Sajikan di atas nasi uduk.", "Tambahkan timun dan bawang goreng.")),
        menu("Bubur Ayam Sayur", SARAPAN, 350, 18, 48, 8, 11000, 20, listOf(SB), emptyList(),
            listOf("Nasi 1 porsi", "Suwiran ayam 50 g", "Wortel", "Daun bawang"),
            listOf("Masak nasi dengan kaldu menjadi bubur.", "Tambahkan ayam dan wortel.", "Taburi daun bawang.")),
        menu("Roti Gandum Telur Alpukat", SARAPAN, 360, 15, 34, 18, 14000, 10, listOf(SB, VG), listOf(TELUR),
            listOf("Roti gandum 2 lembar", "Telur 1 butir", "Alpukat 1/2 buah"),
            listOf("Panggang roti.", "Ratakan alpukat di atas roti.", "Tambahkan telur rebus iris.")),
        menu("Yogurt Buah Granola", SARAPAN, 300, 10, 44, 9, 13000, 5, listOf(SB, VG), listOf(SUSU, KACANG),
            listOf("Yogurt plain 150 g", "Granola 30 g", "Buah potong"),
            listOf("Tuang yogurt ke mangkuk.", "Tambahkan buah.", "Taburi granola.")),
        menu("Smoothie Bowl Pisang Bayam", SARAPAN, 280, 6, 54, 5, 12000, 8, listOf(SB, VG), emptyList(),
            listOf("Pisang beku 2 buah", "Bayam segenggam", "Air kelapa 100 ml", "Chia seed"),
            listOf("Blender semua bahan hingga kental.", "Tuang ke mangkuk.", "Taburi chia seed.")),
        menu("Omelet Sayur Tahu", SARAPAN, 320, 24, 10, 21, 10000, 12, listOf(TP, RK, VG), listOf(TELUR),
            listOf("Telur 2 butir", "Tahu 50 g", "Paprika", "Bayam"),
            listOf("Kocok telur, campur sayur dan tahu.", "Masak di wajan antilengket.", "Lipat dan sajikan.")),
        menu("Lontong Sayur Tahu", SARAPAN, 370, 12, 60, 8, 9000, 15, listOf(SB, VG), emptyList(),
            listOf("Lontong 1 porsi", "Tahu goreng 2 potong", "Labu siam", "Kuah santan encer"),
            listOf("Masak sayur dengan kuah.", "Potong lontong.", "Siram kuah dan tambahkan tahu.")),

        // ---------- MAKAN SIANG ----------
        menu("Ayam Panggang Sayur", SIANG, 520, 42, 25, 28, 22000, 20, listOf(TP, RK), emptyList(),
            listOf("Dada ayam 150 g", "Brokoli", "Wortel", "Minyak zaitun"),
            listOf("Marinasi ayam 15 menit.", "Panggang hingga matang.", "Sajikan dengan sayur kukus.")),
        menu("Ayam Panggang Nasi Merah", SIANG, 480, 36, 48, 16, 20000, 25, listOf(SB, TP), emptyList(),
            listOf("Ayam 120 g", "Nasi merah 100 g", "Brokoli", "Wortel"),
            listOf("Panggang ayam berbumbu.", "Kukus brokoli dan wortel.", "Sajikan dengan nasi merah.")),
        menu("Nila Panggang Nasi Merah", SIANG, 450, 34, 45, 14, 18000, 25, listOf(SB), emptyList(),
            listOf("Ikan nila 150 g", "Nasi merah 100 g", "Lalapan", "Jeruk nipis"),
            listOf("Lumuri ikan bumbu dan jeruk nipis.", "Panggang hingga matang.", "Sajikan dengan nasi merah dan lalapan.")),
        menu("Gado-gado Lontong", SIANG, 480, 18, 62, 16, 15000, 15, listOf(SB, VG), listOf(KACANG, TELUR),
            listOf("Lontong", "Sayur rebus", "Tahu", "Telur rebus", "Bumbu kacang"),
            listOf("Rebus sayur.", "Susun dengan lontong, tahu, dan telur.", "Siram bumbu kacang.")),
        menu("Pecel Tahu Tempe", SIANG, 450, 20, 50, 18, 13000, 15, listOf(SB, VG), listOf(KACANG),
            listOf("Nasi 100 g", "Sayur rebus", "Tahu", "Tempe", "Bumbu pecel"),
            listOf("Rebus sayur.", "Goreng tahu dan tempe.", "Sajikan dengan bumbu pecel.")),
        menu("Nasi Udang Tumis Brokoli", SIANG, 500, 34, 58, 13, 24000, 20, listOf(TP), listOf(UDANG),
            listOf("Udang 100 g", "Brokoli", "Nasi 100 g", "Bawang putih"),
            listOf("Tumis bawang putih.", "Masukkan udang dan brokoli.", "Sajikan dengan nasi.")),
        menu("Soto Ayam Bening", SIANG, 420, 30, 45, 12, 14000, 25, listOf(SB), emptyList(),
            listOf("Ayam 100 g", "Bihun", "Kol", "Kuah kaldu bening"),
            listOf("Rebus ayam dalam kaldu berbumbu.", "Seduh bihun.", "Siram kuah dan taburi seledri.")),
        menu("Capcay Tahu Nasi", SIANG, 430, 18, 56, 14, 16000, 15, listOf(SB, VG), emptyList(),
            listOf("Aneka sayur", "Tahu 80 g", "Nasi 100 g", "Saus tiram vegetarian"),
            listOf("Tumis bawang putih.", "Masukkan tahu dan sayur.", "Sajikan dengan nasi.")),

        // ---------- MAKAN MALAM ----------
        menu("Tumis Tahu Sayur", MALAM, 410, 18, 40, 18, 12000, 15, listOf(SB, VG), emptyList(),
            listOf("Tahu 100 g", "Buncis", "Wortel", "Nasi 80 g"),
            listOf("Goreng sebentar tahu.", "Tumis sayur dan tahu.", "Sajikan dengan nasi.")),
        menu("Pepes Ikan Kembung Nasi", MALAM, 450, 32, 46, 14, 16000, 30, listOf(TP), emptyList(),
            listOf("Ikan kembung 1 ekor", "Daun kemangi", "Bumbu halus", "Nasi 100 g"),
            listOf("Lumuri ikan bumbu halus.", "Bungkus daun pisang dengan kemangi.", "Kukus 20 menit, sajikan dengan nasi.")),
        menu("Sup Ayam Sayur", MALAM, 380, 30, 28, 14, 15000, 25, listOf(SB, RK), emptyList(),
            listOf("Ayam 100 g", "Wortel", "Kentang", "Seledri"),
            listOf("Rebus ayam hingga empuk.", "Masukkan sayur.", "Masak hingga matang.")),
        menu("Tempe Orek Sayur Asem", MALAM, 420, 20, 50, 16, 10000, 20, listOf(SB, VG), emptyList(),
            listOf("Tempe 100 g", "Kecap", "Sayur asem", "Nasi 100 g"),
            listOf("Orek tempe dengan kecap.", "Panaskan sayur asem.", "Sajikan dengan nasi.")),
        menu("Telur Balado Bayam", MALAM, 400, 20, 36, 19, 9000, 15, listOf(SB, VG), listOf(TELUR),
            listOf("Telur rebus 2 butir", "Sambal balado", "Bayam", "Nasi 80 g"),
            listOf("Goreng sebentar telur rebus.", "Masak dengan sambal.", "Sajikan dengan bayam rebus dan nasi.")),
        menu("Ikan Bakar Tumis Kangkung", MALAM, 430, 38, 22, 20, 18000, 25, listOf(TP, RK), emptyList(),
            listOf("Ikan 150 g", "Kangkung", "Bawang putih", "Kecap"),
            listOf("Bakar ikan berbumbu.", "Tumis kangkung.", "Sajikan bersama.")),
        menu("Ayam Kecap Tumis Buncis", MALAM, 520, 34, 55, 17, 17000, 25, listOf(SB), emptyList(),
            listOf("Ayam 120 g", "Kecap manis", "Buncis", "Nasi 100 g"),
            listOf("Masak ayam dengan kecap.", "Tumis buncis.", "Sajikan dengan nasi.")),
        menu("Cah Udang Brokoli", MALAM, 390, 30, 24, 18, 20000, 15, listOf(TP, RK), listOf(UDANG),
            listOf("Udang 120 g", "Brokoli", "Jamur", "Bawang putih"),
            listOf("Tumis bawang putih.", "Masukkan udang, brokoli, dan jamur.", "Masak hingga matang.")),

        // ---------- CAMILAN ----------
        menu("Pisang Rebus", CAMILAN, 110, 1, 27, 0, 4000, 10, listOf(SB, VG), emptyList(),
            listOf("Pisang 1 buah"), listOf("Rebus pisang hingga empuk.", "Sajikan hangat.")),
        menu("Telur Rebus 2 Butir", CAMILAN, 150, 12, 1, 10, 5000, 10, listOf(TP, RK, VG), listOf(TELUR),
            listOf("Telur 2 butir", "Garam"), listOf("Rebus telur 10 menit.", "Kupas dan sajikan.")),
        menu("Kacang Rebus", CAMILAN, 180, 8, 16, 9, 6000, 20, listOf(SB, VG), listOf(KACANG),
            listOf("Kacang tanah 80 g"), listOf("Rebus kacang hingga empuk.", "Tiriskan dan sajikan.")),
        menu("Yogurt Plain Buah", CAMILAN, 130, 6, 20, 3, 8000, 3, listOf(SB, VG), listOf(SUSU),
            listOf("Yogurt plain 120 g", "Buah potong"), listOf("Tuang yogurt.", "Tambahkan buah.")),
        menu("Tahu Kukus Bumbu", CAMILAN, 120, 10, 4, 7, 5000, 15, listOf(TP, RK, VG), emptyList(),
            listOf("Tahu 100 g", "Bawang putih", "Kecap"), listOf("Kukus tahu.", "Siram bumbu kecap bawang.")),
        menu("Smoothie Pisang Oat", CAMILAN, 160, 4, 30, 3, 9000, 5, listOf(SB, VG), emptyList(),
            listOf("Pisang 1 buah", "Oat 20 g", "Air 150 ml"), listOf("Blender semua bahan.", "Sajikan dingin."))
    ).mapIndexed { i, m -> m.copy(id = i + 1L) }   // id tetap supaya seed konsisten
}