package com.example.nutriplan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** Foto profil disimpan di penyimpanan privat aplikasi, satu file per akun. */
object FotoProfil {
    private const val SISI_MAKS = 1024

    private fun awalan(userId: Long) = "profil_${userId}_"

    fun ambil(context: Context, userId: Long): File? =
        context.filesDir.listFiles { f -> f.name.startsWith(awalan(userId)) }
            ?.maxByOrNull { it.lastModified() }

    fun hapus(context: Context, userId: Long) {
        context.filesDir.listFiles { f -> f.name.startsWith(awalan(userId)) }
            ?.forEach { it.delete() }
    }

    /** Mengembalikan file hasil simpan, atau null kalau gagal. */
    suspend fun simpan(context: Context, userId: Long, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver

                // 1) Ukur dulu, lalu decode dalam ukuran lebih kecil supaya hemat memori.
                val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
                var sample = 1
                while (batas.outWidth / sample > SISI_MAKS || batas.outHeight / sample > SISI_MAKS) {
                    sample *= 2
                }
                val mentah = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(
                        it, null, BitmapFactory.Options().apply { inSampleSize = sample }
                    )
                } ?: return@withContext null

                // 2) Putar sesuai orientasi dari kamera.
                val orientasi = resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
                val derajat = when (orientasi) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val bitmap = if (derajat == 0f) mentah else Bitmap.createBitmap(
                    mentah, 0, 0, mentah.width, mentah.height,
                    Matrix().apply { postRotate(derajat) }, true
                )

                // 3) Hapus foto lama, simpan yang baru dengan nama unik (supaya Coil tidak memakai cache lama).
                hapus(context, userId)
                val file = File(context.filesDir, "${awalan(userId)}${System.currentTimeMillis()}.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                file
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
}