package com.example.autoreply

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CHANNEL = "status"
    private const val ID = 1

    fun update(ctx: Context) {
        val prefs = Prefs(ctx)
        val nm = NotificationManagerCompat.from(ctx)
                if (!prefs.enabled || !prefs.showNotification) {
            nm.cancel(ID)
            return
        }
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Статус автоответчика", NotificationManager.IMPORTANCE_LOW)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = "Отвечено: ${prefs.count} раз"
        val last = prefs.journalLines().take(5).joinToString("\n") { Prefs.describe(it) }
        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("AutoReply активен")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        if (last.isNotEmpty()) {
            b.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$last"))
        }
        try {
            nm.notify(ID, b.build())
        } catch (e: SecurityException) {
            // нет разрешения на уведомления
        }
    }
}
