package com.mega.game.engine.screens.viewports

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.max
import kotlin.math.min

// Credit: https://gist.github.com/mgsx-dev/ff693b8d83e6d07f88b0aaf653407e5a
class PixelPerfectFitViewport(worldWidth: Float, worldHeight: Float, camera: OrthographicCamera) :
    FitViewport(worldWidth, worldHeight, camera) {

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        // get the min screen/world rate from width and height

        val wRate = screenWidth / worldWidth
        val hRate = screenHeight / worldHeight
        val rate = min(wRate.toDouble(), hRate.toDouble()).toFloat()

        // round it down and limit to one
        val iRate = max(1.0, MathUtils.floor(rate).toDouble()).toInt()

        // compute rounded viewport dimension
        val viewportWidth = worldWidth.toInt() * iRate
        val viewportHeight = worldHeight.toInt() * iRate

        // Center.
        setScreenBounds(
            (screenWidth - viewportWidth) / 2,
            (screenHeight - viewportHeight) / 2,
            viewportWidth,
            viewportHeight
        )

        apply(centerCamera)
    }
}
