package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val STEP = 1f / 60f

class BossHealthHandlerTest : DescribeSpec({

    fun handler(): BossHealthHandler {
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 8, 2)
        val assets = mockk<AssetManager>(relaxed = true)
        every { assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        val game = mockk<MegamanMaverickGame>(relaxed = true)
        every { game.assets } returns assets
        return BossHealthHandler(game)
    }

    fun boss(phases: Int) = mockk<AbstractBoss>(relaxed = true).also {
        every { it.healthPhases } returns phases
        every { it.healthPhase } returns 1
        every { it.healthBarRegions } returns
            if (phases == 1) gdxArrayOf(ConstKeys.STANDARD) else gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)
        every { it.getMaxHealth() } returns ConstVals.MAX_HEALTH
        every { it.getCurrentHealth() } returns ConstVals.MAX_HEALTH
    }

    fun runFill(phases: Int) {
        val handler = handler()
        var started = 0
        var ended = 0
        handler.set(boss(phases), { started++ }, { ended++ })
        val fill = ConstVals.MAX_HEALTH * phases * ConstVals.DUR_PER_BIT
        handler.finished shouldBe false
        var time = 0f
        while (time + STEP < fill - STEP) {
            handler.update(STEP)
            time += STEP
        }
        withClue("not finished ${fill - time} s before the fill time of $fill s") { handler.finished shouldBe false }
        ended shouldBe 0
        repeat(3) { handler.update(STEP) }
        withClue("finished after the fill time of $fill s") { handler.finished shouldBe true }
        started shouldBe 1
        ended shouldBe 1
    }

    describe("the intro fill") {
        it("of a 1-phase boss takes max x DUR_PER_BIT") {
            runFill(phases = 1)
        }

        it("of a 2-phase boss takes max x 2 x DUR_PER_BIT, one pass per phase") {
            runFill(phases = 2)
        }
    }
})
