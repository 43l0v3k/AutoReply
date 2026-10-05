package com.example.autoreply

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class AutoReplyWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context))
    }

    companion object {
        fun build(ctx: Context): RemoteViews {
            val prefs = Prefs(ctx)
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            if (prefs.enabled) {
                v.setTextViewText(R.id.widget_state, "Включён · ответов: ${prefs.count}")
                v.setTextColor(R.id.widget_state, 0xFF7CFC9A.toInt())
            } else {
                v.setTextViewText(R.id.widget_state, "Выключен")
                v.setTextColor(R.id.widget_state, 0xFFAAAAAA.toInt())
            }
            val click = PendingIntent.getBroadcast(
                ctx, 0, Intent(ctx, ToggleReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.widget_root, click)
            return v
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, AutoReplyWidget::class.java))
            if (ids.isNotEmpty()) mgr.updateAppWidget(ids, build(ctx))
        }
    }
}
