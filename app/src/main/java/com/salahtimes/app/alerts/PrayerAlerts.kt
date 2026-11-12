package com.salahtimes.app.alerts

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.salahtimes.app.MainActivity
import com.salahtimes.app.data.PrayerCalculator
import com.salahtimes.app.data.Settings
import java.time.*

object PrayerAlerts {
    const val CHANNEL = "prayer_alerts"
    private val scheduled = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    fun channel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prayer time alerts with system notification sound"
        })
    }
    fun schedule(context: Context) {
        channel(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val settings = Settings(context)
        val now = ZonedDateTime.now()
        for ((index, name) in scheduled.withIndex()) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java).putExtra("prayer", name)
            val pending = PendingIntent.getBroadcast(context, 100 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if (!settings.notifications) continue
            val next = (0..2).asSequence().flatMap { offset ->
                PrayerCalculator.calculate(now.toLocalDate().plusDays(offset.toLong()), settings.latitude, settings.longitude, settings.method, settings.hanafi).asSequence()
            }.firstOrNull { it.name == name && it.time.isAfter(now) } ?: continue
            val millis = next.time.toInstant().toEpochMilli()
            if (Build.VERSION.SDK_INT >= 31 && alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
class PrayerAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: return
        PrayerAlerts.channel(context)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val note = NotificationCompat.Builder(context, PrayerAlerts.CHANNEL)
                .setSmallIcon(com.salahtimes.app.R.drawable.ic_app)
                .setContentTitle("$prayer prayer")
                .setContentText("It's time for $prayer")
                .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
            context.getSystemService(NotificationManager::class.java).notify(prayer.hashCode(), note)
        }
        PrayerAlerts.schedule(context)
    }
}
class RescheduleReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { PrayerAlerts.schedule(context) }
}
