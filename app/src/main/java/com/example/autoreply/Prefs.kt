package com.example.autoreply

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("autoreply", Context.MODE_PRIVATE)

    // при включении счётчик ответов обнуляется
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) {
            val e = sp.edit().putBoolean("enabled", v)
            if (v) e.putInt("count", 0)
            e.apply()
        }

    var replyText: String
        get() = sp.getString("text", DEFAULT_TEXT) ?: DEFAULT_TEXT
        set(v) = sp.edit().putString("text", v).apply()

    // не отвечать одному и тому же собеседнику чаще, чем раз в N минут
    var cooldownMin: Int
        get() = sp.getInt("cooldown", 60)
        set(v) = sp.edit().putInt("cooldown", v).apply()

    var count: Int
        get() = sp.getInt("count", 0)
        set(v) = sp.edit().putInt("count", v).apply()

    // приложения
    private var disabledApps: Set<String>
        get() = sp.getStringSet("disabled_apps", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("disabled_apps", HashSet(v)).apply()

    fun isAppOn(title: String) = title !in disabledApps

    fun setAppOn(title: String, on: Boolean) {
        disabledApps = if (on) disabledApps - title else disabledApps + title
    }

    // шаблоны
    fun template(i: Int): String = sp.getString("tpl$i", DEFAULT_TEMPLATES[i]) ?: DEFAULT_TEMPLATES[i]

    fun setTemplate(i: Int, text: String) = sp.edit().putString("tpl$i", text).apply()

    // журнал: строки вида "время\tприложение\tимя"
    fun journalLines(): List<String> =
        (sp.getString("journal", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addJournal(app: String, name: String) {
        val line = "${System.currentTimeMillis()}\t${clean(app)}\t${clean(name)}"
        val lines = listOf(line) + journalLines()
        sp.edit().putString("journal", lines.take(50).joinToString("\n")).apply()
    }

    fun clearJournal() = sp.edit().putString("journal", "").apply()

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ')

    companion object {
        const val DEFAULT_TEXT = "Привет! Сейчас не могу ответить, напишу позже. (автоответ)"
        const val TEMPLATE_COUNT = 4
        val DEFAULT_TEMPLATES = listOf(
            DEFAULT_TEXT,
            "Я за рулём, отвечу как только смогу. (автоответ)",
            "Сейчас занят(а), напишу позже. (автоответ)",
            "Сплю, отвечу утром. (автоответ)"
        )

        fun describe(line: String): String {
            val p = line.split("\t")
            if (p.size < 3) return line
            val time = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
                .format(Date(p[0].toLongOrNull() ?: 0L))
            return "$time · ${p[1]} — ${p[2]}"
        }
    }
}
