
package com.example.nutriplan.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nutriplan.data.local.WaktuMakan
import java.util.Calendar

object MealReminder {

    const val CHANNEL_ID = "nutriplan_meal_reminder"
    const val EXTRA_WAKTU = "extra_waktu_makan"

    private const val PREFS = "nutriplan_reminder"
    private const val KEY_ENABLED = "enabled"

    private val jadwal = listOf(
        WaktuMakan.SARAPAN,
        WaktuMakan.SIANG,
        WaktuMakan.MALAM
    )

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        ).getBoolean(KEY_ENABLED, false)
    }

    fun enable(context: Context) {
        context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        ).edit().putBoolean(KEY_ENABLED, true).apply()

        buatChannel(context)
        jadwalkan(context)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        ).edit().putBoolean(KEY_ENABLED, false).apply()

        val alarm = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        jadwal.forEach { waktu ->
            alarm.cancel(pendingIntent(context, waktu))
        }
    }

    fun restore(context: Context) {
        if (isEnabled(context)) {
            buatChannel(context)
            jadwalkan(context)
        }
    }

    fun buatChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Waktu Makan",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat jadwal makan NutriPlan"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private fun jadwalkan(context: Context) {
        val alarm = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        jadwal.forEach { waktu ->
            val bagian = waktu.jam.split(".")
            val jam = bagian[0].toInt()
            val menit = bagian[1].toInt()

            val kalender = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, jam)
                set(Calendar.MINUTE, menit)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarm.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                kalender.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent(context, waktu)
            )
        }
    }

    private fun pendingIntent(
        context: Context,
        waktu: WaktuMakan
    ): PendingIntent {
        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {
            putExtra(EXTRA_WAKTU, waktu.name)
        }

        return PendingIntent.getBroadcast(
            context,
            waktu.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }
}
