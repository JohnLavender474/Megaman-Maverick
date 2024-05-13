package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.engine.drawables.IDrawable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset

class BlinkingArrow(assMan: AssetManager, _center: Vector2 = Vector2()) : Updatable, IDrawable<Batch> {

    companion object {
        private const val BLINK_DUR = 0.2f
    }

    var position: Vector2
        get() = arrowSprite.getPosition()
        set(value) = arrowSprite.setPosition(value.x, value.y)
    var center: Vector2
        get() = arrowSprite.getCenter()
        set(value) = arrowSprite.setCenter(value)

    private val blinkTimer = Timer(BLINK_DUR)
    private val arrowSprite = GameSprite(assMan.getTextureRegion(TextureAsset.UI_1.source, "Arrow"))
    private var arrowVisible = false

    init {
        arrowSprite.setSize(ConstVals.PPM / 2f)
        arrowSprite.setCenter(_center.x, _center.y)
    }

    override fun update(delta: Float) {
        blinkTimer.update(delta)
        if (blinkTimer.isFinished()) {
            arrowVisible = !arrowVisible
            blinkTimer.reset()
        }
    }

    override fun draw(drawer: Batch) {
        if (arrowVisible) arrowSprite.draw(drawer)
    }
}
