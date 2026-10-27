package com.megaman.maverick.game.entities

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class TestExistenceEntity(game: MegamanMaverickGame) : MegaGameEntity(game), IFreezableEntity {
    override var frozen = false
    override fun getType() = EntityType.OTHER
}

class EntityExistenceTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var game: MegamanMaverickGame

    beforeEach {
        engine = GameEngine()
        game = mockk(relaxed = true)
        every { game.engine } returns engine
        every { game.state } returns GameState()
        every { game.getCurrentLevel() } returns LevelDefinition.TEST_1
    }

    describe("engine reset") {
        it("leaves an entity that was live dead") {
            val entity = TestExistenceEntity(game)
            entity.spawn(Properties())
            engine.update(0f)

            engine.reset()

            entity.spawned shouldBe false
            entity.dead shouldBe true
        }
    }

    describe("the ice shield of a FreezableEntityHandler") {
        it("stays live through an unfreeze and a freeze in one frame and a later freeze") {
            val entity = TestExistenceEntity(game)
            val handler = FreezableEntityHandler(entity)
            val shield = FreezableEntityHandler::class.java.getDeclaredField("shieldEntity")
                .apply { isAccessible = true }
                .get(handler) as MegaGameEntity

            handler.setFrozen(true)
            engine.update(0f)
            shield.spawned shouldBe true

            handler.setFrozen(false)
            handler.setFrozen(true)
            engine.update(0f)
            shield.spawned shouldBe true

            handler.setFrozen(true)
            engine.update(0f)

            shield.spawned shouldBe true
        }
    }

    describe("a child and its parent destroyed in one frame") {
        it("runs the onDestroy of the child once") {
            val child = TestExistenceEntity(game)
            var childDestroys = 0
            child.runnablesOnDestroy.put("count") { childDestroys++ }
            val parent = object : MegaGameEntity(game) {
                override fun getType() = EntityType.OTHER

                override fun onDestroy() {
                    super.onDestroy()
                    child.destroy()
                }
            }
            child.spawn(Properties())
            parent.spawn(Properties())
            engine.update(0f)

            child.destroy()
            parent.destroy()
            engine.update(0f)

            childDestroys shouldBe 1
        }
    }
})
