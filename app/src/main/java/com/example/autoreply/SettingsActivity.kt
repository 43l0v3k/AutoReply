package com.example.autoreply

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()

        fun hint(s: String) = TextView(this).apply {
            text = s
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, pad)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(Switch(this).apply {
            text = "Связать с «Не беспокоить»"
            isChecked = prefs.dndLink
            setOnCheckedChangeListener { _, c -> prefs.dndLink = c }
        })
        root.addView(hint("Включился режим «Не беспокоить» - включается AutoReply, и наоборот."))

        root.addView(Switch(this).apply {
            text = "Уведомление «AutoReply активен»"
            isChecked = prefs.showNotification
            setOnCheckedChangeListener { _, c ->
                prefs.showNotification = c
                Notifier.update(this@SettingsActivity)
            }
        })
        root.addView(hint("Постоянное уведомление со счётчиком ответов."))

        // --- звонки ---
        if (CallHandler.supported()) {
            callSwitch = Switch(this).apply {
                text = "Отвечать на звонки"
                isChecked = prefs.callAnswer && CallHandler.hasPermissions(this@SettingsActivity)
                setOnCheckedChangeListener { _, c ->
                    if (c && !CallHandler.hasPermissions(this@SettingsActivity)) {
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS),
                            REQ_CALLS
                        )
                    } else {
                        prefs.callAnswer = c
                        if (c) CallHandler.start(this@SettingsActivity)
                    }
                }
            }
            root.addView(callSwitch)
            root.addView(hint(
                "Когда AutoReply включён и тебе звонят, приложение через несколько секунд берёт трубку, " +
                    "проигрывает приветствие и сбрасывает. Звонящий слышит его через динамик телефона, " +
                    "качество зависит от модели. Выбор цифр («нажмите 1») Android приложениям недоступен."
            ))

            root.addView(TextView(this).apply { text = "Взять трубку через (секунд)" })
            callDelayInput = EditText(this).apply {
                setText(prefs.callDelaySec.toString())
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            root.addView(callDelayInput)
            root.addView(hint("Эти секунды телефон звонит как обычно, можно успеть ответить самому."))

            root.addView(TextView(this).apply { text = "Текст приветствия (его читает синтезатор речи)" })
            callTextInput = EditText(this).apply {
                setText(prefs.callText)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            root.addView(callTextInput)

            audioStatus = TextView(this).apply { setPadding(0, pad, 0, 0) }
            root.addView(audioStatus)
            root.addView(Button(this).apply {
                text = "Выбрать свою запись"
                isAllCaps = false
                setOnClickListener {
                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    startActivityForResult(i, REQ_AUDIO)
                }
            })
            root.addView(Button(this).apply {
                text = "Убрать запись (читать текст)"
                isAllCaps = false
                setOnClickListener {
                    prefs.callAudioUri = null
                    updateAudioStatus()
                }
            })
            root.addView(hint("Если запись выбрана, проигрывается она, а текст не используется."))
            updateAudioStatus()
        }

        root.addView(hint("Плитка: потяни шторку, нажми карандаш и перетащи AutoReply. Виджет: долгое нажатие на рабочий стол, Виджеты, AutoReply."))

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
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
        if (!CallHandler.supported()) return
        prefs.callText = callTextInput.text.toString().ifBlank { Prefs.DEFAULT_CALL_TEXT }
        prefs.callDelaySec = callDelayInput.text.toString().toIntOrNull()?.coerceIn(0, 60) ?: 10
    }

    companion object {
        private const val REQ_CALLS = 2
        private const val REQ_AUDIO = 3
    }
}
