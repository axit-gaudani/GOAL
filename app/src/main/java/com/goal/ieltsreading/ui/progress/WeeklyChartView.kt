package com.goal.ieltsreading.ui.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.DayStat
import com.goal.ieltsreading.domain.Dates

/** A small bar chart of study minutes for the last 7 days, drawn by hand on a Canvas. */
class WeeklyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var days: List<DayStat> = emptyList()
    private var goal = 15
    private val density = resources.displayMetrics.density

    // Colours from the design: green = goal met, light blue = studied but goal not met.
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.g3_studied) }
    private val goalBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.g3_success_dark) }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.track) }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 11 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    fun setData(days: List<DayStat>, goalMinutes: Int) {
        this.days = days
        this.goal = goalMinutes.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days.isEmpty()) return
        val labelSpace = 20 * density
        val chartHeight = height - labelSpace - paddingTop
        val maxValue = maxOf(goal.toFloat(), days.maxOf { it.minutes }.toFloat())
        val slot = width.toFloat() / days.size
        // Bars fill the column but are never wider than 26dp, with 8dp rounded corners.
        val barWidth = minOf(slot - 8 * density, 26 * density)
        val radius = 8 * density
        val bottom = paddingTop + chartHeight

        days.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2
            if (d.minutes > 0) {
                val h = maxOf((d.minutes / maxValue) * chartHeight, 2 * radius)
                rect.set(cx - barWidth / 2, bottom - h, cx + barWidth / 2, bottom)
                canvas.drawRoundRect(rect, radius, radius, if (d.reachedGoal) goalBarPaint else barPaint)
            } else {
                // A small grey stub keeps empty days visible.
                rect.set(cx - barWidth / 2, bottom - 4 * density, cx + barWidth / 2, bottom)
                canvas.drawRoundRect(rect, 2 * density, 2 * density, emptyPaint)
            }
            canvas.drawText(Dates.weekdayShort(d.day), cx, height - 4 * density, labelPaint)
        }
    }
}
