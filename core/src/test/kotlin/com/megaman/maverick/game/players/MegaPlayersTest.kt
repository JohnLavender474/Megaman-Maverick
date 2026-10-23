package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

private class TestEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private fun entityWithBody(x: Float = 0f, y: Float = 0f) = TestEntity().also {
    it.addComponent(BodyComponent(Body(BodyType.DYNAMIC, x, y, 2f, 2f)))
}

class MegaPlayersTest : DescribeSpec({

    describe("MegaPlayer") {
        it("fails to register an entity without a body component") {
            val players = MegaPlayers()

            val e = shouldThrow<MissingComponentException> { players.register(PlayerSlot(0), TestEntity()) }

            e.type shouldBe BodyComponent::class
        }

        it("reads the body through the body component") {
            val entity = entityWithBody()
            val player = MegaPlayers().register(PlayerSlot(0), entity)
            val newBody = Body(BodyType.DYNAMIC)

            entity.getComponent(BodyComponent::class)!!.body = newBody

            player.body shouldBeSameInstanceAs newBody
        }
    }

    describe("MegaPlayers") {
        it("has a capacity of 1 by default") {
            MegaPlayers().capacity shouldBe 1
        }

        it("nearestTo gives the player whose body center is nearest, and a tie goes to the lower slot") {
            val players = MegaPlayers(3)
            players.register(PlayerSlot(0), entityWithBody(x = 10f))
            players.register(PlayerSlot(1), entityWithBody(x = 2f))
            players.register(PlayerSlot(2), entityWithBody(x = -2f))

            players.nearestTo(Vector2(9f, 1f))!!.slot shouldBe PlayerSlot(0)
            players.nearestTo(Vector2(1f, 1f))!!.slot shouldBe PlayerSlot(1)
        }
    }
})
