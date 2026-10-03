package com.example.autoreply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var switch: Switch
    private lateinit var textInput: EditText
    private lateinit var cooldownInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            fitsSystemWindows = true
        }

        fun label(s: String) = TextView(this).apply {
            text = s
            setPadding(0, pad, 0, 0)
        }

        status = TextView(this)
        switch = Switch(this).apply {
            text = "Автоответчик включён"
            isChecked = prefs.enabled
            setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }
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
        root.addView(switch)
        root.addView(label("Текст ответа"))
        root.addView(textInput)
        root.addView(label("Не отвечать одному человеку чаще, чем раз в N минут"))
        root.addView(cooldownInput)

        setContentView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = if (granted) "Доступ к уведомлениям: есть" else "Доступ к уведомлениям: нет, выдай"
    }

    override fun onPause() {
        super.onPause()
        prefs.replyText = textInput.text.toString().ifBlank { Prefs.DEFAULT_TEXT }
        prefs.cooldownMin = cooldownInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 60
    }
}
