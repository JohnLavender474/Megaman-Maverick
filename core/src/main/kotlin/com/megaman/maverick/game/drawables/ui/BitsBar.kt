package com.megaman.maverick.game.drawables.ui

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.ConstKeys
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

    private lateinit var blackBackground: GameSprite
    private val bitSprites = Array<GameSprite>()

    private var initialized = false

    override fun init() {
        if (initialized) return

        try {
            for (i in 0 until ConstVals.STANDARD_MAX_STAT_BITS) {
                val bit = GameSprite(assMan.getTextureRegion(TextureAsset.BITS.source, bitRegion))
                bit.setSize(ConstVals.STAT_BIT_WIDTH * ConstVals.PPM, ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
                bit.setPosition(x, y + i * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
                bitSprites.add(bit)
            }

            blackBackground = GameSprite(assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK))
            blackBackground.setPosition(x, y)

            initialized = true
        } catch (e: Exception) {
            throw Exception("Failed to create bits bar for bitRegion=$bitRegion", e)
        }
    }

    override fun draw(drawer: Batch) {
        blackBackground.setSize(
            ConstVals.STAT_BIT_WIDTH * ConstVals.PPM,
            maxSupplier() * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM
        )
        blackBackground.draw(drawer)

        for (i in 0 until min(countSupplier(), maxSupplier())) bitSprites[i].draw(drawer)
    }
}
