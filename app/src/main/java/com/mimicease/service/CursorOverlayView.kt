package com.mimicease.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * A persistent overlay view that displays the current head mouse position 
 * and a circular progress indicator for dwell clicking.
 */
class CursorOverlayView(context: Context) : View(context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    // Cursor visuals
    private val cursorRadius = 15f
    private val strokeWidth = 5f
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Green for progress
        style = Paint.Style.STROKE
        this.strokeWidth = this@CursorOverlayView.strokeWidth
    }
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63") // Pink inner dot
        style = Paint.Style.FILL
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80000000") // Semi-transparent black outline
        style = Paint.Style.STROKE
        this.strokeWidth = this@CursorOverlayView.strokeWidth
    }

    private var currentX = 0f
    private var currentY = 0f
    private var progressRatio = 0f // 0.0 to 1.0

    // Bounding rect for drawing the arc
    private val arcRect = RectF()

    init {
        // Size of the view needs to accommodate the cursor and stroke
        val size = ((cursorRadius + strokeWidth) * 2).toInt()
        layoutParams.width = size
        layoutParams.height = size
        layoutParams.x = 0
        layoutParams.y = 0
    }

    fun show() {
        if (parent == null) {
            windowManager.addView(this, layoutParams)
        }
    }

    fun hide() {
        if (parent != null) {
            windowManager.removeView(this)
        }
    }

    /**
     * Updates the position of the cursor and the progress of the dwell click.
     * @param x Absolute X coordinate on screen
     * @param y Absolute Y coordinate on screen
     * @param progress 0.0f (no progress) to 1.0f (click triggered)
     */
    fun update(x: Float, y: Float, progress: Float) {
        currentX = x
        currentY = y
        progressRatio = progress.coerceIn(0f, 1f)
        
        // Update layout params
        // Offset by center so x,y is the center of the cursor
        layoutParams.x = (x - layoutParams.width / 2f).toInt()
        layoutParams.y = (y - layoutParams.height / 2f).toInt()
        
        if (parent != null) {
            windowManager.updateViewLayout(this, layoutParams)
        }
        invalidate() // Trigger redraw for progress
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        // Draw solid inner cursor
        canvas.drawCircle(cx, cy, cursorRadius - strokeWidth, cursorPaint)
        
        // Draw background outline for progress
        canvas.drawCircle(cx, cy, cursorRadius, backgroundPaint)

        // Draw arc for dwell progress
        if (progressRatio > 0f) {
            arcRect.set(cx - cursorRadius, cy - cursorRadius, cx + cursorRadius, cy + cursorRadius)
            canvas.drawArc(arcRect, -90f, 360f * progressRatio, false, progressPaint)
        }
    }
}
