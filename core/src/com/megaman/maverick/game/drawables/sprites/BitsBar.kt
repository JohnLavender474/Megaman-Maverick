package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.Initializable
import com.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import kotlin.math.min

class BitsBar(
    private val assMan: AssetManager,
    private val bitRegion: String,
    private val x: Float,
    private val y: Float,
    private val countSupplier: () -> Int,
    private val maxSupplier: () -> Int = { STANDARD_MAX_BITS },
) : Initializable, IDrawable<Batch> {

    companion object {
        private const val STANDARD_MAX_BITS = 30
        private const val BIT_WIDTH = ConstVals.PPM / 2f
        private const val BIT_HEIGHT = ConstVals.PPM / 8f
    }

    private lateinit var blackBackground: Sprite
    private val bitSprites = Array<Sprite>()

    private var initialized = false

    override fun init() {
        if (initialized) return

        for (i in 0 until STANDARD_MAX_BITS) {
            val bit = Sprite(assMan.getTextureRegion(TextureAsset.UI_1.source, bitRegion))
            bit.setSize(BIT_WIDTH, BIT_HEIGHT)
            bit.setPosition(x, y + i * BIT_HEIGHT)
            bitSprites.add(bit)
        }

        blackBackground = Sprite(assMan.getTextureRegion(TextureAsset.COLORS.source, "Black"))
        blackBackground.setPosition(x, y)

        initialized = true
    }

    override fun draw(drawer: Batch) {
        blackBackground.setSize(BIT_WIDTH, maxSupplier() * BIT_HEIGHT)
        blackBackground.draw(drawer)

        for (i in 0 until min(countSupplier(), maxSupplier())) bitSprites.get(i).draw(drawer)
    }
}
