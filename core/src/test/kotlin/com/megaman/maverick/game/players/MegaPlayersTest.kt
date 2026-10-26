package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.ConstKeys
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.points.Points
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.maverick.game.engine.input.InputFrame
import com.megaman.maverick.game.screens.levels.camera.IFocusable
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class PlainEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private class TestPlayerEntity(game: MegamanMaverickGame) : MegaGameEntity(game), IDirectional, IFaceable,
    IDamageable, IHealthEntity, IFocusable {
    private val bounds = GameRectangle(1f, 2f, 3f, 4f)

    override fun getFocusBounds() = bounds

    override fun getFocusPosition(): Vector2 = bounds.getCenter(Vector2())

    var deviceSwitches = 0
    var playerLight = PlayerLight.STANDARD

    init {
        addComponent(GravityDeviceComponent { deviceSwitches++ })
        addComponent(PlayerLightComponent { playerLight })
    }

    override var direction = Direction.UP
    override var facing = Facing.RIGHT
    override var invincible = false
    override fun getType() = EntityType.MEGAMAN
}

class MegaPlayersTest : DescribeSpec({

    lateinit var game: MegamanMaverickGame

    beforeEach {
        game = mockk(relaxed = true)
        every { game.engine } returns mockk<GameEngine>(relaxed = true)
    }

    fun playerEntity() = TestPlayerEntity(game).also {
        it.addComponent(BodyComponent(Body(BodyType.DYNAMIC, 0f, 0f, 2f, 2f)))
        it.addComponent(BehaviorsComponent())
        it.addComponent(PlayerStateComponent())
        it.addComponent(ControllerComponent())
        it.addComponent(PointsComponent().also { points -> points.putPoints(ConstKeys.HEALTH, Points(0, 10, 10)) })
    }

    describe("MegaPlayer") {
        it("registers an entity with no parts; optional reads give their empty value") {
            val player = MegaPlayers().register(PlayerSlot(0), PlainEntity())

            player.hasBody shouldBe false
            player.ready shouldBe false
            player.teleporting shouldBe false
            player.damaged shouldBe false
            player.invincible shouldBe true
            player.shooting shouldBe false
            player.lightOrNull shouldBe null
            player.isBehaviorActive("any") shouldBe false
            player.isAnyBehaviorActive("any", "other") shouldBe false
            player.onGravityDeviceSwitch()
        }

        it("reports a missing part when a caller reads a part it needs") {
            val player = MegaPlayers().register(PlayerSlot(0), PlainEntity())

            shouldThrow<MissingComponentException> { player.body }.type shouldBe BodyComponent::class
            shouldThrow<IllegalStateException> { player.direction }
            shouldThrow<IllegalStateException> { player.facing }
            shouldThrow<IllegalStateException> { player.focus }
            shouldThrow<IllegalStateException> { player.currentHealth }
            shouldThrow<MissingComponentException> { player.ready = true }
        }

        it("stores the view on its entity, and asPlayer reads it") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            entity.asPlayer shouldBeSameInstanceAs player
            TestPlayerEntity(game).asPlayer shouldBe null
            null.asPlayer shouldBe null
        }

        it("reads the body through the body component") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)
            val newBody = Body(BodyType.DYNAMIC)

            entity.getComponent(BodyComponent::class)!!.body = newBody

            player.hasBody shouldBe true
            player.body shouldBeSameInstanceAs newBody
        }

        it("reads direction, facing, dead, invincible and ready from the entity, and writes ready") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            entity.direction = Direction.LEFT
            entity.facing = Facing.LEFT
            entity.dead = false
            entity.invincible = true
            entity.getComponent(PlayerStateComponent::class)!!.ready = true

            player.direction shouldBe Direction.LEFT
            player.facing shouldBe Facing.LEFT
            player.dead shouldBe false
            player.invincible shouldBe true
            player.ready shouldBe true

            player.ready = false

            entity.getComponent(PlayerStateComponent::class)!!.ready shouldBe false
        }

        it("reads teleporting and the light, calls the gravity device receiver, and depletes health") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            entity.getComponent(PlayerStateComponent::class)!!.teleporting = true
            entity.playerLight = PlayerLight.JETPACK
            player.onGravityDeviceSwitch()
            player.depleteHealth()

            player.teleporting shouldBe true
            player.lightOrNull shouldBe PlayerLight.JETPACK
            entity.deviceSwitches shouldBe 1
            entity.getCurrentHealth() shouldBe 0
        }

        it("reads current and max health, and translates health") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            player.translateHealth(-4) shouldBe true

            player.currentHealth shouldBe 6
            player.maxHealth shouldBe 10
            entity.getCurrentHealth() shouldBe 6
        }

        it("gives the Megaman entity through the megaman accessor, and reads its shooting") {
            val megaman = Megaman(game)
            megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
            megaman.addComponent(BehaviorsComponent())
            megaman.addComponent(megaman.playerState)
            megaman.addComponent(ControllerComponent())

            val player = MegaPlayers().register(PlayerSlot(0), megaman)

            player.megaman shouldBeSameInstanceAs megaman
            megaman.shooting = true
            player.shooting shouldBe true
            megaman.shooting = false
            player.shooting shouldBe false
        }

        it("gives the entity as the camera focus") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            player.focus shouldBeSameInstanceAs entity
            player.focus.getFocusPosition() shouldBe Vector2(2.5f, 4f)
        }

        it("reads a behavior through the behaviors component") {
            val player = MegaPlayers().register(PlayerSlot(0), playerEntity())

            player.isBehaviorActive("missing") shouldBe false
        }
    }

    describe("WorldGravity") {
        it("starts up, and set stores the direction and sets it on every registered player") {
            val players = MegaPlayers(2)
            val entity0 = playerEntity()
            val entity1 = playerEntity()
            players.register(PlayerSlot(0), entity0)
            players.register(PlayerSlot(1), entity1)
            val gravity = WorldGravity(players)

            gravity.direction shouldBe Direction.UP

            gravity.set(Direction.DOWN)

            gravity.direction shouldBe Direction.DOWN
            entity0.direction shouldBe Direction.DOWN
            entity1.direction shouldBe Direction.DOWN
        }
    }

    describe("MegaPlayers") {
        it("has a capacity of 1 by default") {
            MegaPlayers().capacity shouldBe 1
        }

        it("gives each player the input frame of its slot") {
            val players = MegaPlayers(2)
            val player0 = players.register(PlayerSlot(0), playerEntity())
            val player1 = players.register(PlayerSlot(1), playerEntity())
            val frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)

            player0.input shouldBe InputFrame.RELEASED

            players.inputs[PlayerSlot(1)] = frame

            player1.input shouldBe frame
            player0.input shouldBe InputFrame.RELEASED
        }

        it("binds the slot's input source to the entity's controller component") {
            val players = MegaPlayers(2)
            val entity = playerEntity()
            val player = players.register(PlayerSlot(1), entity)
            val frame = InputFrame.RELEASED.with(MegaControllerButton.B, ButtonStatus.PRESSED)

            players.inputs[PlayerSlot(1)] = frame

            entity.getComponent(ControllerComponent::class)!!.input!!.read() shouldBe frame
            player.input shouldBe frame
        }

        it("binds no input source to an entity without a controller component") {
            val entity = playerEntity().also { it.removeComponent(ControllerComponent::class) }

            val player = MegaPlayers().register(PlayerSlot(0), entity)

            player.input shouldBe InputFrame.RELEASED
        }

        it("tells the damage signal's listeners in the order they connected, with the player and the damager") {
            val players = MegaPlayers()
            val player = players.register(PlayerSlot(0), playerEntity())
            val damager = mockk<IDamager>()
            val calls = ArrayList<String>()
            val first: (PlayerDamaged) -> Unit = {
                calls.add("first")
                it.player shouldBeSameInstanceAs player
                it.damager shouldBeSameInstanceAs damager
            }
            players.playerDamaged.connect(first)
            players.playerDamaged.connect { calls.add("second") }

            players.onPlayerDamaged(player, damager)
            players.playerDamaged.disconnect(first)
            players.onPlayerDamaged(player, damager)

            calls shouldBe listOf("first", "second", "second")
        }

        it("targetFor gives the first player with a body") {
            val players = MegaPlayers(2)
            players.register(PlayerSlot(0), PlainEntity())
            val withBody = players.register(PlayerSlot(1), playerEntity())

            players.targetFor(TestPlayerEntity(game)) shouldBeSameInstanceAs withBody
        }

        it("targetFor gives the player in slot 0, also when it is not present") {
            val players = MegaPlayers()
            val entity = playerEntity()
            val player = players.register(PlayerSlot(0), entity)

            entity.spawned = false

            players.targetFor(TestPlayerEntity(game)) shouldBeSameInstanceAs player
        }
    }
})
