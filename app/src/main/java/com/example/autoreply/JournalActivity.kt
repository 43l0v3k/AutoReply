package com.example.autoreply

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class JournalActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val list = TextView(this).apply { setPadding(0, pad, 0, 0) }
        fun refresh() {
            val lines = prefs.journalLines()
            list.text = if (lines.isEmpty()) "Пока пусто"
            else lines.joinToString("\n\n") { Prefs.describe(it) }
        }

        val clear = Button(this).apply {
            text = "Очистить"
            setOnClickListener { prefs.clearJournal(); refresh() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(clear)
            addView(list)
        }
        refresh()
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }
}
