package com.example.autoreply

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var callSwitch: Switch
    private lateinit var callDelayInput: EditText
    private lateinit var callTextInput: EditText
    private lateinit var audioStatus: TextView
    private lateinit var callStatus: TextView
    private lateinit var stopInput: EditText
    private lateinit var blackInput: EditText
    private lateinit var whiteInput: EditText
    private lateinit var timerInput: EditText
    private lateinit var startBtn: TextView
    private lateinit var endBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val ui = Ui(this)
        val col = ui.page(this, "Настройки", back = true)

        fun title(s: String) = col.addView(ui.section(s), ui.lp(top = 24, bottom = 8))
        fun label(s: String) = ui.tv(s, 14f, ui.fg, true).apply { setPadding(0, ui.dp(14), 0, ui.dp(8)) }

        // --- общее ---
        title("Общее")
        col.addView(ui.card(
            ui.toggle("Связать с «Не беспокоить»", "Включился режим — включился AutoReply, и наоборот", prefs.dndLink) {
                prefs.dndLink = it
            }.view,
            ui.divider(),
            ui.toggle("Уведомление «AutoReply активен»", "Постоянное уведомление со счётчиком ответов", prefs.showNotification) {
                prefs.showNotification = it
                Notifier.update(this)
            }.view
        ))

        // --- звонки ---
        if (CallHandler.supported()) {
            title("Звонки")
            val row = ui.toggle(
                "Отвечать на звонки",
                "Через несколько секунд берёт трубку, проигрывает приветствие и сбрасывает",
                prefs.callAnswer && CallHandler.hasPermissions(this)
            ) { c ->
                if (c && !CallHandler.hasPermissions(this)) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS),
                        REQ_CALLS
                    )
                } else {
                    prefs.callAnswer = c
                    if (c) CallHandler.start(this)
                }
            }
            callSwitch = row.sw
            col.addView(ui.card(row.view))
            col.addView(ui.hint(
                "Звонящий слышит приветствие через динамик телефона, качество зависит от модели. " +
                    "Выбор цифр («нажмите 1») Android приложениям недоступен."
            ))

            callDelayInput = ui.input(prefs.callDelaySec.toString(), number = true)
            callTextInput = ui.input(prefs.callText, multiline = true)
            audioStatus = ui.tv("", 13.5f, ui.dim).apply { setPadding(0, ui.dp(10), 0, ui.dp(10)) }
            val audioButtons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(ui.button("Своя запись") {
                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    startActivityForResult(i, REQ_AUDIO)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = ui.dp(6) })
                addView(ui.button("Убрать запись") {
                    prefs.callAudioUri = null
                    updateAudioStatus()
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = ui.dp(6) })
            }
            callStatus = ui.tv("", 12f, ui.dim).apply { setLineSpacing(0f, 1.15f) }

            col.addView(ui.card(
                label("Взять трубку через (секунд)").apply { setPadding(0, 0, 0, ui.dp(8)) },
                callDelayInput,
                ui.hint("Эти секунды телефон звонит как обычно, можно успеть ответить самому."),
                label("Текст приветствия (читает синтезатор речи)"),
                callTextInput,
                audioStatus,
                audioButtons,
                ui.hint("Если запись выбрана, проигрывается она, а текст не используется.")
            ), ui.lp(top = 12))
            updateAudioStatus()

            col.addView(ui.card(label("Проверка звонков").apply { setPadding(0, 0, 0, ui.dp(8)) }, callStatus), ui.lp(top = 12))
        }

        // --- фильтры ---
        title("Фильтры")
        stopInput = ui.input(prefs.stopWords, multiline = true)
        blackInput = ui.input(prefs.blacklist, multiline = true)
        whiteInput = ui.input(prefs.whitelist, multiline = true)
        col.addView(ui.card(
            label("Стоп-слова").apply { setPadding(0, 0, 0, ui.dp(8)) },
            stopInput,
            ui.hint("Если в сообщении есть одно из слов, автоответ не отправляется. Через запятую или с новой строки."),
            label("Чёрный список имён"),
            blackInput,
            ui.hint("Этим людям не отвечать. Достаточно части имени из уведомления."),
            label("Белый список имён"),
            whiteInput,
            ui.hint("Если список не пуст, отвечать только этим людям. Чёрный список важнее белого.")
        ))

        // --- расписание ---
        title("Расписание")
        startBtn = ui.button("") {
            TimePickerDialog(this, { _, h, m ->
                prefs.scheduleStart = h * 60 + m
                updateScheduleButtons()
            }, prefs.scheduleStart / 60, prefs.scheduleStart % 60, true).show()
        }
        endBtn = ui.button("") {
            TimePickerDialog(this, { _, h, m ->
                prefs.scheduleEnd = h * 60 + m
                updateScheduleButtons()
            }, prefs.scheduleEnd / 60, prefs.scheduleEnd % 60, true).show()
        }
        val times = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(startBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = ui.dp(6) })
            addView(endBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = ui.dp(6) })
        }
        col.addView(ui.card(
            ui.toggle("Отвечать только по расписанию", null, prefs.scheduleOn) { prefs.scheduleOn = it }.view,
            times.apply { layoutParams = ui.lp(top = 14) }
        ))
        updateScheduleButtons()
        col.addView(ui.hint("Вне этого окна автоответ молчит, даже если включён. Окно может идти через полночь."))

        // --- таймер ---
        title("Таймер")
        timerInput = ui.input(prefs.timerMin.toString(), number = true)
        col.addView(ui.card(
            label("Выключить через (минут)").apply { setPadding(0, 0, 0, ui.dp(8)) },
            timerInput,
            ui.hint("Автоответчик сам выключится через столько минут после включения. 0 — без таймера. Срабатывает с небольшой погрешностью.")
        ))

        col.addView(ui.hint("Плитка: потяни шторку, нажми карандаш и перетащи AutoReply. Виджет: долгое нажатие на рабочий стол, Виджеты, AutoReply."), ui.lp(top = 16))
    }

    override fun onResume() {
        super.onResume()
        if (!CallHandler.supported()) return
        if (!CallHandler.isListening()) CallHandler.start(this)
        updateCallStatus()
    }

    private fun updateCallStatus() {
        val head = listOf(
            "Разрешения на звонки: " + if (CallHandler.hasPermissions(this)) "есть" else "НЕТ",
            "AutoReply включён: " + if (prefs.enabled) "да" else "НЕТ",
            "Слежу за звонками: " + if (CallHandler.isListening()) "да" else "НЕТ",
            "Последние события:"
        )
        val events = prefs.callDebug.split("\n").filter { it.isNotBlank() }
            .ifEmpty { listOf("пока пусто") }
        callStatus.text = (head + events).joinToString("\n")
    }

    private fun updateScheduleButtons() {
        startBtn.text = "Начало: ${Prefs.fmtTime(prefs.scheduleStart)}"
        endBtn.text = "Конец: ${Prefs.fmtTime(prefs.scheduleEnd)}"
    }

    private fun updateAudioStatus() {
        audioStatus.text = if (prefs.callAudioUri != null) "Запись: выбрана" else "Запись: не выбрана, читает синтезатор"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_AUDIO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.callAudioUri = uri.toString()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
        }
        updateAudioStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_CALLS) return
        if (CallHandler.hasPermissions(this)) {
            prefs.callAnswer = true
            CallHandler.start(this)
        } else {
            prefs.callAnswer = false
            callSwitch.isChecked = false
            Toast.makeText(this, "Без доступа к звонкам функция не работает", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.stopWords = stopInput.text.toString()
        prefs.blacklist = blackInput.text.toString()
        prefs.whitelist = whiteInput.text.toString()
        prefs.timerMin = timerInput.text.toString().toIntOrNull()?.coerceIn(0, 1440) ?: 0
        if (!CallHandler.supported()) return
        prefs.callText = callTextInput.text.toString().ifBlank { Prefs.DEFAULT_CALL_TEXT }
        prefs.callDelaySec = callDelayInput.text.toString().toIntOrNull()?.coerceIn(0, 60) ?: 10
    }

    companion object {
        private const val REQ_CALLS = 2
        private const val REQ_AUDIO = 3
    }
}
