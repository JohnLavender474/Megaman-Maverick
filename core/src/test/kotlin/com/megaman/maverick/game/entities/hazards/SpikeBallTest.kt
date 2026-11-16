package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.motion.MotionComponent
import com.maverick.game.engine.motion.Pendulum
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val PPM = ConstVals.PPM.toFloat()

class SpikeBallTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
    }

    afterEach {
        engine.reset()
        unmockkAll()
    }

    fun SpikeBall.spawnAt(xTiles: Float, length: Float) {
        spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(xTiles * PPM, 10f * PPM, PPM, PPM),
                ConstKeys.LENGTH pairTo length
            )
        ) shouldBe true
        engine.update(0f)
    }

    fun SpikeBall.chainSprites(): List<GameSprite> {
        val component = getComponent(SpritesComponent::class)!!
        component.preProcess(0f)
        return (0 until component.sprites.size).mapNotNull { component.sprites["chain_$it"] }
    }

    fun SpikeBall.pendulumDefinition() = getComponent(MotionComponent::class)!!.definitions[ConstKeys.PENDULUM]!!

    describe("a pooled SpikeBall") {
        it("reuses its pendulum, its anchor and its chain sprites on the next spawn, and hides the extra links") {
            val ball = SpikeBall(services)

            ball.spawnAt(xTiles = 5f, length = 4f)
            val definition = ball.pendulumDefinition()
            val pendulum = definition.motion as Pendulum
            val anchor = pendulum.anchor
            val links = ball.chainSprites()
            links.size shouldBe 16
            links.map { it.hidden } shouldContainExactly List(16) { false }
            anchor.x shouldBe 5.5f * PPM

            ball.destroy()
            engine.update(0f)
            ball.spawnAt(xTiles = 20f, length = 3f)

            ball.pendulumDefinition() shouldBeSameInstanceAs definition
            (ball.pendulumDefinition().motion as Pendulum).anchor shouldBeSameInstanceAs anchor
            anchor.x shouldBe 20.5f * PPM
            pendulum.length shouldBe 3f * PPM
            val shorter = ball.chainSprites()
            shorter.zip(links).forEach { (now, before) -> now shouldBeSameInstanceAs before }
            shorter.map { it.hidden } shouldContainExactly List(12) { false } + List(4) { true }

            ball.destroy()
            engine.update(0f)
            ball.spawnAt(xTiles = 5f, length = 4f)

            ball.chainSprites().map { it.hidden } shouldContainExactly List(16) { false }
            pendulum.length shouldBe 4f * PPM
        }
    }
})
