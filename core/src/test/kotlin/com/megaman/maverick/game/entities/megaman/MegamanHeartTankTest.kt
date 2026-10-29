package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanHeartTankTest : DescribeSpec({

    lateinit var state: GameState
    lateinit var megaman: Megaman

    beforeEach {
        state = GameState()
        val game = testServices(engine = mockk<GameEngine>(relaxed = true), state = state)

        megaman = Megaman(game)
        megaman.addComponent(PointsComponent().apply {
            putPoints(ConstKeys.HEALTH, ConstVals.MIN_HEALTH, MegamanValues.START_HEALTH, MegamanValues.START_HEALTH)
        })
        state.addListener(megaman)
    }

    afterEach { unmockkAll() }

    describe("max health") {
        it("is START_HEALTH plus HEALTH_BUMP for each heart tank") {
            state.addHeartTank(MegaHeartTank.A)
            state.addHeartTank(MegaHeartTank.D)
            megaman.getMaxHealth() shouldBe MegamanValues.START_HEALTH + 2 * MegaHeartTank.HEALTH_BUMP

            state.removeHeartTank(MegaHeartTank.A)
            megaman.getMaxHealth() shouldBe MegamanValues.START_HEALTH + MegaHeartTank.HEALTH_BUMP
        }

        it("does not change when a tank that is already owned is added again") {
            state.addHeartTank(MegaHeartTank.B)
            state.addHeartTank(MegaHeartTank.B)
            megaman.getMaxHealth() shouldBe MegamanValues.START_HEALTH + MegaHeartTank.HEALTH_BUMP
        }

        it("is MAX_HEALTH with every heart tank") {
            MegaHeartTank.entries.forEach { state.addHeartTank(it) }
            megaman.getMaxHealth() shouldBe ConstVals.MAX_HEALTH
        }

        it("goes back to START_HEALTH on reset") {
            MegaHeartTank.entries.forEach { state.addHeartTank(it) }
            state.reset()
            megaman.getMaxHealth() shouldBe MegamanValues.START_HEALTH
        }
    }
})
