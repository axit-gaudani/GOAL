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

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.brand_primary) }
    private val goalBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.success) }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.track) }
    private val goalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_secondary)
        strokeWidth = 1.5f * density
    }
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
        val labelSpace = 22 * density
        val chartHeight = height - labelSpace - paddingTop
        val maxValue = maxOf(goal * 1.25f, days.maxOf { it.minutes }.toFloat())
        val slot = width.toFloat() / days.size
        val barWidth = slot * 0.5f
        val radius = 6 * density

        days.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2
            val bottom = paddingTop + chartHeight
            rect.set(cx - barWidth / 2, paddingTop.toFloat(), cx + barWidth / 2, bottom)
            canvas.drawRoundRect(rect, radius, radius, emptyPaint)
            if (d.minutes > 0) {
                val h = (d.minutes / maxValue) * chartHeight
                rect.set(cx - barWidth / 2, bottom - h, cx + barWidth / 2, bottom)
                canvas.drawRoundRect(rect, radius, radius, if (d.reachedGoal) goalBarPaint else barPaint)
            }
            canvas.drawText(Dates.weekdayShort(d.day), cx, height - 6 * density, labelPaint)
        }
        val goalY = paddingTop + chartHeight - (goal / maxValue) * chartHeight
        canvas.drawLine(0f, goalY, width.toFloat(), goalY, goalLinePaint)
    }
}
