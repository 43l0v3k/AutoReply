package com.example.autoreply

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("autoreply", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var replyText: String
        get() = sp.getString("text", DEFAULT_TEXT) ?: DEFAULT_TEXT
        set(v) = sp.edit().putString("text", v).apply()

    // не отвечать одному и тому же собеседнику чаще, чем раз в N минут
    var cooldownMin: Int
        get() = sp.getInt("cooldown", 60)
        set(v) = sp.edit().putInt("cooldown", v).apply()

    companion object {
        const val DEFAULT_TEXT = "Привет! Сейчас не могу ответить, напишу позже. (автоответ)"
    }
}
