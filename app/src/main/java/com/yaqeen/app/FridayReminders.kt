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

internal enum class FridayReminderKind(
    val action: String,
    val requestCode: Int,
    val notificationId: Int,
    val title: String,
    val message: String
) {
    KAHF(
        "com.yaqeen.app.ACTION_FRIDAY_KAHF", 1801, 1801,
        "موعد سورة الكهف",
        "حان وقت قراءة سورة الكهف. افتح المصحف من يقين."
    ),
    SALAWAT(
        "com.yaqeen.app.ACTION_FRIDAY_SALAWAT", 1802, 1802,
        "أكثر من الصلاة على النبي ﷺ",
        "قال رسول الله ﷺ: «فأكثروا عليَّ من الصلاة فيه»."
    ),
    HADITH(
        "com.yaqeen.app.ACTION_FRIDAY_HADITH", 1803, 1803,
        "حديث يوم الجمعة",
        "إن من أفضل أيامكم يوم الجمعة... فأكثروا عليَّ من الصلاة فيه."
    )
}

internal fun nextFridayAt(nowMillis: Long, hour: Int, minute: Int): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val daysUntilFriday = (Calendar.FRIDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
        add(Calendar.DAY_OF_YEAR, daysUntilFriday)
        if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 7)
    }
    return target.timeInMillis
}

internal fun nextFridaySalawatAt(
    nowMillis: Long,
    startHour: Int,
    startMinute: Int,
    intervalHours: Int
): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val safeInterval = intervalHours.coerceIn(1, 12)
    if (now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
        val next = (now.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, safeInterval) }
        val endOfFriday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis > nowMillis && next.timeInMillis < endOfFriday.timeInMillis) {
            return next.timeInMillis
        }
    }
    return nextFridayAt(nowMillis, startHour, startMinute)
}

internal object FridayReminderScheduler {
    private const val PREFS = "friday_reminders"
    private const val CHANNEL_ID = "friday_reminders"

    private fun key(kind: FridayReminderKind, suffix: String) = "${kind.name.lowercase()}_$suffix"

    fun isEnabled(context: Context, kind: FridayReminderKind): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(kind, "enabled"), false)

    fun hour(context: Context, kind: FridayReminderKind): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(kind, "hour"), 9)

    fun minute(context: Context, kind: FridayReminderKind): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(kind, "minute"), 0)

    fun intervalHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(FridayReminderKind.SALAWAT, "interval"), 3)

    fun enable(context: Context, kind: FridayReminderKind, hour: Int, minute: Int, intervalHours: Int = 3) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(key(kind, "enabled"), true)
            .putInt(key(kind, "hour"), hour.coerceIn(0, 23))
            .putInt(key(kind, "minute"), minute.coerceIn(0, 59))
            .apply()
        if (kind == FridayReminderKind.SALAWAT) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(key(kind, "interval"), intervalHours.coerceIn(1, 12)).apply()
        }
        createChannel(context)
        scheduleNext(context, kind)
    }

    fun disable(context: Context, kind: FridayReminderKind) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(key(kind, "enabled"), false).apply()
        alarmManager(context).cancel(pendingIntent(context, kind))
    }

    fun restoreEnabled(context: Context) {
        FridayReminderKind.entries.filter { isEnabled(context, it) }.forEach { scheduleNext(context, it) }
    }

    fun scheduleNext(context: Context, kind: FridayReminderKind, nowMillis: Long = System.currentTimeMillis()) {
        if (!isEnabled(context, kind)) return
        val trigger = if (kind == FridayReminderKind.SALAWAT) {
            nextFridaySalawatAt(nowMillis, hour(context, kind), minute(context, kind), intervalHours(context))
        } else {
            nextFridayAt(nowMillis, hour(context, kind), minute(context, kind))
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

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context, kind: FridayReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            kind.requestCode,
            Intent(context, FridayReminderReceiver::class.java).setAction(kind.action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "تذكيرات يوم الجمعة", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تذكيرات اختيارية بسورة الكهف والصلاة على النبي وحديث الجمعة" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun notify(context: Context, kind: FridayReminderKind) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (kind == FridayReminderKind.KAHF) putExtra(MainActivity.EXTRA_OPEN_SURAH, 18)
            putExtra(MainActivity.EXTRA_OPEN_FRIDAY_REMINDERS, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context, kind.requestCode + 100, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(kind.title)
            .setContentText(kind.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(kind.message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
    }
}

class FridayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            FridayReminderScheduler.restoreEnabled(context)
            return
        }
        val kind = FridayReminderKind.entries.firstOrNull { it.action == intent?.action } ?: return
        FridayReminderScheduler.notify(context, kind)
        FridayReminderScheduler.scheduleNext(context, kind)
    }
}
