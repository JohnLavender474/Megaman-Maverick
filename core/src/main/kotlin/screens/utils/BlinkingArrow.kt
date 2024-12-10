package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset

class BlinkingArrow(assMan: AssetManager, center: Vector2 = Vector2(), rotation: Float = 0f) : Updatable,
    IDrawable<Batch> {

    companion object {
        private const val BLINK_DUR = 0.1f
    }

    var position: Vector2
        get() = arrowSprite.getPosition()
        set(value) = arrowSprite.setPosition(value.x, value.y)
    var centerX: Float
        get() = arrowSprite.getCenter().x
        set(value) {
            arrowSprite.setCenterX(value)
        }
    var centerY: Float
        get() = arrowSprite.getCenter().y
        set(value) {
            arrowSprite.setCenterY(value)
        }
    var rotation: Float
        get() = arrowSprite.rotation
        set(value) {
            arrowSprite.rotation = value
        }

    private val blinkTimer = Timer(BLINK_DUR)
    private val arrowSprite = GameSprite(assMan.getTextureRegion(TextureAsset.UI_1.source, "Arrow"))
    private var arrowVisible = false

    init {
        arrowSprite.setSize(ConstVals.PPM / 2f)
        arrowSprite.setCenter(center.x, center.y)
        arrowSprite.setOriginCenter()
        this@BlinkingArrow.rotation = rotation
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
