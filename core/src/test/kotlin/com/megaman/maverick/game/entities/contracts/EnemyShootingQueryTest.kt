package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.players.TestMegaPlayer
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private class ShootingTestPlayer(services: MegaServices, override val character: PlayerCharacter) :
    TestMegaPlayer(services) {

    override var shooting = false

    init {
        facing = Facing.LEFT
        addComponent(BodyComponent(Body(BodyType.DYNAMIC, x = 100f, width = 32f, height = 48f)))
    }
}

class EnemyShootingQueryTest : DescribeSpec({

    lateinit var players: MegaPlayers
    lateinit var enemy: AbstractEnemy

    fun playerOf(character: PlayerCharacter): ShootingTestPlayer {
        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        val player = ShootingTestPlayer(services, character)
        players.register(PlayerSlot(0), player)
        return player
    }

    beforeEach {
        players = MegaPlayers()
        enemy = mockk<AbstractEnemy>(relaxed = true)
        every { enemy.players } returns players
        every { enemy.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.DYNAMIC, width = 32f))
        every { enemy.isMegamanShootingAtMe() } answers { callOriginal() }
    }

    afterEach { unmockkAll() }

    listOf(PlayerCharacter.X, PlayerCharacter.PROTOMAN).forEach { character ->
        describe("$character") {
            it("shoots at the enemy when it shoots toward it") {
                val player = playerOf(character)
                player.shooting = true

                enemy.isMegamanShootingAtMe() shouldBe true
            }

            it("does not shoot at the enemy when it does not shoot") {
                playerOf(character)

                enemy.isMegamanShootingAtMe() shouldBe false
            }

            it("does not shoot at the enemy when it faces away") {
                val player = playerOf(character)
                player.shooting = true
                player.facing = Facing.RIGHT

                enemy.isMegamanShootingAtMe() shouldBe false
            }
        }
    }
})
