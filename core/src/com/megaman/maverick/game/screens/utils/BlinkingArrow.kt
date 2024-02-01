package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.engine.drawables.IDrawable
import com.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset

class BlinkingArrow(assMan: AssetManager, val center: Vector2) : Updatable, IDrawable<Batch> {

    companion object {
        private const val BLINK_DUR = 0.2f
    }

    private val blinkTimer = Timer(BLINK_DUR)
    private var arrowSprite: Sprite
    private var arrowVisible = false

    init {
        arrowSprite = Sprite(assMan.getTextureRegion(TextureAsset.UI_1.source, "Arrow"))
        arrowSprite.setSize(ConstVals.PPM / 2f)
        arrowSprite.setCenter(center.x, center.y)
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
