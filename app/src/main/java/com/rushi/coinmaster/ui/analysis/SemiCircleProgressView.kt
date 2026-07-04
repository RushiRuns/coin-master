package com.rushi.coinmaster.ui.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SemiCircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progressRatio = 0f
    private var progressText = "0%"

    private fun isDarkTheme(): Boolean {
        return (context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkTheme()) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Green
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkTheme()) Color.WHITE else Color.BLACK
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val rectF = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // Enforce 2:1 width-to-height ratio for the semi-circle
        val height = width / 2
        setMeasuredDimension(width, height)
    }

    fun setProgress(spent: Long, allocated: Long) {
        progressRatio = if (allocated > 0L) {
            spent.toFloat() / allocated.toFloat()
        } else {
            0f
        }

        // Color coding based on budget usage thresholds
        val progressColor = when {
            progressRatio < 0.75f -> Color.parseColor("#4CAF50") // Green
            progressRatio < 1.0f -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Red
        }
        progressPaint.color = progressColor

        val percent = (progressRatio * 100).toInt()
        progressText = "$percent%"

        // Re-evaluate themes on update
        backgroundPaint.color = if (isDarkTheme()) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        textPaint.color = if (isDarkTheme()) Color.WHITE else Color.BLACK

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val strokeWidth = width * 0.12f // 12% of layout width is stroke
        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth

        val margin = strokeWidth / 2f
        rectF.set(margin, margin, width - margin, width * 2f - margin)

        // Draw arc from 180 degrees sweeping 180 degrees
        canvas.drawArc(rectF, 180f, 180f, false, backgroundPaint)

        val sweepAngle = (progressRatio.coerceAtMost(1f) * 180f)
        if (sweepAngle > 0) {
            canvas.drawArc(rectF, 180f, sweepAngle, false, progressPaint)
        }

        // Draw percentage text centered at the bottom
        textPaint.textSize = width * 0.18f
        canvas.drawText(progressText, width / 2f, height - (strokeWidth * 0.2f), textPaint)
    }
}
