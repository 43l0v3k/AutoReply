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

            // связка с «Не беспокоить» (по умолчанию включена)
    var dndLink: Boolean
        get() = sp.getBoolean("dnd_link", true)
        set(v) = sp.edit().putBoolean("dnd_link", v).apply()

    var showNotification: Boolean
        get() = sp.getBoolean("show_notification", true)
        set(v) = sp.edit().putBoolean("show_notification", v).apply()
    // --- v1.2.0: фильтры, расписание, таймер ---

    // списки хранятся строкой: слова через запятую или с новой строки
    var stopWords: String
        get() = sp.getString("stop_words", "") ?: ""
        set(v) = sp.edit().putString("stop_words", v).apply()

    var blacklist: String
        get() = sp.getString("blacklist", "") ?: ""
        set(v) = sp.edit().putString("blacklist", v).apply()

    var whitelist: String
        get() = sp.getString("whitelist", "") ?: ""
        set(v) = sp.edit().putString("whitelist", v).apply()

    // расписание: отвечать только в окне [start, end), минуты с полуночи
    var scheduleOn: Boolean
        get() = sp.getBoolean("schedule_on", false)
        set(v) = sp.edit().putBoolean("schedule_on", v).apply()

    var scheduleStart: Int
        get() = sp.getInt("schedule_start", 22 * 60)
        set(v) = sp.edit().putInt("schedule_start", v).apply()

    var scheduleEnd: Int
        get() = sp.getInt("schedule_end", 8 * 60)
        set(v) = sp.edit().putInt("schedule_end", v).apply()

    // таймер: выключить автоответчик через N минут после включения (0 - без таймера)
    var timerMin: Int
        get() = sp.getInt("timer_min", 0)
        set(v) = sp.edit().putInt("timer_min", v).apply()

    private fun parseList(s: String) =
        s.split(',', '\n', ';').map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun inSchedule(nowMin: Int = nowMinutes()): Boolean {
        if (!scheduleOn) return true
        val s = scheduleStart
        val e = scheduleEnd
        return when {
            s == e -> true
            s < e -> nowMin in s until e
            else -> nowMin >= s || nowMin < e // окно через полночь
        }
    }

    fun hasStopWord(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        val t = text.lowercase()
        return parseList(stopWords).any { t.contains(it) }
    }

    // чёрный список важнее белого; если белый список не пуст, отвечаем только ему
    fun nameAllowed(name: String): Boolean {
        val n = name.lowercase()
        if (parseList(blacklist).any { n.contains(it) }) return false
        val white = parseList(whitelist)
        return white.isEmpty() || white.any { n.contains(it) }
    }

    // звонки: автоответ голосом (Android 9+)
    var callAnswer: Boolean
        get() = sp.getBoolean("call_answer", false)
        set(v) = sp.edit().putBoolean("call_answer", v).apply()

    // через сколько секунд брать трубку
    var callDelaySec: Int
        get() = sp.getInt("call_delay", 10)
        set(v) = sp.edit().putInt("call_delay", v).apply()

    var callText: String
        get() = sp.getString("call_text", DEFAULT_CALL_TEXT) ?: DEFAULT_CALL_TEXT
        set(v) = sp.edit().putString("call_text", v).apply()

    // своя запись приветствия (uri аудиофайла), иначе читает синтезатор речи
    var callAudioUri: String?
        get() = sp.getString("call_audio", null)
        set(v) = sp.edit().putString("call_audio", v).apply()

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
        const val DEFAULT_CALL_TEXT = "Здравствуйте. Абонент сейчас не может ответить. Напишите ему в мессенджер, он ответит позже."
        const val TEMPLATE_COUNT = 4
        val DEFAULT_TEMPLATES = listOf(
            DEFAULT_TEXT,
            "Я за рулём, отвечу как только смогу. (автоответ)",
            "Сейчас занят(а), напишу позже. (автоответ)",
            "Сплю, отвечу утром. (автоответ)"
        )

        fun nowMinutes(): Int {
            val c = java.util.Calendar.getInstance()
            return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
        }

        fun fmtTime(min: Int) = "%02d:%02d".format(min / 60, min % 60)

        fun describe(line: String): String {
            val p = line.split("\t")
            if (p.size < 3) return line
            val time = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
                .format(Date(p[0].toLongOrNull() ?: 0L))
            return "$time · ${p[1]} — ${p[2]}"
        }
    }
}
