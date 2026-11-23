package com.example.nutriplan.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.nutriplan.MainActivity
import com.example.nutriplan.data.local.WaktuMakan

class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")   // izin sudah diperiksa di bawah (Android 13+)
    override fun onReceive(context: Context, intent: Intent) {
        if (!MealReminder.isEnabled(context)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val waktu = runCatching {
            WaktuMakan.valueOf(intent.getStringExtra(MealReminder.EXTRA_WAKTU) ?: "")
        }.getOrNull() ?: return

        MealReminder.buatChannel(context)
        val buka = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, MealReminder.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Waktunya ${waktu.label.lowercase()}")
            .setContentText("Buka NutriPlan untuk melihat menu hari ini.")
            .setContentIntent(buka)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(100 + waktu.ordinal, notif)
    }
}