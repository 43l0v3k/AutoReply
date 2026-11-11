package com.example.autoreply

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService

// единая точка включения/выключения: приложение, плитка, виджет, DND, таймер
object Controller {
    fun setEnabled(ctx: Context, on: Boolean, fromDnd: Boolean = false) {
        val prefs = Prefs(ctx)
        if (prefs.enabled != on) prefs.enabled = on
        updateTimer(ctx, on, prefs.timerMin)
        Notifier.update(ctx)
        AutoReplyWidget.refresh(ctx)
        try {
            TileService.requestListeningState(ctx, ComponentName(ctx, AutoReplyTile::class.java))
        } catch (e: Exception) {
            // плитка не добавлена в шторку
        }
        if (!fromDnd && prefs.dndLink) {
            AutoReplyService.instance?.syncDnd(on)
        }
    }

    // при включении ставим будильник на выключение, при выключении снимаем
    private fun updateTimer(ctx: Context, on: Boolean, minutes: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 2, Intent(ctx, TimerOffReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        if (on && minutes > 0) {
            // неточный будильник: разрешение не нужно, может сработать с небольшой задержкой
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi)
        }
    }
}
