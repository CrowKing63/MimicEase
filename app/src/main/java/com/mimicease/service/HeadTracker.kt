package com.mimicease.service

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.abs

class HeadTracker(context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    // Configurable parameters
    var sensitivityX = 2500f // pixels per radian
    var sensitivityY = 2500f // pixels per radian
    var deadzone = 0.05f     // radians (to prevent jitters when mostly still)
    var acceleration = 1.5f  // non-linear acceleration for faster larger movements
    
    // Screen bounds
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Current logical cursor position
    var currentX = 0f
        private set
    var currentY = 0f
        private set

    init {
        updateScreenDimensions()
        // Start from center
        currentX = screenWidth / 2f
        currentY = screenHeight / 2f
    }
    
    fun updateScreenDimensions() {
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    /**
     * Updates the logical cursor position based on head rotation.
     * @param yaw The yaw value in radians (positive = looking right, negative = looking left)
     * @param pitch The pitch value in radians (positive = looking up, negative = looking down)
     * @return Pair of (x, y) representing the new cursor coordinates
     */
    fun updatePosition(yaw: Float, pitch: Float): Pair<Float, Float> {
        // 1. Apply deadzone logic
        val activeYaw = applyDeadzone(yaw, deadzone)
        val activePitch = applyDeadzone(pitch, deadzone)

        // 2. Apply acceleration curve (e.g., x^1.5 while preserving sign)
        val accelYaw = applyAcceleration(activeYaw, acceleration)
        val accelPitch = applyAcceleration(activePitch, acceleration)
        
        // 3. Calculate delta movement (inverse pitch because looking UP corresponds to cursor UP which is negative Y in Android)
        val dx = accelYaw * sensitivityX
        val dy = -accelPitch * sensitivityY

        // 4. Update and clamp to screen bounds
        currentX = (currentX + dx).coerceIn(0f, screenWidth.toFloat())
        currentY = (currentY + dy).coerceIn(0f, screenHeight.toFloat())

        return Pair(currentX, currentY)
    }
    
    /**
     * Forces the cursor to the center of the screen
     */
    fun recenter() {
        updateScreenDimensions()
        currentX = screenWidth / 2f
        currentY = screenHeight / 2f
    }

    private fun applyDeadzone(value: Float, threshold: Float): Float {
        return if (abs(value) < threshold) {
            0f
        } else {
            // Smoothly ramp up from 0 after deadzone
            val sign = if (value > 0) 1f else -1f
            (abs(value) - threshold) * sign
        }
    }

    private fun applyAcceleration(value: Float, power: Float): Float {
        if (value == 0f) return 0f
        val sign = if (value > 0) 1f else -1f
        return Math.pow(abs(value).toDouble(), power.toDouble()).toFloat() * sign
    }
}
