package com.megaman.maverick.game.spawns

import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.enemies.TellySaucer
import com.megaman.maverick.game.entities.items.HealthTank
import com.megaman.maverick.game.entities.items.HeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CanSpawnContractTest : DescribeSpec({

    describe("TellySaucer.canSpawn") {
        val canSpawnLeft = "${ConstKeys.CAN}_${ConstKeys.SPAWN}_${ConstKeys.LEFT}"
        val canSpawnRight = "${ConstKeys.CAN}_${ConstKeys.SPAWN}_${ConstKeys.RIGHT}"

        fun saucerProps(centerX: Float, key: String) =
            props(ConstKeys.BOUNDS pairTo GameRectangle(centerX - 16f, 0f, 32f, 32f), key pairTo false)

        it("picks the side from the live player nearest the spawn, not from its own body") {
            val players = MegaPlayers(2)
            val services = testServices(players = players)
            listOf(0f, 400f).forEachIndexed { slot, x ->
                val player = TestHealthPlayer(services)
                player.body.setPosition(x, 0f)
                player.existenceState = EntityExistenceState.LIVE
                players.register(PlayerSlot(slot), player)
            }
            val saucer = TellySaucer(services)

            saucer.canSpawn(saucerProps(350f, canSpawnLeft)) shouldBe true
            saucer.canSpawn(saucerProps(350f, canSpawnRight)) shouldBe false
            saucer.canSpawn(saucerProps(50f, canSpawnLeft)) shouldBe false
            saucer.canSpawn(saucerProps(50f, canSpawnRight)) shouldBe true

            saucer.addComponent(BodyComponent(Body(BodyType.ABSTRACT, 0f, 0f, 32f, 32f)))
            saucer.canSpawn(saucerProps(350f, canSpawnLeft)) shouldBe true
            saucer.canSpawn(saucerProps(350f, canSpawnRight)) shouldBe false
        }
    }

    describe("the tank canSpawn checks") {
        it("HeartTank rejects a collected tank and writes no field") {
            val gameState = GameState().also { it.addHeartTank(MegaHeartTank.A) }
            val tank = HeartTank(testServices(gameState = gameState))

            tank.canSpawn(props(ConstKeys.VALUE pairTo "A")) shouldBe false
            tank.canSpawn(props(ConstKeys.VALUE pairTo "B")) shouldBe true
            shouldThrow<UninitializedPropertyAccessException> { tank.heartTank }
        }

        it("HealthTank rejects a collected tank and writes no field") {
            val gameState = GameState().also { it.putHealthTank(MegaHealthTank.A) }
            val tank = HealthTank(testServices(gameState = gameState))

            tank.canSpawn(props(ConstKeys.VALUE pairTo "A")) shouldBe false
            tank.canSpawn(props(ConstKeys.VALUE pairTo "B")) shouldBe true
            shouldThrow<UninitializedPropertyAccessException> { tank.healthTank }
        }
    }
})
