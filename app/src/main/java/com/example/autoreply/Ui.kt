package com.example.autoreply

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Маленькая дизайн-система приложения: цвета, карточки, кнопки, строки с переключателями. */
class Ui(val ctx: Context) {
    private val density = ctx.resources.displayMetrics.density
    val dark = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val bg = if (dark) 0xFF0D1015.toInt() else 0xFFF2F4F8.toInt()
    val card = if (dark) 0xFF171B23.toInt() else 0xFFFFFFFF.toInt()
    val field = if (dark) 0xFF202531.toInt() else 0xFFEDF0F5.toInt()
    val fg = if (dark) 0xFFF1F3F7.toInt() else 0xFF14171D.toInt()
    val dim = if (dark) 0xFF8D95A5.toInt() else 0xFF667085.toInt()
    val line = if (dark) 0xFF262B37.toInt() else 0xFFE6E9F0.toInt()
    val accent = 0xFF4C8DFF.toInt()
    val accent2 = 0xFF7B5CFF.toInt()
    val good = 0xFF2FC48D.toInt()
    val bad = 0xFFF0584D.toInt()

    fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    fun shape(color: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    fun gradient(a: Int, b: Int, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    fun ripple(content: Drawable): Drawable =
        RippleDrawable(ColorStateList.valueOf(if (dark) 0x33FFFFFF else 0x22000000), content, null)

    fun tv(s: CharSequence, sp: Float = 15f, color: Int = fg, bold: Boolean = false) = TextView(ctx).apply {
        text = s
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    fun lp(top: Int = 0, bottom: Int = 0, w: Int = ViewGroup.LayoutParams.MATCH_PARENT) =
        LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    /** Экран: шапка + прокручиваемая колонка. Возвращает колонку для наполнения. */
    fun page(act: Activity, title: String, back: Boolean): LinearLayout {
        if (Build.VERSION.SDK_INT >= 21) {
            act.window.statusBarColor = bg
            act.window.navigationBarColor = bg
        }
        if (!dark) {
            var f = act.window.decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= 23) f = f or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26) f = f or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            act.window.decorView.systemUiVisibility = f
        }
        act.window.setBackgroundDrawable(shape(bg, 0))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(28))
        }
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(16), dp(8))
        }
        if (back) {
            head.addView(tv("‹", 30f, fg).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(14), dp(4))
                setOnClickListener { act.finish() }
            })
        } else {
            head.setPadding(dp(20), dp(12), dp(16), dp(8))
        }
        head.addView(tv(title, if (back) 22f else 30f, fg, true))

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(bg)
            addView(head)
            addView(ScrollView(ctx).apply {
                isFillViewport = false
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(col)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        act.setContentView(root)
        return col
    }

    fun section(s: String) = tv(s.uppercase(), 12f, dim, true).apply {
        letterSpacing = 0.08f
        setPadding(dp(6), 0, 0, 0)
    }

    fun hint(s: String) = tv(s, 12.5f, dim).apply {
        setPadding(dp(6), dp(8), dp(6), 0)
        setLineSpacing(0f, 1.15f)
    }

    fun card(vararg views: View): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(card, 20, if (dark) null else line)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        for (v in views) addView(v)
    }

    fun divider() = View(ctx).apply {
        setBackgroundColor(line)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(12); bottomMargin = dp(12)
        }
    }

    class ToggleRow(val view: View, val sw: Switch)

    fun toggle(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): ToggleRow {
        val sw = Switch(ctx).apply {
            isChecked = checked
            showText = false
            if (Build.VERSION.SDK_INT >= 23) {
                val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                thumbTintList = ColorStateList(states, intArrayOf(Color.WHITE, if (dark) 0xFFB5BCCB.toInt() else Color.WHITE))
                trackTintList = ColorStateList(states, intArrayOf(accent, if (dark) 0xFF3A4150.toInt() else 0xFFC9CFDB.toInt()))
            }
            setOnCheckedChangeListener { _, c -> onChange(c) }
        }
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(tv(title, 16f))
            if (sub != null) addView(tv(sub, 12.5f, dim).apply { setPadding(0, dp(2), 0, 0) })
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(sw)
            setOnClickListener { sw.toggle() }
        }
        return ToggleRow(row, sw)
    }

    fun input(value: String, multiline: Boolean = false, number: Boolean = false, hint: String? = null) = EditText(ctx).apply {
        setText(value)
        if (hint != null) setHint(hint)
        inputType = when {
            number -> InputType.TYPE_CLASS_NUMBER
            multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> InputType.TYPE_CLASS_TEXT
        }
        textSize = 15f
        setTextColor(fg)
        setHintTextColor(dim)
        background = shape(field, 14)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        if (multiline) minLines = 2
    }

    enum class Kind { PRIMARY, SOFT, DANGER }

    fun button(label: String, kind: Kind = Kind.SOFT, onClick: () -> Unit): TextView = TextView(ctx).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        minHeight = dp(48)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        when (kind) {
            Kind.PRIMARY -> { setTextColor(Color.WHITE); background = ripple(gradient(accent, accent2, 14)) }
            Kind.SOFT -> { setTextColor(fg); background = ripple(shape(field, 14)) }
            Kind.DANGER -> { setTextColor(bad); background = ripple(shape(field, 14)) }
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    fun dot(color: Int) = View(ctx).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply { rightMargin = dp(8) }
    }
}
