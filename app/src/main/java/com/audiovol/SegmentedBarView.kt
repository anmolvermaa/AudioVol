package com.audiovol


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a row of rounded-rect segments, like the level bar in the alarm/media/
 * phone cards. Maps 1:1 onto AudioManager's discrete volume steps:
 *   maxLevel     -> audioManager.getStreamMaxVolume(streamType)
 *   currentLevel -> audioManager.getStreamVolume(streamType)
 * so each segment represents exactly one volume step (no rounding).
 */
class SegmentedBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var maxLevel: Int = 15
        set(value) {
            field = value.coerceAtLeast(1)
            currentLevel = currentLevel.coerceIn(0, field)
            invalidate()
        }

    var currentLevel: Int = 10
        set(value) {
            field = value.coerceIn(0, maxLevel)
            invalidate()
        }

    var activeColor: Int = Color.parseColor("#E8785A")
        set(value) { field = value; invalidate() }

    var inactiveColor: Int = Color.parseColor("#2A2A30")
        set(value) { field = value; invalidate() }

    var segmentSpacingDp: Float = 5f
        set(value) { field = value; invalidate() }

    var segmentCornerRadiusDp: Float = 4f
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setLevel(current: Int, max: Int) {
        maxLevel = max.coerceAtLeast(1)
        currentLevel = current.coerceIn(0, maxLevel)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val spacing = dp(segmentSpacingDp)
        val cornerRadius = dp(segmentCornerRadiusDp)

        val totalSpacing = spacing * (maxLevel - 1)
        val segmentWidth = (width - totalSpacing) / maxLevel

        var left = 0f
        for (i in 0 until maxLevel) {
            paint.color = if (i < currentLevel) activeColor else inactiveColor
            val rect = RectF(left, 0f, left + segmentWidth, height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            left += segmentWidth + spacing
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}