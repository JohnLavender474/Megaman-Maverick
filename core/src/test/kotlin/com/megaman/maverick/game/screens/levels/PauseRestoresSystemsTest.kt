package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.groups.GroupsSystem
import com.maverick.game.engine.motion.MotionSystem
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.currentHealth
import com.megaman.maverick.game.entities.translateHealth
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private class PauseRig {
    val controller = ControllerSystem()
    val behaviors = BehaviorsSystem()
    val motion = MotionSystem()
    val updatables = UpdatablesSystem()
    val sprites = SpritesSystem(collector = {})
    val groups = GroupsSystem()
    val systems = Array<GameSystem>().apply { addAll(controller, behaviors, motion, updatables, sprites, groups) }

    val state = GameState()
    val megaman: Megaman
    val handler: LevelStateHandler
    val stats: PlayerStatsHandler

    init {
        val engine = mockk<GameEngine>(relaxed = true)
        every { engine.systems } returns systems

        val services = testServices(engine = engine, gameState = state)
        megaman = Megaman(services)
        services.players.register(PlayerSlot(0), megaman)
        megaman.addComponent(PointsComponent().apply {
            putPoints(ConstKeys.HEALTH, ConstVals.MIN_HEALTH, MegamanValues.START_HEALTH, MegamanValues.START_HEALTH)
        })
        state.addListener(megaman.definePlayerProgressComponent())

        val game = mockk<MegamanMaverickGame>(relaxed = true)
        every { game.engine } returns engine
        every { game.gameState } returns state
        every { game.players } returns services.players

        handler = LevelStateHandler(game)
        stats = PlayerStatsHandler(game, handler)
    }

    fun onValues() = systems.map { it.on }

    fun allOn() = systems.map { true }

    fun allOffButSprites() = systems.map { it === sprites }

    fun runStatsUntilFinished() {
        var steps = 0
        while (!stats.finished) {
            stats.update(0.05f)
            steps++
            check(steps < 1000) { "the stats timers never finished" }
        }
    }
}

class PauseRestoresSystemsTest : DescribeSpec({

    lateinit var rig: PauseRig

    beforeEach { rig = PauseRig() }

    afterEach { unmockkAll() }

    describe("pause and resume") {
        it("turns off every system but the sprites while paused") {
            rig.handler.pause()

            rig.onValues() shouldBe rig.allOffButSprites()
        }

        it("restores each system to its value from before the pause") {
            rig.motion.on = false
            rig.groups.on = false
            val before = rig.onValues()

            rig.handler.pause()
            rig.handler.resume()

            rig.onValues() shouldBe before
        }

        it("keeps the gate systems off when the pause comes during a gate opening") {
            rig.behaviors.on = false
            rig.motion.on = false

            rig.handler.pause()
            rig.handler.resume()

            rig.behaviors.on shouldBe false
            rig.motion.on shouldBe false
            rig.controller.on shouldBe true
            rig.updatables.on shouldBe true
        }

        it("changes no system on a resume with no pause before it") {
            rig.motion.on = false
            val before = rig.onValues()

            rig.handler.resume()

            rig.onValues() shouldBe before
        }

        it("changes no system on a second resume") {
            rig.handler.pause()
            rig.handler.resume()
            rig.motion.on = false
            val before = rig.onValues()

            rig.handler.resume()

            rig.onValues() shouldBe before
        }

        it("keeps the values from before the first pause when pause runs twice") {
            rig.motion.on = false
            val before = rig.onValues()

            rig.handler.pause()
            rig.handler.pause()
            rig.handler.resume()

            rig.onValues() shouldBe before
        }

        it("changes no system on a resume after clear") {
            rig.handler.pause()
            rig.handler.clear()

            rig.handler.resume()

            rig.onValues() shouldBe rig.allOffButSprites()
        }
    }

    describe("system changes from the stats handler") {
        it("go to the live systems when the game is not paused") {
            rig.handler.turnOffAllSystemsButSprites()
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.turnOnAllSystems()
            rig.onValues() shouldBe rig.allOn()
        }

        it("go to the saved values while paused, and resume applies them") {
            rig.handler.pause()

            rig.handler.turnOnAllSystems()
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.resume()
            rig.onValues() shouldBe rig.allOn()
        }

        it("keep the systems off after resume when the last change while paused turned them off") {
            rig.handler.pause()

            rig.handler.turnOffAllSystemsButSprites()
            rig.handler.resume()

            rig.onValues() shouldBe rig.allOffButSprites()
        }
    }

    describe("a health fill") {
        it("that starts before the pause and ends during it keeps the live systems off, and resume turns them on") {
            rig.megaman.translateHealth(-10)
            rig.stats.addHealth(rig.megaman, 4)
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.pause()
            rig.runStatsUntilFinished()

            rig.megaman.currentHealth shouldBe MegamanValues.START_HEALTH - 6
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.resume()
            rig.onValues() shouldBe rig.allOn()
        }

        it("that is still running at resume keeps the systems off until it ends") {
            rig.megaman.translateHealth(-20)
            rig.stats.addHealth(rig.megaman, 10)

            rig.handler.pause()
            rig.stats.update(0.1f)
            rig.handler.resume()

            rig.stats.finished shouldBe false
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.runStatsUntilFinished()
            rig.onValues() shouldBe rig.allOn()
        }

        it("that starts during the pause does not change the live systems until resume") {
            rig.megaman.translateHealth(-10)
            rig.handler.pause()

            rig.stats.addHealth(rig.megaman, 4)
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.stats.update(0.05f)
            rig.handler.resume()
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.runStatsUntilFinished()
            rig.onValues() shouldBe rig.allOn()
        }

        it("with no pause freezes the systems and turns them on at its end") {
            rig.megaman.translateHealth(-10)

            rig.stats.addHealth(rig.megaman, 4)
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.runStatsUntilFinished()
            rig.onValues() shouldBe rig.allOn()
        }
    }

    describe("a heart tank") {
        it("that ends during the pause keeps the live systems off, and resume turns them on") {
            rig.stats.attain(MegaHeartTank.A)
            rig.stats.update(0.05f)
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.pause()
            rig.runStatsUntilFinished()

            rig.state.containsHeartTank(MegaHeartTank.A) shouldBe true
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.resume()
            rig.onValues() shouldBe rig.allOn()
        }

        it("that is picked up during the pause does not change the live systems") {
            rig.handler.pause()

            rig.stats.attain(MegaHeartTank.A)
            rig.runStatsUntilFinished()

            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.resume()
            rig.onValues() shouldBe rig.allOn()
        }
    }

    describe("a health tank") {
        it("that ends during the pause keeps the live systems off, and resume turns them on") {
            rig.stats.attain(MegaHealthTank.A)
            rig.stats.update(0.05f)
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.pause()
            rig.runStatsUntilFinished()

            rig.state.containsHealthTank(MegaHealthTank.A) shouldBe true
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.handler.resume()
            rig.onValues() shouldBe rig.allOn()
        }

        it("that is still running at resume keeps the systems off until it ends") {
            rig.stats.attain(MegaHealthTank.A)
            rig.stats.update(0.05f)

            rig.handler.pause()
            rig.stats.update(0.1f)
            rig.handler.resume()

            rig.stats.finished shouldBe false
            rig.onValues() shouldBe rig.allOffButSprites()

            rig.runStatsUntilFinished()
            rig.onValues() shouldBe rig.allOn()
        }
    }
})
