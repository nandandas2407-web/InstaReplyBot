package com.instareply.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyActivityChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var counts: IntArray = IntArray(7)
    private var labels: List<String> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E88E5.toInt()
    }
    private val emptyBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt()
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF424242.toInt()
        textSize = 12f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF757575.toInt()
        textSize = 11f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val density = resources.displayMetrics.density

    fun setData(newCounts: IntArray) {
        require(newCounts.size == 7) { "WeeklyActivityChart expects exactly 7 values" }
        counts = newCounts.copyOf()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        labels = (0 until 7).map {
            val label = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val labelZone = 24f * density
        val valueZone = 18f * density
        val chartTop = valueZone
        val chartBottom = h - labelZone
        val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

        val slot = w / 7f
        val barWidth = slot * 0.5f
        val radius = 6f * density

        for (i in 0 until 7) {
            val value = counts[i]
            val cx = slot * i + slot / 2f

            val barHeight = if (max > 0) {
                ((chartBottom - chartTop) * (value.toFloat() / max)).coerceAtLeast(2f * density)
            } else {
                2f * density
            }
            val top = chartBottom - barHeight

            val rect = RectF(cx - barWidth / 2f, top, cx + barWidth / 2f, chartBottom)
            canvas.drawRoundRect(rect, radius, radius, if (value > 0) barPaint else emptyBarPaint)

            if (value > 0) {
                canvas.drawText(value.toString(), cx, top - 4f * density, valuePaint)
            }
            if (i < labels.size) {
                canvas.drawText(labels[i], cx, h - 6f * density, labelPaint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (180f * density).toInt()
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(MeasureSpec.getSize(heightMeasureSpec), desired)
            else -> desired
        }
        val w = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec)
            else -> (180f * density).toInt()
        }
        setMeasuredDimension(w, h)
    }
}