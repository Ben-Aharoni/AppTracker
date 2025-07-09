package com.example.apptracker.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var appDurations: List<Pair<String, Long>> = emptyList()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val colors = mutableMapOf<String, Int>()

    fun setData(data: List<Pair<String, Long>>) {
        appDurations = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (appDurations.isEmpty()) return

        val maxValue = appDurations.maxOf { it.second }.toFloat()
        val barWidth = width / (appDurations.size * 2f)
        val barSpace = barWidth

        val chartHeight = height * 0.8f
        val bottomY = height.toFloat() - 40f

        appDurations.forEachIndexed { index, (appName, duration) ->
            val left = index * (barWidth + barSpace)
            val right = left + barWidth

            val percent = duration / maxValue
            val barHeight = chartHeight * percent
            val top = bottomY - barHeight

            val color = colors.getOrPut(appName) {
                Color.rgb(Random.nextInt(60, 200), Random.nextInt(60, 200), Random.nextInt(60, 200))
            }

            barPaint.color = color

            val rectF = RectF(left, top, right, bottomY)
            canvas.drawRoundRect(rectF, 20f, 20f, barPaint)

            val seconds = (duration / 1000).coerceAtLeast(1)
            canvas.drawText("${seconds}s", left + barWidth / 2, top - 12f, textPaint)

            canvas.drawText(appName.take(6), left + barWidth / 2, bottomY + 28f, textPaint)
        }
    }
}
