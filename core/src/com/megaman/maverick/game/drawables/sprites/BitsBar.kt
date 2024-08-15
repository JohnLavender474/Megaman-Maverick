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
    private val maxSupplier: () -> Int = { ConstVals.STANDARD_MAX_STAT_BITS },
) : Initializable, IDrawable<Batch> {

    private lateinit var blackBackground: Sprite
    private val bitSprites = Array<Sprite>()

    private var initialized = false

    override fun init() {
        if (initialized) return

        for (i in 0 until ConstVals.STANDARD_MAX_STAT_BITS) {
            val bit = Sprite(assMan.getTextureRegion(TextureAsset.UI_1.source, bitRegion))
            bit.setSize(ConstVals.STAT_BIT_WIDTH * ConstVals.PPM, ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
            bit.setPosition(x, y + i * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
            bitSprites.add(bit)
        }

        blackBackground = Sprite(assMan.getTextureRegion(TextureAsset.COLORS.source, "Black"))
        blackBackground.setPosition(x, y)

        initialized = true
    }

    override fun draw(drawer: Batch) {
        blackBackground.setSize(ConstVals.STAT_BIT_WIDTH * ConstVals.PPM, maxSupplier() * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
        blackBackground.draw(drawer)

        for (i in 0 until min(countSupplier(), maxSupplier())) bitSprites.get(i).draw(drawer)
    }
}
