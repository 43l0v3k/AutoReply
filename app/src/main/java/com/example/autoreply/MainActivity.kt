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
            val btn = Button(this)
            btn.isAllCaps = false
            btn.text = prefs.template(i)
            btn.setOnClickListener { textInput.setText(prefs.template(i)) }
            btn.setOnLongClickListener {
                val t = textInput.text.toString()
                if (t.isNotBlank()) {
                    prefs.setTemplate(i, t)
                    btn.text = t
                    Toast.makeText(this, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                }
                true
            }
            root.addView(btn)
        }

        root.addView(label("Не отвечать одному человеку чаще, чем раз в N минут"))
        root.addView(cooldownInput)

        root.addView(Button(this).apply {
            text = "Настройки"
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        })

        root.addView(Button(this).apply {
            text = "Журнал ответов"
            setOnClickListener { startActivity(Intent(this@MainActivity, JournalActivity::class.java)) }
        })

        root.addView(TextView(this).apply {
            text = "WhatsApp и Instagram — продукты компании Meta, признанной экстремистской в РФ и запрещённой на территории России."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, pad, 0, 0)
        })

        root.addView(TextView(this).apply {
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            val sb = SpannableStringBuilder("v$version · 43l0v3k · ")
            fun link(label: String, url: String) {
                val start = sb.length
                sb.append(label)
                sb.setSpan(URLSpan(url), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            link("GitHub", GITHUB_URL)
            if (RUSTORE_URL.isNotBlank()) {
                sb.append(" · ")
                link("RuStore", RUSTORE_URL)
            }
            text = sb
            movementMethod = LinkMovementMethod.getInstance()
            gravity = Gravity.CENTER
            setPadding(0, pad * 2, 0, pad)
        })

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Notifier.update(this)
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = if (granted) "Доступ к уведомлениям: есть" else "Доступ к уведомлениям: нет, выдай"
        if (mainSwitch.isChecked != prefs.enabled) mainSwitch.isChecked = prefs.enabled
        Notifier.update(this)
    }

    override fun onPause() {
        super.onPause()
        prefs.replyText = textInput.text.toString().ifBlank { Prefs.DEFAULT_TEXT }
        prefs.cooldownMin = cooldownInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 60
    }

    companion object {
        const val GITHUB_URL = "https://github.com/43l0v3k/AutoReply"
        // ссылку на RuStore вставь сюда после модерации
        const val RUSTORE_URL = ""
    }
}
