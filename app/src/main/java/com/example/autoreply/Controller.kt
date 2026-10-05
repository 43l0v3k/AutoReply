package com.example.autoreply

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

// единая точка включения/выключения: приложение, плитка, виджет, DND
object Controller {
    fun setEnabled(ctx: Context, on: Boolean, fromDnd: Boolean = false) {
        val prefs = Prefs(ctx)
        if (prefs.enabled != on) prefs.enabled = on
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
}
