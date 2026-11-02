package com.megaman.maverick.game.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.isLocked
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.blocks.Lift
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.projectiles.SpiderWeb
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject

private class SitePlayer(services: MegaServices) : MegaGameEntity(services) {
    override fun getType() = EntityType.SPECIAL
}

class TwoPlayerSitesTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var players: MegaPlayers
    lateinit var player0: SitePlayer
    lateinit var player1: SitePlayer

    fun player(slot: Int, x: Float): SitePlayer {
        val entity = SitePlayer(services)
        val body = Body(BodyType.DYNAMIC, x, 0f, 32f, 32f)
        body.addFixture(Fixture(body, FixtureType.FEET, GameRectangle(x, 0f, 32f, 32f)))
        entity.addComponent(BodyComponent(body))
        entity.addComponent(LocksComponent(MegaLockGroup.entries.size))
        players.register(PlayerSlot(slot), entity)
        entity.spawn(props()) shouldBe true
        return entity
    }

    fun tick(entity: MegaGameEntity) =
        entity.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(0f) }

    beforeEach {
        engine = GameEngine()
        players = MegaPlayers(2)
        services = testServices(engine = engine, players = players)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        mockkObject(EntityFactories)
        every { EntityFactories.fetch(any(), any()) } returns mockk(relaxed = true)
        player0 = player(0, 0f)
        player1 = player(1, 640f)
        engine.update(0f)
    }

    afterEach {
        engine.reset()
        unmockkObject(EntityFactories)
    }

    fun web(x: Float): SpiderWeb {
        val web = SpiderWeb(services)
        web.spawn(props(ConstKeys.ID pairTo 1, ConstKeys.POSITION pairTo Vector2(x, 16f), ConstKeys.OWNER pairTo player0))
        engine.update(0f)
        tick(web)
        return web
    }

    describe("SpiderWeb") {
        it("sticks to the player that it touches, and frees that player") {
            val spider = web(656f)

            player1.isLocked(MegaLockGroup.BEHAVIORS) shouldBe true
            player0.isLocked(MegaLockGroup.BEHAVIORS) shouldBe false

            spider.destroy()
            engine.update(0f)

            player1.isLocked(MegaLockGroup.BEHAVIORS) shouldBe false
            player1.isLocked(MegaLockGroup.MOVEMENT) shouldBe false
        }

        it("keeps a player stuck while another web holds that player, and frees a player of another web") {
            val first = web(656f)
            val second = web(656f)
            val other = web(16f)

            first.destroy()
            engine.update(0f)
            player1.isLocked(MegaLockGroup.BEHAVIORS) shouldBe true
            player0.isLocked(MegaLockGroup.BEHAVIORS) shouldBe true

            second.destroy()
            engine.update(0f)
            player1.isLocked(MegaLockGroup.BEHAVIORS) shouldBe false
            player0.isLocked(MegaLockGroup.BEHAVIORS) shouldBe true

            other.destroy()
            engine.update(0f)
            player0.isLocked(MegaLockGroup.BEHAVIORS) shouldBe false
        }
    }

    describe("Lift") {
        it("lifts for a player in slot 1 when slot 0 is away") {
            val lift = Lift(services)
            lift.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(640f, 0f, 32f, 32f)))
            engine.update(0f)
            lift.body.setCenter(656f, 16f)

            tick(lift)

            lift.body.physics.velocity.y shouldBeGreaterThan 0f
        }
    }
})
