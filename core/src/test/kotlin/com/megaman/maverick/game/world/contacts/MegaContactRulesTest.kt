package com.megaman.maverick.game.world.contacts

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.contacts.ContactRuleTable
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify

private class RulesTestEntity(game: MegaServices) : MegaGameEntity(game), IBodyEntity, IDirectional {
    override var direction = Direction.UP
    override fun getType() = EntityType.SPECIAL
}

class MegaContactRulesTest : DescribeSpec({

    lateinit var players: MegaPlayers
    lateinit var gravity: WorldGravity
    lateinit var table: ContactRuleTable
    lateinit var services: MegaServices
    lateinit var splash: MegaGameEntity

    fun entity(): RulesTestEntity {
        val entity = RulesTestEntity(services)
        val body = Body(BodyType.DYNAMIC, 0f, 0f, 32f, 32f)
        body.putProperty(ConstKeys.PRIOR, body.getPosition(Vector2()))
        entity.addComponent(BodyComponent(body))
        return entity
    }

    fun fixture(entity: RulesTestEntity, type: FixtureType): Fixture {
        val fixture = Fixture(entity.body, type, GameRectangle(0f, 0f, 32f, 32f))
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    fun sandFixture() = Fixture(Body(BodyType.STATIC, 0f, 0f, 32f, 32f), FixtureType.SAND, GameRectangle(0f, 0f, 32f, 32f))

    fun gravityChangeFixture(direction: Direction): Fixture {
        val fixture = Fixture(
            Body(BodyType.ABSTRACT, -64f, -64f, 160f, 160f), FixtureType.GRAVITY_CHANGE, GameRectangle(0f, 0f, 160f, 160f)
        )
        fixture.putProperty(ConstKeys.DIRECTION, direction)
        return fixture
    }

    fun register(entity: RulesTestEntity, phase: PlayPhase) {
        entity.addComponent(PlayPhaseComponent(phase))
        players.register(PlayerSlot(0), entity)
    }

    beforeEach {
        players = MegaPlayers()
        gravity = WorldGravity(players)
        services = testServices(engine = mockk<GameEngine>(relaxed = true), players = players, gravity = gravity)
        table = megaContactRules(players, gravity) { _, _ -> false }
        splash = mockk(relaxed = true)
        mockkObject(EntityFactories)
        every { EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH) } returns splash
    }

    afterEach {
        unmockkObject(EntityFactories)
    }

    describe("feet on sand, begin") {
        it("spawns no splash when no player is registered") {
            val feet = fixture(entity(), FixtureType.FEET)

            table.dispatch(ProcessState.BEGIN, feet, sandFixture(), 0f)

            verify(exactly = 0) { EntityFactories.fetch(any(), any()) }
            feet.body.isSensing(BodySense.FEET_ON_SAND) shouldBe true
        }

        it("spawns no splash when the registered player is not in play") {
            register(entity(), PlayPhase.PRE_PLAY)
            val feet = fixture(entity(), FixtureType.FEET)

            table.dispatch(ProcessState.BEGIN, sandFixture(), feet, 0f)

            verify(exactly = 0) { EntityFactories.fetch(any(), any()) }
            feet.body.isSensing(BodySense.FEET_ON_SAND) shouldBe true
        }

        it("spawns one sand splash with sound at the bottom center of the feet when a player is in play") {
            register(entity(), PlayPhase.IN_PLAY)
            val feet = fixture(entity(), FixtureType.FEET)
            val props = slot<Properties>()
            every { splash.spawn(capture(props)) } returns true

            table.dispatch(ProcessState.BEGIN, feet, sandFixture(), 0f)

            verify(exactly = 1) { EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH) }
            verify(exactly = 1) { splash.spawn(any()) }
            props.captured.get(ConstKeys.POSITION, Vector2::class) shouldBe Vector2(16f, 0f)
            props.captured.get(ConstKeys.TYPE) shouldBe SplashType.SAND
            props.captured.get(ConstKeys.SOUND) shouldBe true
            feet.body.isSensing(BodySense.FEET_ON_SAND) shouldBe true
        }
    }

    describe("body in a gravity change area, begin") {
        it("sets the world gravity, and so the direction of every player, for a player") {
            val player = entity()
            register(player, PlayPhase.IN_PLAY)

            table.dispatch(ProcessState.BEGIN, fixture(player, FixtureType.BODY), gravityChangeFixture(Direction.LEFT), 0f)

            gravity.direction shouldBe Direction.LEFT
            player.direction shouldBe Direction.LEFT
        }

        it("sets only the direction of a non-player entity") {
            val player = entity()
            register(player, PlayPhase.IN_PLAY)
            val other = entity()

            table.dispatch(ProcessState.BEGIN, gravityChangeFixture(Direction.DOWN), fixture(other, FixtureType.BODY), 0f)

            other.direction shouldBe Direction.DOWN
            gravity.direction shouldBe Direction.UP
            player.direction shouldBe Direction.UP
        }
    }
})
