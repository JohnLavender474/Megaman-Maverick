package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.maverick.game.engine.camera.CameraOrientation
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sorting.IComparableDrawable
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpriteMatrix
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.screens.viewports.PixelPerfectFitViewport
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType

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
    rotatable: Boolean = true,
    var initPos: Vector2 = Vector2(startX, startY).add(modelWidth / 2f, modelHeight / 2f),
    var doMove: () -> Boolean = { true }
) : Updatable, IComparableDrawable<Batch>, Resettable, IEventListener {

    companion object {
        const val TAG = "Background"
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

    val cameraDrawOffset = Vector2()

    private val rotation = if (rotatable) CameraRotation() else null
    private val orientation: CameraOrientation

    init {
        val width = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val height = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val camera = OrthographicCamera()

        orientation = CameraOrientation(camera)

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

    fun startRotation(direction: Direction, time: Float) {
        rotation?.start(direction, time)
    }

    fun immediateRotation(direction: Direction) {
        val rotation = rotation ?: return
        rotation.snap(direction)
        orientation.write(rotation.angle)
    }

    fun updateViewportSize(width: Int, height: Int) = viewport.update(width, height)

    override fun update(delta: Float) {
        GameLogger.debug(TAG, "Updating background: ${(backgroundSprites[0, 0] as GameSprite).boundingRectangle}")
        val rotation = rotation ?: return
        rotation.step(delta)
        orientation.write(rotation.angle)
    }

    override fun draw(drawer: Batch) {
        val position = viewport.camera.position
        val x = position.x
        val y = position.y
        val shifted = !cameraDrawOffset.isZero && doMove.invoke()
        if (shifted) {
            position.x += cameraDrawOffset.x * parallaxX
            position.y += cameraDrawOffset.y * parallaxY
        }

        viewport.apply()
        drawer.projectionMatrix = viewport.camera.combined
        backgroundSprites.draw(drawer)

        if (shifted) {
            position.x = x
            position.y = y
        }
    }

    override fun reset() = set(initPos.x, initPos.y)
}
