package com.abhi.ganasuno.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Lightweight tinted glass: original category hue, layered specular sheen and original vector art.
 *  No per-card backdrop sampling, network calls, or persistent animation work. */
class GenreGlassCard(context: Context, title: String, color: Int, artwork: Int) : FrameLayout(context) {
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun blend(a: Int, b: Int, fraction: Float): Int {
        fun channel(x: Int, y: Int) = (x + (y - x) * fraction).toInt().coerceIn(0, 255)
        return Color.argb(245, channel(Color.red(a), Color.red(b)), channel(Color.green(a), Color.green(b)), channel(Color.blue(a), Color.blue(b)))
    }
    private fun rounded(colors: IntArray, direction: GradientDrawable.Orientation): GradientDrawable =
        GradientDrawable(direction, colors).apply { cornerRadius = dp(18).toFloat() }

    init {
        val base = rounded(intArrayOf(blend(color, Color.WHITE, .15f), blend(color, color, 0f), blend(color, Color.BLACK, .35f)), GradientDrawable.Orientation.TL_BR)
        val reflection = rounded(intArrayOf(0x35ffffff, 0x03ffffff, 0x00ffffff), GradientDrawable.Orientation.TOP_BOTTOM)
        val edge = rounded(intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT), GradientDrawable.Orientation.TOP_BOTTOM).apply {
            setStroke(dp(1), 0x44ffffff)
        }
        background = LayerDrawable(arrayOf(base, reflection, edge))
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        elevation = dp(2).toFloat()
        minimumHeight = dp(112)
        // Decorative art sits behind a label-protection scrim. Original category hue remains the base.
        val art = ImageView(context).apply {
            setImageResource(artwork)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = .94f
        }
        addView(art, LayoutParams(dp(98), dp(98), Gravity.END or Gravity.TOP).apply {
            marginEnd = dp(3); topMargin = dp(1)
        })
        addView(View(context).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            background = rounded(intArrayOf(0x00000000, 0x08000000, 0xB8000000.toInt()), GradientDrawable.Orientation.TOP_BOTTOM)
        }, LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT))
        val name = TextView(context).apply {
            text = title; textSize = 16f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.BOTTOM or Gravity.START
            maxLines = 2; ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(dp(2).toFloat(), 0f, 1f, 0x90000000.toInt())
            setPadding(dp(13), dp(8), dp(12), dp(13))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(name, LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        foreground = RippleDrawable(ColorStateList.valueOf(0x28ffffff), null,
            rounded(intArrayOf(Color.WHITE, Color.WHITE), GradientDrawable.Orientation.TOP_BOTTOM))
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Preserve space for the label on compact displays, without scaling text down.
        val art = getChildAt(0)
        val size = (w * .58f).toInt().coerceIn(dp(44), dp(106))
        val lp = art.layoutParams
        if (lp.width != size || lp.height != size) {
            lp.width = size; lp.height = size; art.layoutParams = lp
        }
    }
}
