package com.megaman.maverick.game.drawables.ui

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.drawables.IDrawable
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import kotlin.math.max
import kotlin.math.min

class BossBitRuns {

    var lowRegion = 0
    var lowCount = 0
    var highRegion = NO_REGION
    var highCount = 0

    companion object {
        const val NO_REGION = -1
    }
}

class BossBitsBar(
    private val assMan: AssetManager,
    private val regions: Array<String>,
    private val x: Float,
    private val y: Float,
    private val phaseSupplier: () -> Int,
    private val countSupplier: () -> Int,
    private val maxSupplier: () -> Int
) : Initializable, IDrawable<Batch> {

    companion object {
        const val PHASE_2_BITS = "boss_phase_2"

        fun runs(phase: Int, phases: Int, count: Int, max: Int, out: BossBitRuns): BossBitRuns {
            val low = max(0, min(count, max))
            out.lowRegion = phase - 1
            out.lowCount = low
            out.highRegion = if (phase < phases) phase else BossBitRuns.NO_REGION
            out.highCount = if (phase < phases) max - low else 0
            return out
        }
    }

    private lateinit var blackBackground: GameSprite
    private val bitSprites = Array<Array<GameSprite>>()
    private val runs = BossBitRuns()

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return

        try {
            regions.forEach { region ->
                val sprites = Array<GameSprite>()
                for (i in 0 until ConstVals.STANDARD_MAX_STAT_BITS) {
                    val bit = GameSprite(assMan.getTextureRegion(TextureAsset.BITS.source, region))
                    bit.setSize(ConstVals.STAT_BIT_WIDTH * ConstVals.PPM, ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
                    bit.setPosition(x, y + i * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM)
                    sprites.add(bit)
                }
                bitSprites.add(sprites)
            }

            blackBackground = GameSprite(assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK))
            blackBackground.setPosition(x, y)

            initialized = true
        } catch (e: Exception) {
            throw Exception("Failed to create boss bits bar for regions=$regions", e)
        }
    }

    override fun draw(drawer: Batch) {
        val max = min(maxSupplier(), ConstVals.STANDARD_MAX_STAT_BITS)
        blackBackground.setSize(
            ConstVals.STAT_BIT_WIDTH * ConstVals.PPM,
            max * ConstVals.STAT_BIT_HEIGHT * ConstVals.PPM
        )
        blackBackground.draw(drawer)

        runs(phaseSupplier(), regions.size, countSupplier(), max, runs)
        val low = bitSprites[runs.lowRegion]
        for (i in 0 until runs.lowCount) low[i].draw(drawer)
        if (runs.highRegion != BossBitRuns.NO_REGION) {
            val high = bitSprites[runs.highRegion]
            for (i in runs.lowCount until runs.lowCount + runs.highCount) high[i].draw(drawer)
        }
    }
}
