package com.mega.game.engine.screens.viewports

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.max
import kotlin.math.min

class SemiPixelPerfectFitViewport(
    worldWidth: Float,
    worldHeight: Float,
    camera: OrthographicCamera,
    private val minScale: Float = 1f
) : FitViewport(worldWidth, worldHeight, camera) {

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        // Calculate the maximum possible scale while maintaining aspect ratio
        val scaleX = screenWidth / worldWidth
        val scaleY = screenHeight / worldHeight
        val scale = min(scaleX, scaleY)

        // Apply minimum scale constraint
        val constrainedScale = max(scale, minScale)

        // Find the nearest integer scale (both floor and ceil)
        val floorScale = max(minScale, MathUtils.floor(constrainedScale).toFloat())
        val ceilScale = max(minScale, MathUtils.ceil(constrainedScale).toFloat())

        // Choose which scale to use based on how close we are to the next integer
        val threshold = 0.7f // Adjust this value to change when we jump to the next scale
        val finalScale = if (constrainedScale - floorScale > threshold) ceilScale else floorScale

        // Calculate viewport dimensions
        val viewportWidth = (worldWidth * finalScale).toInt()
        val viewportHeight = (worldHeight * finalScale).toInt()

        // Center the viewport
        setScreenBounds(
            (screenWidth - viewportWidth) / 2,
            (screenHeight - viewportHeight) / 2,
            viewportWidth,
            viewportHeight
        )

        apply(centerCamera)
    }
}
