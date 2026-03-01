package com.mimicease.service

class DwellClickController(
    private val actionExecutor: ActionExecutor,
    var thresholdPixel: Float = 30f,    // Distance before resetting dwell
    var dwellDurationMs: Long = 1500L,   // Amount of time to trigger a click
    var clickAction: com.mimicease.domain.model.Action = com.mimicease.domain.model.Action.TapAtCursor
) {

    private var lastX = -1f
    private var lastY = -1f
    private var startDwellTimeMs = 0L
    private var isDwelling = false
    
    // To prevent rapid repeated clicks at the same location
    private var justClicked = false
    private var postClickDelayEndMs = 0L
    private val postClickCooldownMs = 1000L

    /**
     * Call this with the new cursor position every frame.
     * @param x Current absolute X
     * @param y Current absolute Y
     * @param currentTimeMs Current system uptime
     * @return Float Progress from 0.0f to 1.0f towards
     */
    fun update(x: Float, y: Float, currentTimeMs: Long): Float {
        // Handle cooldown after a click
        if (justClicked) {
            if (currentTimeMs >= postClickDelayEndMs) {
                justClicked = false
            } else {
                 return 1.0f // Keep progress full while cooling down
            }
        }

        // Init
        if (lastX == -1f) {
            lastX = x
            lastY = y
            startDwellTimeMs = currentTimeMs
            isDwelling = true
            return 0f
        }

        // Calculate distance from start
        val dx = x - lastX
        val dy = y - lastY
        val distSq = dx * dx + dy * dy

        if (distSq > thresholdPixel * thresholdPixel) {
            // Moved outside the threshold radius -> Reset dwell
            lastX = x
            lastY = y
            startDwellTimeMs = currentTimeMs
            isDwelling = true
            return 0f
        }

        // Still inside radius
        if (isDwelling) {
            val elapsed = currentTimeMs - startDwellTimeMs
            if (elapsed >= dwellDurationMs) {
                // Trigger Click!
                actionExecutor.execute(clickAction)
                
                // Reset states
                isDwelling = false
                justClicked = true
                postClickDelayEndMs = currentTimeMs + postClickCooldownMs
                lastX = x
                lastY = y
                return 1.0f
            } else {
                return (elapsed.toFloat() / dwellDurationMs.toFloat()).coerceIn(0f, 1f)
            }
        }

        return 0f
    }

    /**
     * Resets current dwell timer manually (e.g., if a gesture click occurs)
     */
    fun reset(currentTimeMs: Long) {
        startDwellTimeMs = currentTimeMs
        justClicked = false
    }
}
