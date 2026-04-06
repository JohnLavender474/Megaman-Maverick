package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.interfaces.*
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import kotlin.math.floor

/**
 * @param steps optional number of discrete alpha steps. If null, alpha follows the timer ratio
 * continuously. If provided (must be > 0), the alpha is quantized into [steps] equal increments
 * (e.g. steps=10 → alpha advances in 0.1 increments).
 */
class Fade(private val type: FadeType, duration: Float, private val steps: Int? = DEFAULT_FADE_STEPS) :
    ITypable<FadeType>, Initializable, Updatable, Resettable, IDrawable<Batch>, IPositional,
    IDimensionable, IJustFinishable {

    init {
        require(steps == null || steps > 0) { "steps must be a positive value" }
    }

    enum class FadeType { FADE_IN, FADE_OUT }

    companion object {
        const val TAG = "Fadeout"
        const val DEFAULT_FADE_STEPS = 10
    }

    private val timer = Timer(duration).setToEnd()
    private val sprite = GameSprite()

    override fun getType() = type

    override fun init(vararg params: Any) = timer.reset()

    override fun update(delta: Float) = timer.update(delta)

    override fun reset() = init()

    override fun draw(drawer: Batch) {
        val raw = timer.getRatio()
        val ratio = if (steps != null) floor(raw * steps).toFloat() / steps else raw
        val alpha = when (type) {
            FadeType.FADE_IN -> 1f - ratio
            FadeType.FADE_OUT -> ratio
        }
        sprite.setAlpha(alpha)
        sprite.draw(drawer)
    }

    override fun isFinished() = timer.isFinished()

    override fun isJustFinished() = timer.isJustFinished()

    fun setToEnd() = timer.setToEnd()

    fun setRegion(region: TextureRegion) = sprite.setRegion(region)

    override fun setX(x: Float) {
        sprite.x = x
    }

    override fun setY(y: Float) {
        sprite.y = y
    }

    override fun getX() = sprite.x

    override fun getY() = sprite.y

    override fun getWidth() = sprite.width

    override fun getHeight() = sprite.height

    override fun setWidth(width: Float): Fade {
        sprite.setSize(width, sprite.height)
        return this
    }

    override fun setHeight(height: Float): Fade {
        sprite.setSize(sprite.width, height)
        return this
    }
}
