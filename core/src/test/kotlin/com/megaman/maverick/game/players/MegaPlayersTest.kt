package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.polling.IControllerPoller
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
import com.megaman.maverick.game.input.InputFrame
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
    var spawnedWith: Properties? = null

    override fun getFocusBounds() = bounds

    override fun getFocusPosition(): Vector2 = bounds.getCenter(Vector2())

    override fun spawn(spawnProps: Properties): Boolean {
        spawnedWith = spawnProps
        return true
    }

    var deviceSwitches = 0
    var playerLight = PlayerLight.STANDARD

    init {
        putProperty(PlayerKeys.GRAVITY_DEVICE_SWITCH) { deviceSwitches++ }
        putProperty(PlayerKeys.LIGHT) { playerLight }
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

    fun playerEntity(
        damageTimer: Timer = Timer(1f).setToEnd(),
        shootAnimTimer: Timer = Timer(1f).setToEnd()
    ) = TestPlayerEntity(game).also {
        it.addComponent(BodyComponent(Body(BodyType.DYNAMIC, 0f, 0f, 2f, 2f)))
        it.addComponent(BehaviorsComponent())
        it.addComponent(PlayerStateComponent(damageTimer, shootAnimTimer))
        it.addComponent(PointsComponent().also { points -> points.putPoints(ConstKeys.HEALTH, Points(0, 10, 10)) })
    }

    describe("MegaPlayer") {
        it("fails to register an entity without a body component") {
            val e = shouldThrow<MissingComponentException> { MegaPlayers().register(PlayerSlot(0), PlainEntity()) }

            e.type shouldBe BodyComponent::class
        }

        it("fails to register an entity without a player state component") {
            val entity = TestPlayerEntity(game).also {
                it.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
                it.addComponent(BehaviorsComponent())
            }

            val e = shouldThrow<MissingComponentException> { MegaPlayers().register(PlayerSlot(0), entity) }

            e.type shouldBe PlayerStateComponent::class
        }

        it("fails to register an entity that is not directional") {
            val entity = PlainEntity().also {
                it.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
                it.addComponent(BehaviorsComponent())
                it.addComponent(PlayerStateComponent(Timer(1f), Timer(1f)))
            }

            shouldThrow<IllegalArgumentException> { MegaPlayers().register(PlayerSlot(0), entity) }
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

            player.body shouldBeSameInstanceAs newBody
        }

        it("reads direction, dead, ready and damaged from the entity and its player state, and writes ready") {
            val damageTimer = Timer(1f)
            val entity = playerEntity(damageTimer = damageTimer)
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            entity.direction = Direction.LEFT
            entity.dead = false
            entity.getComponent(PlayerStateComponent::class)!!.ready = true

            player.direction shouldBe Direction.LEFT
            player.dead shouldBe false
            player.ready shouldBe true
            player.damaged shouldBe true

            damageTimer.setToEnd()

            player.damaged shouldBe false

            player.ready = false

            entity.getComponent(PlayerStateComponent::class)!!.ready shouldBe false
        }

        it("reads and writes facing, direction and canBeDamaged, and reads invincible and shooting") {
            val shootAnimTimer = Timer(1f)
            val entity = playerEntity(shootAnimTimer = shootAnimTimer)
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            player.facing = Facing.LEFT
            player.direction = Direction.DOWN
            player.canBeDamaged = false
            entity.invincible = true

            entity.facing shouldBe Facing.LEFT
            entity.direction shouldBe Direction.DOWN
            entity.getComponent(PlayerStateComponent::class)!!.canBeDamaged shouldBe false
            player.invincible shouldBe true
            player.shooting shouldBe true

            shootAnimTimer.setToEnd()

            player.shooting shouldBe false
        }

        it("reads teleporting and the light, calls the gravity device receiver, and depletes health") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)

            entity.getComponent(PlayerStateComponent::class)!!.teleporting = true
            entity.playerLight = PlayerLight.JETPACK
            player.onGravityDeviceSwitch()
            player.depleteHealth()

            player.teleporting shouldBe true
            player.light shouldBe PlayerLight.JETPACK
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

        it("gives the Megaman entity through the megaman accessor") {
            val megaman = Megaman(game)
            megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
            megaman.addComponent(BehaviorsComponent())
            megaman.addComponent(megaman.playerState)
            megaman.putProperty(PlayerKeys.GRAVITY_DEVICE_SWITCH) {}
            megaman.putProperty(PlayerKeys.LIGHT) { PlayerLight.STANDARD }

            val player = MegaPlayers().register(PlayerSlot(0), megaman)

            player.megaman shouldBeSameInstanceAs megaman
        }

        it("gives the entity as the camera focus, and spawns the entity") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)
            val props = Properties()

            player.spawn(props) shouldBe true

            player.focus shouldBeSameInstanceAs entity
            player.focus.getFocusPosition() shouldBe Vector2(2.5f, 4f)
            entity.spawnedWith shouldBeSameInstanceAs props
        }

        it("fails to register an entity that is not focusable") {
            val entity = object : MegaGameEntity(game), IDirectional, IFaceable, IDamageable, IHealthEntity {
                init {
                    putProperty(PlayerKeys.GRAVITY_DEVICE_SWITCH) {}
                    putProperty(PlayerKeys.LIGHT) { PlayerLight.STANDARD }
                    addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
                    addComponent(BehaviorsComponent())
                    addComponent(PlayerStateComponent(Timer(1f), Timer(1f)))
                }

                override var direction = Direction.UP
                override var facing = Facing.RIGHT
                override var invincible = false
                override fun getType() = EntityType.MEGAMAN
            }

            shouldThrow<IllegalArgumentException> { MegaPlayers().register(PlayerSlot(0), entity) }
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
            val poller = mockk<IControllerPoller>()
            every { poller.getStatus(any()) } returns ButtonStatus.RELEASED
            every { poller.getStatus(MegaControllerButton.A) } returns ButtonStatus.JUST_PRESSED
            val frame = InputFrame.of(poller)

            player0.input shouldBe InputFrame.RELEASED

            players.inputs[PlayerSlot(1)] = frame

            player1.input shouldBe frame
            player0.input shouldBe InputFrame.RELEASED
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
