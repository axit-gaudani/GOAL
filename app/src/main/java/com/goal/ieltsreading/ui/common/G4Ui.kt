package com.goal.ieltsreading.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.goal.ieltsreading.R

/**
 * Small drawing helpers for the word tiles used on the sentence, vocabulary and ABC screens:
 * flat rounded tiles and "raised" tiles (thin border with a thicker bottom edge).
 */
object G4Ui {

    fun dp(ctx: Context, value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, ctx.resources.displayMetrics).toInt()

    fun color(ctx: Context, @ColorRes res: Int): Int = ContextCompat.getColor(ctx, res)

    /** Rounded rectangle with an optional border. */
    fun rounded(
        ctx: Context,
        @ColorRes fill: Int,
        radiusDp: Float,
        @ColorRes stroke: Int? = null,
        strokeDp: Float = 0f
    ): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(ctx, radiusDp).toFloat()
        setColor(color(ctx, fill))
        if (stroke != null) setStroke(dp(ctx, strokeDp).coerceAtLeast(1), color(ctx, stroke))
    }

    /** White tile with a 1.5dp border and a 4dp bottom edge, so it looks like a key you can press. */
    fun raised(
        ctx: Context,
        @ColorRes fill: Int = R.color.surface,
        @ColorRes border: Int = R.color.option_stroke,
        radiusDp: Float = 14f
    ): Drawable {
        val edge = GradientDrawable().apply {
            cornerRadius = dp(ctx, radiusDp).toFloat()
            setColor(color(ctx, border))
        }
        val face = GradientDrawable().apply {
            cornerRadius = dp(ctx, radiusDp - 1.5f).toFloat()
            setColor(color(ctx, fill))
        }
        val side = dp(ctx, 1.5f)
        return LayerDrawable(arrayOf(edge, face)).apply { setLayerInset(1, side, side, side, dp(ctx, 4f)) }
    }

    /** Adds a press ripple on top of [content]. */
    fun ripple(ctx: Context, content: Drawable): Drawable =
        RippleDrawable(ColorStateList.valueOf(color(ctx, R.color.option_stroke)), content, null)

    /** A word tile button (BUILD bank, match pairs). [raisedEdge] adds space for the thicker bottom edge. */
    fun tile(
        ctx: Context,
        text: CharSequence,
        background: Drawable,
        @ColorRes textColor: Int,
        sizeSp: Float = 18f,
        @FontRes font: Int = R.font.inter_bold,
        raisedEdge: Boolean = false,
        padHDp: Float = 18f,
        padVDp: Float = 12f
    ): AppCompatButton = AppCompatButton(ctx).apply {
        this.text = text
        isAllCaps = false
        this.background = ripple(ctx, background)
        stateListAnimator = null
        setTextColor(color(ctx, textColor))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        typeface = ResourcesCompat.getFont(ctx, font)
        gravity = Gravity.CENTER
        minWidth = 0
        minimumWidth = 0
        minHeight = dp(ctx, 48f)
        minimumHeight = dp(ctx, 48f)
        val h = dp(ctx, padHDp)
        val v = dp(ctx, padVDp)
        setPadding(h, v, h, v + if (raisedEdge) dp(ctx, 3f) else 0)
    }
}
