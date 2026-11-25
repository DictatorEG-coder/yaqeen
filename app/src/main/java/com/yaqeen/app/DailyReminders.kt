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
import java.util.Calendar

internal enum class DailyReminderKind(
    val key: String,
    val title: String,
    val message: String,
    val notificationId: Int
) {
    SALAWAT("salawat", "الصلاة على النبي ﷺ", "لا تنسَ الصلاة والسلام على رسول الله ﷺ.", 3101),
    SUNNAH("sunnah", "سنة نبوية", "تذكير بسنة نبوية ثابتة؛ افتح يقين واقرأ دليلها.", 3102),
    HADITH("hadith", "حديث نبوي", "حان وقت قراءة حديث من كتب السنة.", 3103),
    NIGHT_PRAYER("night_prayer", "قيام الليل", "تذكير بقيام الليل ولو بركعتين.", 3104),
    QURAN("quran", "قراءة القرآن", "اجعل لك وردًا من القرآن اليوم.", 3105),
    MORNING_ADHKAR("morning_adhkar", "أذكار الصباح", "حافظ على أذكار الصباح من حصن المسلم.", 3106),
    EVENING_ADHKAR("evening_adhkar", "أذكار المساء", "حافظ على أذكار المساء من حصن المسلم.", 3107)
}

internal object DailyReminderScheduler {
    private const val PREFS = "daily_reminders"
    private const val CHANNEL = "daily_reminders"
    private fun pref(kind: DailyReminderKind, field: String) = "${kind.key}_$field"

    fun enabled(context: Context, kind: DailyReminderKind): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(pref(kind, "enabled"), false)

    fun mode(context: Context, kind: DailyReminderKind): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(pref(kind, "mode"), "interval") ?: "interval"

    fun hour(context: Context, kind: DailyReminderKind): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(pref(kind, "hour"), defaultHour(kind))

    fun minute(context: Context, kind: DailyReminderKind): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(pref(kind, "minute"), 0)

    fun interval(context: Context, kind: DailyReminderKind): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(pref(kind, "interval"), defaultInterval(kind))

    fun configure(
        context: Context,
        kind: DailyReminderKind,
        enabled: Boolean,
        mode: String,
        hour: Int,
        minute: Int,
        intervalHours: Int
    ) {
        require(mode == "interval" || mode == "daily")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(pref(kind, "enabled"), enabled)
            .putString(pref(kind, "mode"), mode)
            .putInt(pref(kind, "hour"), hour.coerceIn(0, 23))
            .putInt(pref(kind, "minute"), minute.coerceIn(0, 59))
            .putInt(pref(kind, "interval"), intervalHours.coerceIn(1, 24))
            .apply()
        if (enabled) {
            createChannel(context)
            schedule(context, kind)
        } else {
            alarmManager(context).cancel(pendingIntent(context, kind))
        }
    }

    fun restore(context: Context) {
        DailyReminderKind.entries.filter { enabled(context, it) }.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, kind: DailyReminderKind, now: Long = System.currentTimeMillis()) {
        if (!enabled(context, kind)) return
        val trigger = if (mode(context, kind) == "daily") {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour(context, kind))
                set(Calendar.MINUTE, minute(context, kind))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.timeInMillis
        } else {
            now + interval(context, kind) * 60L * 60L * 1000L
        }
        val alarm = alarmManager(context)
        val pending = pendingIntent(context, kind)
        alarm.cancel(pending)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun deliver(context: Context, kind: DailyReminderKind) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val launch = PendingIntent.getActivity(
            context, kind.notificationId + 100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(kind.title)
            .setContentText(kind.message)
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "التذكيرات اليومية", NotificationManager.IMPORTANCE_DEFAULT)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context, kind: DailyReminderKind) =
        PendingIntent.getBroadcast(
            context, kind.notificationId,
            Intent(context, DailyReminderReceiver::class.java).setAction("com.yaqeen.app.DAILY_${kind.key.uppercase()}"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun defaultHour(kind: DailyReminderKind): Int = when (kind) {
        DailyReminderKind.MORNING_ADHKAR -> 7
        DailyReminderKind.EVENING_ADHKAR -> 17
        DailyReminderKind.NIGHT_PRAYER -> 23
        else -> 20
    }

    private fun defaultInterval(kind: DailyReminderKind): Int = when (kind) {
        DailyReminderKind.SALAWAT -> 3
        DailyReminderKind.SUNNAH, DailyReminderKind.HADITH -> 24
        DailyReminderKind.NIGHT_PRAYER -> 24
        else -> 24
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyReminderScheduler.restore(context)
            return
        }
        val kind = DailyReminderKind.entries.firstOrNull {
            intent?.action == "com.yaqeen.app.DAILY_${it.key.uppercase()}"
        } ?: return
        DailyReminderScheduler.deliver(context, kind)
        DailyReminderScheduler.schedule(context, kind)
    }
}
