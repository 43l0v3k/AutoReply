package com.example.autoreply

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var ui: Ui
    private lateinit var textInput: EditText
    private lateinit var cooldownInput: EditText

    private lateinit var hero: LinearLayout
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var heroBtn: TextView
    private lateinit var accessDot: View
    private lateinit var accessText: TextView
    private lateinit var accessBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        ui = Ui(this)
        val col = ui.page(this, "AutoReply", back = false)

        // ---------- главная карточка: включить / выключить ----------
        heroTitle = ui.tv("", 24f, Color.WHITE, true)
        heroSub = ui.tv("", 14f, 0xCCFFFFFF.toInt())
        heroBtn = ui.tv("⏻", 26f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ui.dp(60), ui.dp(60))
        }
        val heroTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(heroTitle)
            addView(heroSub.apply { setPadding(0, ui.dp(4), 0, 0) })
        }
        hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ui.dp(22), ui.dp(22), ui.dp(18), ui.dp(22))
            addView(heroTexts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(heroBtn)
            isClickable = true
            setOnClickListener {
                val now = !prefs.enabled
                Controller.setEnabled(this@MainActivity, now)
                if (now) askNotificationPermission()
                renderHero()
            }
        }
        col.addView(hero, ui.lp(top = 4))

        // ---------- доступ к уведомлениям ----------
        accessDot = ui.dot(ui.dim)
        accessText = ui.tv("", 14f)
        accessBtn = ui.button("Выдать доступ", Ui.Kind.PRIMARY) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        val accessRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(accessDot)
            addView(accessText)
        }
        col.addView(ui.card(accessRow, accessBtn.apply {
            layoutParams = ui.lp(top = 12)
        }), ui.lp(top = 12))

        // ---------- быстрые переходы ----------
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ui.button("Настройки") {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = ui.dp(6) })
            addView(ui.button("Журнал") {
                startActivity(Intent(this@MainActivity, JournalActivity::class.java))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = ui.dp(6) })
        }
        col.addView(nav, ui.lp(top = 12))

        // ---------- мессенджеры ----------
        col.addView(ui.section("Мессенджеры"), ui.lp(top = 24, bottom = 8))
        val appsCard = ui.card()
        Apps.ALL.forEachIndexed { i, app ->
            if (i > 0) appsCard.addView(ui.divider())
            appsCard.addView(ui.toggle(app.title, null, prefs.isAppOn(app.title)) { prefs.setAppOn(app.title, it) }.view)
        }
        col.addView(appsCard)

        // ---------- текст ответа ----------
        col.addView(ui.section("Текст ответа"), ui.lp(top = 24, bottom = 8))
        textInput = ui.input(prefs.replyText, multiline = true)
        col.addView(textInput)

        col.addView(ui.hint("Шаблоны: нажми, чтобы применить. Удерживай, чтобы сохранить в шаблон текст из поля выше."))
        for (i in 0 until Prefs.TEMPLATE_COUNT) {
            val t = ui.tv(prefs.template(i), 14.5f).apply {
                maxLines = 3
                setPadding(ui.dp(16), ui.dp(13), ui.dp(16), ui.dp(13))
                background = ui.ripple(ui.shape(ui.card, 16, if (ui.dark) null else ui.line))
                isClickable = true
                setOnClickListener { textInput.setText(prefs.template(i)) }
                setOnLongClickListener {
                    val s = textInput.text.toString()
                    if (s.isNotBlank()) {
                        prefs.setTemplate(i, s)
                        text = s
                        Toast.makeText(this@MainActivity, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
            col.addView(t, ui.lp(top = 8))
        }

        // ---------- частота ----------
        col.addView(ui.section("Частота"), ui.lp(top = 24, bottom = 8))
        cooldownInput = ui.input(prefs.cooldownMin.toString(), number = true)
        col.addView(cooldownInput)
        col.addView(ui.hint("Не отвечать одному человеку чаще, чем раз в N минут."))

        // ---------- низ ----------
        col.addView(ui.tv(
            "WhatsApp и Instagram — продукты компании Meta, признанной экстремистской в РФ и запрещённой на территории России.",
            12f, ui.dim
        ), ui.lp(top = 28))

        col.addView(TextView(this).apply {
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
            textSize = 12.5f
            setTextColor(ui.dim)
            setLinkTextColor(ui.accent)
            movementMethod = LinkMovementMethod.getInstance()
            gravity = Gravity.CENTER
            setPadding(0, ui.dp(16), 0, 0)
        })
    }

    private fun renderHero() {
        val on = prefs.enabled
        hero.background = if (on) ui.gradient(ui.accent, ui.accent2, 26)
        else ui.shape(ui.card, 26, if (ui.dark) null else ui.line)
        heroTitle.text = if (on) "Автоответчик включён" else "Автоответчик выключен"
        heroTitle.setTextColor(if (on) Color.WHITE else ui.fg)
        heroSub.text = if (on) "Отвечено: ${prefs.count} раз" else "Нажми кнопку справа, чтобы включить"
        heroSub.setTextColor(if (on) 0xCCFFFFFF.toInt() else ui.dim)
        heroBtn.setTextColor(if (on) Color.WHITE else ui.dim)
        heroBtn.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (on) 0x33FFFFFF else ui.field)
        }
    }

    private fun renderAccess() {
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        accessDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (granted) ui.good else ui.bad)
        }
        accessText.text = if (granted) "Доступ к уведомлениям есть" else "Нет доступа к уведомлениям — без него не работает"
        accessBtn.visibility = if (granted) View.GONE else View.VISIBLE
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
        renderHero()
        renderAccess()
        // запускаем слежение за звонками и без доступа к уведомлениям
        if (prefs.callAnswer) CallHandler.start(this)
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
