package com.megaman.maverick.game.entities.enemies

import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.testServices
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class StarknerAliveTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var probe: Starkner

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine, players = mockk<MegaPlayers>(relaxed = true))
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        val target = mockk<MegaGameEntity>(relaxed = true)
        every { target.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.ABSTRACT))
        every { services.players.targetFor(any()) } returns target
        probe = Starkner(services)
    }

    afterEach { engine.reset() }

    fun spawnProps(id: Int): Properties = props(ConstKeys.ID pairTo id, ConstKeys.POSITION pairTo Vector2())

    fun spawnLive(id: Int): Starkner {
        val starkner = Starkner(services)
        starkner.spawn(spawnProps(id)) shouldBe true
        engine.update(0f)
        probe.canSpawn(spawnProps(id)) shouldBe false
        return starkner
    }

    describe("the ALIVE entry of a Starkner") {
        it("is removed at once when the member destroy is called") {
            val starkner = spawnLive(101)

            starkner.destroy()

            probe.canSpawn(spawnProps(101)) shouldBe true
        }

        it("is removed at once when the engine destroys it") {
            val starkner = spawnLive(102)

            engine.destroy(starkner)

            probe.canSpawn(spawnProps(102)) shouldBe true
        }

        it("is removed when the engine resets") {
            spawnLive(103)

            engine.reset()

            probe.canSpawn(spawnProps(103)) shouldBe true
        }

        it("is removed when the engine is disposed") {
            spawnLive(104)

            engine.dispose()

            probe.canSpawn(spawnProps(104)) shouldBe true
            engine = GameEngine()
        }

        it("stays held by a successor that spawns with the same id before the next update") {
            val starkner = spawnLive(105)

            starkner.destroy()
            Starkner(services).spawn(spawnProps(105)) shouldBe true
            engine.update(0f)

            probe.canSpawn(spawnProps(105)) shouldBe false
        }

        it("is removed when a Starkner is destroyed while its spawn is queued") {
            val starkner = Starkner(services)
            starkner.spawn(spawnProps(106)) shouldBe true

            starkner.destroy()
            engine.update(0f)
            engine.update(0f)

            probe.canSpawn(spawnProps(106)) shouldBe true
        }
    }
})
