package com.example.autoreply

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
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

        root.addView(hint("Плитка: потяни шторку, нажми карандаш и перетащи AutoReply. Виджет: долгое нажатие на рабочий стол, Виджеты, AutoReply."))

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }
}
