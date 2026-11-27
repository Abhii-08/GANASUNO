package com.abhi.ganasuno.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import kotlin.math.abs

/** The two existing Home labels share one glass surface and rotate as a unit.
 * Restore original heading layout and styling on every non-Home screen. */
class AnimatedPlaylistGreeting(private val card: LinearLayout, private val title: TextView,
                               private val subtitle: TextView, private val reduced: () -> Boolean) {
    private var active = false
    private var foreground = false
    private var front = "Now Playing"
    private var back = false
    private var cycle = 0
    private var animator: ValueAnimator? = null
    private var downX = 0f
    private var downY = 0f
    private val originalCamera = card.cameraDistance
    private val originalMaxLines = title.maxLines
    private val originalClickable = card.isClickable
    private val originalFocusable = card.isFocusable
    private val originalDescription = card.contentDescription
    private val originalBackground = card.background
    private val originalForeground = card.foreground
    private val originalPaddingStart = card.paddingStart
    private val originalPaddingTop = card.paddingTop
    private val originalPaddingEnd = card.paddingEnd
    private val originalPaddingBottom = card.paddingBottom
    private val originalMinimumHeight = card.minimumHeight
    private val originalGravity = card.gravity
    private val originalTitleHeight = title.layoutParams.height
    private val originalTitleSize = title.textSize
    private val originalSubtitleSize = subtitle.textSize
    private val originalSubtitleColors = subtitle.textColors
    private val originalSubtitleMaxLines = subtitle.maxLines
    private val originalTitleAccessibility = title.importantForAccessibility
    private val originalSubtitleAccessibility = subtitle.importantForAccessibility
    private val originalSubtitleVisibility = subtitle.visibility
    private val subtitleLabel = "Listen Now · GANASUNO"
    private val bounds = Rect()
    private val next = Runnable { scheduled = false; if (eligible()) flip(false) else resetFace() }
    private val attach = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { schedule() }
        override fun onViewDetachedFromWindow(v: View) { suspendAnimation() }
    }
    init { card.addOnAttachStateChangeListener(attach) }
    // Only the visible Home card runs the timer; the two text rows rotate together.
    private fun eligible(): Boolean = active && foreground && card.isAttachedToWindow && card.isShown && card.hasWindowFocus() && card.getGlobalVisibleRect(bounds) && ValueAnimator.areAnimatorsEnabled() && !reduced() && title.context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled != true
    fun setForeground(value: Boolean) { foreground = value; if (value) schedule() else suspendAnimation() }
    /** Called from the existing activity ticker, also notices window focus / accessibility changes. */
    fun visibilityChanged() {
        if (!active) return
        if (!eligible()) { if (scheduled || animator != null || back) suspendAnimation() } else if (animator == null && !scheduled) schedule()
    }
    fun bind(name: String) {
        if (active && name == front) return
        val first = !active
        active = true
        front = name
        suspendAnimation()
        if (first) {
            val context = card.context
            // Keep a stable two-row footprint; grow for large accessibility fonts.
            card.background = GlassFeatureStyle.surface(context, 24)
            card.foreground = GlassFeatureStyle.ripple(context, 24)
            card.minimumHeight = GlassFeatureStyle.dp(context, 94)
            card.gravity = Gravity.CENTER_VERTICAL
            card.setPaddingRelative(GlassFeatureStyle.dp(context, 12), GlassFeatureStyle.dp(context, 10),
                GlassFeatureStyle.dp(context, 12), GlassFeatureStyle.dp(context, 10))
            title.maxLines = 1
            // A constrained title height enables native TextView auto-sizing rather than
            // clipping greetings. Keep the height independent of the currently shown face.
            title.layoutParams = title.layoutParams.apply {
                height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f, title.resources.displayMetrics).toInt()
            }
            title.setAutoSizeTextTypeUniformWithConfiguration(16, 28, 1, TypedValue.COMPLEX_UNIT_SP)
            subtitle.visibility = View.VISIBLE
            subtitle.text = subtitleLabel
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            subtitle.setTextColor(GlassFeatureStyle.secondary(context))
            // Allow wrapping at narrow widths or increased system font size, not clipping.
            subtitle.maxLines = Int.MAX_VALUE
            title.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            subtitle.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            card.cameraDistance = 8000f * card.resources.displayMetrics.density
            card.setOnClickListener { flip(true) }
            card.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; false }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.x - downX; val dy = event.y - downY
                        if (abs(dx) > ViewConfiguration.get(v.context).scaledTouchSlop * 3 && abs(dx) > abs(dy)) {
                            // Cancel the normal click before consuming the swipe.
                            val cancel = MotionEvent.obtain(event); cancel.action = MotionEvent.ACTION_CANCEL
                            v.onTouchEvent(cancel); cancel.recycle(); flip(true); true
                        } else false
                    }
                    else -> false
                }
            }
        }
        schedule()
    }
    private var scheduled = false
    private fun schedule() {
        card.removeCallbacks(next); scheduled = false
        if (eligible() && animator == null) { scheduled = true; card.postDelayed(next, if (back) PlaylistGreeting.BACK_MS else PlaylistGreeting.FRONT_MS) }
    }
    private fun resetFace() { back = false; card.rotationY = 0f; if (title.text.toString() != front) title.text = front; if (card.contentDescription != "$front. $subtitleLabel") card.contentDescription = "$front. $subtitleLabel" }
    private fun suspendAnimation() {
        card.removeCallbacks(next); scheduled = false
        animator?.let { it.removeAllListeners(); it.removeAllUpdateListeners(); it.cancel() }; animator = null
        if (active) resetFace()
    }
    private fun flip(manual: Boolean) {
        if (!eligible() || animator != null) { if (animator == null) suspendAnimation(); return }
        card.removeCallbacks(next); scheduled = false
        if (manual) card.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        val toBack = !back
        val message = if (toBack) PlaylistGreeting.reverse(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), cycle++) else front
        var swapped = false
        val animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PlaylistGreeting.FLIP_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { a ->
                val progress = a.animatedValue as Float
                if (!swapped && progress >= .5f) { swapped = true; title.text = message }
                card.rotationY = PlaylistGreeting.angle(progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    animator = null; card.rotationY = 0f; title.text = message; back = toBack; schedule()
                }
            })
        }
        animator = animation; animation.start()
    }
    fun unbind() {
        if (!active) return
        suspendAnimation(); active = false
        card.setOnTouchListener(null); card.setOnClickListener(null); card.isClickable = originalClickable; card.isFocusable = originalFocusable
        card.contentDescription = originalDescription; card.cameraDistance = originalCamera
        title.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTitleSize)
        title.maxLines = originalMaxLines
        title.layoutParams = title.layoutParams.apply { height = originalTitleHeight }
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSubtitleSize)
        subtitle.setTextColor(originalSubtitleColors)
        subtitle.maxLines = originalSubtitleMaxLines
        subtitle.visibility = originalSubtitleVisibility
        title.importantForAccessibility = originalTitleAccessibility
        subtitle.importantForAccessibility = originalSubtitleAccessibility
        card.background = originalBackground
        card.foreground = originalForeground
        card.minimumHeight = originalMinimumHeight
        card.gravity = originalGravity
        card.setPaddingRelative(originalPaddingStart, originalPaddingTop, originalPaddingEnd, originalPaddingBottom)
    }
    fun dispose() { unbind(); card.removeOnAttachStateChangeListener(attach) }
}
