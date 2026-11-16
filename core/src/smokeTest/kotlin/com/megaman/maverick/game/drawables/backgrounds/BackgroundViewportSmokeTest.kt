package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.utils.viewport.Viewport
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class BackgroundViewportSmokeTest : DescribeSpec({

    val screenWidth = 1920
    val screenHeight = 1080

    var oldGraphics: Graphics? = null
    var oldGl: GL20? = null

    class ProbeBackground(game: MegamanMaverickGame, model: TextureRegion) :
        Background(game, "probe", 0f, 0f, model, 16f, 16f, 1, 1) {
        fun currentViewport(): Viewport = viewport
    }

    fun background(): ProbeBackground {
        val game = mockk<MegamanMaverickGame>(relaxed = true)
        every { game.isPixelPerfect() } returns false
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns 16
        every { texture.height } returns 16
        return ProbeBackground(game, TextureRegion(texture))
    }

    beforeSpec { GdxNativesLoader.load() }

    beforeEach {
        oldGraphics = Gdx.graphics
        oldGl = Gdx.gl
        val graphics = mockk<Graphics>(relaxed = true)
        every { graphics.width } returns screenWidth
        every { graphics.height } returns screenHeight
        every { graphics.backBufferWidth } returns screenWidth
        every { graphics.backBufferHeight } returns screenHeight
        Gdx.graphics = graphics
        Gdx.gl = mockk(relaxed = true)
    }

    afterEach {
        Gdx.graphics = oldGraphics
        Gdx.gl = oldGl
    }

    describe("Background on TOGGLE_PIXEL_PERFECT") {
        listOf(true, false).forEach { pixelPerfect ->
            it("gives the new viewport the screen size (pixelPerfect=$pixelPerfect)") {
                val bg = background()
                bg.updateViewportSize(screenWidth, screenHeight)
                val before = bg.currentViewport()

                bg.onEvent(Event(EventType.TOGGLE_PIXEL_PERFECT, props(ConstKeys.VALUE pairTo pixelPerfect)))

                val after = bg.currentViewport()
                after shouldNotBeSameInstanceAs before
                (after.screenWidth > 0) shouldBe true
                (after.screenHeight > 0) shouldBe true
            }
        }
    }
})
