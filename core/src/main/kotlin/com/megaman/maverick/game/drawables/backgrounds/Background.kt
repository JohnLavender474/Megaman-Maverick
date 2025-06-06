package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpriteMatrix
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.screens.viewports.PixelPerfectFitViewport
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera

open class Background(
    val game: MegamanMaverickGame,
    var key: String,
    startX: Float,
    startY: Float,
    model: TextureRegion,
    modelWidth: Float,
    modelHeight: Float,
    rows: Int,
    columns: Int,
    priority: DrawingPriority = DrawingPriority(DrawingSection.BACKGROUND, 0),
    var parallaxX: Float = ConstVals.DEFAULT_PARALLAX_X,
    var parallaxY: Float = ConstVals.DEFAULT_PARALLAX_Y,
    var rotatable: Boolean = true,
    var initPos: Vector2 = Vector2(startX, startY).add(modelWidth / 2f, modelHeight / 2f),
    var doMove: () -> Boolean = { true }
) : Updatable, IComparableDrawable<Batch>, Resettable, IEventListener {

    companion object {
        const val TAG = "Background"
        private const val PRINT_CAM_DEBUG = false
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TOGGLE_PIXEL_PERFECT)

    override var priority: DrawingPriority = priority
        set(value) {
            backgroundSprites.forEach { sprite ->
                field = value
                sprite.priority.section = value.section
                sprite.priority.value = value.value
            }
        }

    protected open val backgroundSprites: SpriteMatrix =
        SpriteMatrix(model, priority, modelWidth, modelHeight, rows, columns)

    protected var viewport: Viewport

    init {
        val width = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val height = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val camera = RotatableCamera(printDebug = PRINT_CAM_DEBUG)

        viewport = when {
            game.isPixelPerfect() -> PixelPerfectFitViewport(width, height, camera)
            else -> FitViewport(width, height, camera)
        }

        backgroundSprites.setPosition(startX, startY)
        set(initPos.x, initPos.y)
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.TOGGLE_PIXEL_PERFECT) {
            val pixelPerfect = event.getProperty(ConstKeys.VALUE, Boolean::class)!!

            val width = ConstVals.VIEW_WIDTH * ConstVals.PPM
            val height = ConstVals.VIEW_HEIGHT * ConstVals.PPM
            val camera = viewport.camera as OrthographicCamera

            viewport = when {
                pixelPerfect -> PixelPerfectFitViewport(width, height, camera)
                else -> FitViewport(width, height, camera)
            }
        }
    }

    fun set(pos: Vector2) = set(pos.x, pos.y)

    fun set(x: Float, y: Float) {
        viewport.camera.position.x = x
        viewport.camera.position.y = y
    }

    fun move(deltaX: Float, deltaY: Float) {
        if (!doMove.invoke()) return
        viewport.camera.position.x += deltaX * parallaxX
        viewport.camera.position.y += deltaY * parallaxY
    }

    fun startRotation(direction: Direction, time: Float): Boolean {
        val camera = viewport.camera
        if (!rotatable || camera !is RotatableCamera) return false
        camera.startRotation(direction, time)
        return true
    }

    fun immediateRotation(direction: Direction): Boolean {
        val camera = viewport.camera
        if (!rotatable || camera !is RotatableCamera) return false
        camera.immediateRotation(direction)
        return true
    }

    fun updateViewportSize(width: Int, height: Int) = viewport.update(width, height)

    override fun update(delta: Float) {
        GameLogger.debug(TAG, "Updating background: ${(backgroundSprites[0, 0] as GameSprite).boundingRectangle}")
        val camera = viewport.camera
        if (camera is RotatableCamera) camera.update(delta)
    }

    override fun draw(drawer: Batch) {
        viewport.apply()
        drawer.projectionMatrix = viewport.camera.combined
        backgroundSprites.draw(drawer)
    }

    override fun reset() = set(initPos.x, initPos.y)
}
