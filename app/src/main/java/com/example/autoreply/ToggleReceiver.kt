package com.example.autoreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Controller.setEnabled(context, !Prefs(context).enabled)
    }
}
