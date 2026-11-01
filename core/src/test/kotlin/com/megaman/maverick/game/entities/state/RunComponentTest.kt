package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.entities.GameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class RunEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class RunComponentTest : DescribeSpec({

    describe("RunComponent") {
        it("clears both values on reset") {
            val component = RunComponent()
            component.running = true
            component.runTime = 2f

            component.reset()

            component.running shouldBe false
            component.runTime shouldBe 0f
        }
    }

    describe("the running query") {
        it("reads the component in play") {
            val entity = RunEntity()
            entity.addComponent(RunComponent())

            entity.running = true

            entity.running shouldBe true
        }

        it("is false out of play, and the component keeps its value") {
            val entity = RunEntity()
            val component = RunComponent()
            entity.addComponent(component)
            entity.addComponent(PlayPhaseComponent(PlayPhase.PRE_PLAY))
            entity.running = true

            entity.running shouldBe false
            component.running shouldBe true
        }

        it("is false on an entity with no component") {
            RunEntity().running shouldBe false
        }

        it("fails at a write on an entity with no component") {
            shouldThrow<MissingComponentException> { RunEntity().running = true }
        }
    }
})
