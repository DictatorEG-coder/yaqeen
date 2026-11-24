package com.yaqeen.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal fun normalizeSalawatIntervalHours(hours: Int): Int = hours.coerceIn(1, 24)

internal fun salawatIntervalMillis(hours: Int): Long =
    normalizeSalawatIntervalHours(hours) * 60L * 60L * 1000L

internal object SalawatReminderScheduler {
    private const val PREFS = "salawat_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOURS = "interval_hours"
    private const val REQUEST_CODE = 724
    private const val CHANNEL_ID = "salawat_reminders"
    const val ACTION_REMIND = "com.yaqeen.app.ACTION_SALAWAT_REMINDER"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun intervalHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOURS, 3)

    fun schedule(context: Context, hours: Int) {
        val safeHours = normalizeSalawatIntervalHours(hours)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_HOURS, safeHours)
            .apply()
        createChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis = salawatIntervalMillis(safeHours)
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context))
    }

    private fun reminderPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, SalawatReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التذكير بالصلاة على النبي ﷺ",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تذكير اختياري بالصلاة على النبي ﷺ" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تذكير بالصلاة على النبي ﷺ")
            .setContentText("اللهم صلِّ وسلِّم على نبينا محمد ﷺ")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(724, notification)
    }
}

class SalawatReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (SalawatReminderScheduler.isEnabled(context)) {
                    SalawatReminderScheduler.schedule(
                        context,
                        SalawatReminderScheduler.intervalHours(context)
                    )
                }
            }
            SalawatReminderScheduler.ACTION_REMIND -> SalawatReminderScheduler.showNotification(context)
        }
    }
}
