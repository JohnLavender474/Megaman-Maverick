package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.components.require
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

class WhiteBurstTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var burst: WhiteBurst

    fun spawn(spawnProps: Properties) {
        burst.init()
        burst.onSpawn(spawnProps)
    }

    fun update(delta: Float) = burst.require(UpdatablesComponent::class).updatables.values().forEach { it.update(delta) }

    fun explosionActive(): Boolean {
        burst.body.preProcess()
        return burst.body.getFixtures(Array<IFixture>(), FixtureType.EXPLOSION).first().isActive()
    }

    beforeEach {
        engine = mockk(relaxed = true)
        val services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 96, 32)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        burst = WhiteBurst(services)
    }

    afterEach { unmockkAll() }

    describe("a white burst") {
        it("keeps its explosion fixture and lives on with no spawn options, as for a laser") {
            spawn(props(ConstKeys.POSITION pairTo Vector2()))
            explosionActive() shouldBe true
            update(10f)
            verify(exactly = 0) { engine.destroy(burst) }
        }

        it("has no active explosion fixture when it does not collide") {
            spawn(props(ConstKeys.POSITION pairTo Vector2(), ConstKeys.COLLIDE pairTo false))
            explosionActive() shouldBe false
        }

        it("destroys itself after its duration") {
            spawn(props(ConstKeys.POSITION pairTo Vector2(), ConstKeys.DURATION pairTo 0.15f))
            update(0.1f)
            verify(exactly = 0) { engine.destroy(burst) }
            update(0.1f)
            verify(exactly = 1) { engine.destroy(burst) }
        }
    }
})
