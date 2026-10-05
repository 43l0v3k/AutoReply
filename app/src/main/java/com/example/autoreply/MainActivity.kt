package com.example.autoreply

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var mainSwitch: Switch
    private lateinit var textInput: EditText
    private lateinit var cooldownInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun label(s: String) = TextView(this).apply {
            text = s
            setPadding(0, pad, 0, 0)
        }

        status = TextView(this)
        mainSwitch = Switch(this).apply {
            text = "Автоответчик включён"
            isChecked = prefs.enabled
            setOnCheckedChangeListener { _, checked ->
                if (checked != prefs.enabled) {
                    Controller.setEnabled(this@MainActivity, checked)
                    if (checked) askNotificationPermission()
                }
            }
        }
        textInput = EditText(this).apply {
            setText(prefs.replyText)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        cooldownInput = EditText(this).apply {
            setText(prefs.cooldownMin.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val accessBtn = Button(this).apply {
            text = "Доступ к уведомлениям"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        root.addView(status)
        root.addView(accessBtn)
        root.addView(mainSwitch)

        root.addView(label("Приложения"))
        for (app in Apps.ALL) {
            root.addView(Switch(this).apply {
                text = app.title
                isChecked = prefs.isAppOn(app.title)
                setOnCheckedChangeListener { _, c -> prefs.setAppOn(app.title, c) }
            })
        }

        root.addView(label("Текст ответа"))
        root.addView(textInput)

        root.addView(label("Шаблоны: нажми, чтобы применить. Удерживай, чтобы сохранить в шаблон текст из поля выше"))
        for (i in 0 until Prefs.TEMPLATE_COUNT) {
