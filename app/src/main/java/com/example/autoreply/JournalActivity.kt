package com.example.autoreply

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout

class JournalActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val ui = Ui(this)
        val col = ui.page(this, "Журнал ответов", back = true)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun refresh() {
            list.removeAllViews()
            val lines = prefs.journalLines()
            if (lines.isEmpty()) {
                list.addView(
                    ui.tv("Пока пусто.\nЗдесь появятся последние автоответы: время, мессенджер и имя.", 14.5f, ui.dim)
                        .apply { gravity = Gravity.CENTER; setPadding(0, ui.dp(48), 0, ui.dp(48)) }
                )
                return
            }
            for (l in lines) {
                val p = l.split("\t")
                val view = if (p.size >= 3) {
                    val time = Prefs.describe(l).substringBefore(" · ")
                    ui.card(
                        ui.tv(p[2], 16f, ui.fg, true),
                        ui.tv("${p[1]} · $time", 13f, ui.dim).apply { setPadding(0, ui.dp(3), 0, 0) }
                    )
                } else ui.card(ui.tv(l, 14f))
                list.addView(view, ui.lp(bottom = 8))
            }
        }

        col.addView(list)
        col.addView(ui.button("Очистить журнал", Ui.Kind.DANGER) { prefs.clearJournal(); refresh() }, ui.lp(top = 8))
        refresh()
    }
}
