package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
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

private class TestPlayerEntity(game: MegamanMaverickGame) : MegaGameEntity(game), IDirectional {
    override var direction = Direction.UP
    override fun getType() = EntityType.MEGAMAN
}

class MegaPlayersTest : DescribeSpec({

    lateinit var game: MegamanMaverickGame

    beforeEach {
        game = mockk(relaxed = true)
        every { game.engine } returns mockk<GameEngine>(relaxed = true)
    }

    fun playerEntity(x: Float = 0f, damageTimer: Timer = Timer(1f).setToEnd()) = TestPlayerEntity(game).also {
        it.addComponent(BodyComponent(Body(BodyType.DYNAMIC, x, 0f, 2f, 2f)))
        it.addComponent(BehaviorsComponent())
        it.addComponent(PlayerStateComponent(damageTimer))
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
                it.addComponent(PlayerStateComponent(Timer(1f)))
            }

            shouldThrow<IllegalArgumentException> { MegaPlayers().register(PlayerSlot(0), entity) }
        }

        it("reads the body through the body component") {
            val entity = playerEntity()
            val player = MegaPlayers().register(PlayerSlot(0), entity)
            val newBody = Body(BodyType.DYNAMIC)

            entity.getComponent(BodyComponent::class)!!.body = newBody

            player.body shouldBeSameInstanceAs newBody
        }

        it("reads direction, dead, ready and damaged from the entity and its player state") {
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
        }

        it("reads a behavior through the behaviors component") {
            val player = MegaPlayers().register(PlayerSlot(0), playerEntity())

            player.isBehaviorActive("missing") shouldBe false
        }
    }

    describe("MegaPlayers") {
        it("has a capacity of 1 by default") {
            MegaPlayers().capacity shouldBe 1
        }

        it("targetFor gives the player in slot 0, also when it is not present") {
            val players = MegaPlayers()
            val entity = playerEntity()
            val player = players.register(PlayerSlot(0), entity)

            entity.spawned = false

            players.targetFor(TestPlayerEntity(game)) shouldBeSameInstanceAs player
        }

        it("nearestTo gives the player whose body center is nearest, and a tie goes to the lower slot") {
            val players = MegaPlayers(3)
            players.register(PlayerSlot(0), playerEntity(x = 10f))
            players.register(PlayerSlot(1), playerEntity(x = 2f))
            players.register(PlayerSlot(2), playerEntity(x = -2f))

            players.nearestTo(Vector2(9f, 1f))!!.slot shouldBe PlayerSlot(0)
            players.nearestTo(Vector2(1f, 1f))!!.slot shouldBe PlayerSlot(1)
        }
    }
})
