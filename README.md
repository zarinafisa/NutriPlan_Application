# NutriPlan

Aplikasi Android untuk membantu merencanakan asupan nutrisi harian. Dibuat dengan Kotlin dan Jetpack Compose, dengan antarmuka berbahasa Indonesia.

> Proyek akhir Praktikum Pengembangan Aplikasi Perangkat Bergerak, Universitas Brawijaya.

## Fitur

- Registrasi dan login pengguna (password disimpan dalam bentuk hash)
- Kalkulator kebutuhan kalori harian
- Pembuatan rencana makan otomatis sesuai profil dan target kalori
- Penggantian menu dengan filter alergi, pola makan (misalnya vegetarian), dan rentang kalori
- 30 menu bawaan lengkap dengan bahan, langkah memasak, nilai gizi, dan perkiraan harga
- Penyimpanan data lokal di perangkat, tanpa perlu internet

## Teknologi

- **Bahasa:** Kotlin
- **UI:** Jetpack Compose
- **Database lokal:** Room
- **Build:** Gradle (Kotlin DSL)
- **Arsitektur:** Repository pattern

## Struktur Proyek

```
app/src/main/java/com/example/nutriplan/
├── data/
│   ├── local/        # Entity, DAO, dan database Room
│   └── repository/   # Sumber data, misalnya AuthRepository
├── domain/           # Logika bisnis (kalkulator kalori, generator rencana makan)
└── util/             # Fungsi bantu, misalnya PasswordHasher
```

## Cara Menjalankan

1. Pasang **Android Studio** versi terbaru (JDK bawaan sudah cukup).
2. Clone repository:
   ```bash
   git clone https://github.com/zarinafisa/NutriPlan_Application.git
   ```
3. Buka folder proyek lewat **File > Open** di Android Studio.
4. Tunggu proses **Gradle Sync** selesai.
5. Pilih emulator atau perangkat Android, lalu klik **Run**.

## Menjalankan Tes

```bash
./gradlew test
```

Di Windows PowerShell gunakan `.\gradlew test`.

## Catatan

Nilai gizi dan harga pada menu bawaan adalah data contoh untuk keperluan tugas, bukan acuan gizi yang akurat.

## Lisensi

Proyek ini dibuat untuk keperluan pembelajaran.