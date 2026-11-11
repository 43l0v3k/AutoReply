package com.example.autoreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// срабатывает по таймеру и выключает автоответчик
class TimerOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).enabled) Controller.setEnabled(context, false)
    }
}
