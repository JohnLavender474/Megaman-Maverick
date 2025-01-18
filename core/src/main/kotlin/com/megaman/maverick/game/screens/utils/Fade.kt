package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.interfaces.*
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.screens.utils.Fade.FadeType

class Fade(private val type: FadeType, duration: Float) : ITypable<FadeType>, Initializable, Updatable,
    Resettable, IDrawable<Batch>, IPositional, IDimensionable, IJustFinishable {

    enum class FadeType { FADE_IN, FADE_OUT }

    companion object {
        const val TAG = "Fadeout"
    }

    private val timer = Timer(duration).setToEnd()
    private val sprite = GameSprite()

    override fun getType() = type

    override fun init() = timer.reset()

    override fun update(delta: Float) = timer.update(delta)

    override fun reset() = init()

    override fun draw(drawer: Batch) {
        val alpha = when (type) {
            FadeType.FADE_IN -> 1f - timer.getRatio()
            FadeType.FADE_OUT -> timer.getRatio()
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
