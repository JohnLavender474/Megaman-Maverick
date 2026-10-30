package com.megaman.maverick.game.entities

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class TestExistenceEntity(services: MegaServices) : MegaGameEntity(services), IFreezableEntity {
    override var frozen = false
    override fun getType() = EntityType.OTHER
}

class EntityExistenceTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine()
        services = testServices(engine = engine)
    }

    describe("engine reset") {
        it("leaves an entity that was live not live after the update") {
            val entity = TestExistenceEntity(services)
            entity.spawn(Properties())
            engine.update(0f)

            engine.reset()

            entity.existenceState.liveNow shouldBe false
            entity.existenceState.liveAfterUpdate shouldBe false
        }
    }

    describe("the ice shield of a FreezableEntityHandler") {
        it("stays live through an unfreeze and a freeze in one frame and a later freeze") {
            val entity = TestExistenceEntity(services)
            val handler = FreezableEntityHandler(entity)
            val shield = FreezableEntityHandler::class.java.getDeclaredField("shieldEntity")
                .apply { isAccessible = true }
                .get(handler) as MegaGameEntity

            handler.setFrozen(true)
            engine.update(0f)
            shield.existenceState.liveNow shouldBe true

            handler.setFrozen(false)
            handler.setFrozen(true)
            engine.update(0f)
            shield.existenceState.liveNow shouldBe true

            handler.setFrozen(true)
            engine.update(0f)

            shield.existenceState.liveNow shouldBe true
        }
    }

    describe("a child and its parent destroyed in one frame") {
        it("runs the onDestroy of the child once") {
            val child = TestExistenceEntity(services)
            var childDestroys = 0
            child.runnablesOnDestroy.put("count") { childDestroys++ }
            val parent = object : MegaGameEntity(services) {
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
