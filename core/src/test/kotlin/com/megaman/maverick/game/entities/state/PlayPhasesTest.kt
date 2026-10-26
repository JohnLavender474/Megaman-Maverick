package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.entities.GameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class PhaseEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class PlayPhasesTest : DescribeSpec({

    describe("play phase") {
        it("reads IN_PLAY for an entity without the component") {
            val entity = PhaseEntity()

            entity.playPhase shouldBe PlayPhase.IN_PLAY
            entity.inPlay shouldBe true
        }

        it("starts PRE_PLAY with the component, and setPlayPhase changes it") {
            val entity = PhaseEntity().also { it.addComponent(PlayPhaseComponent()) }

            entity.playPhase shouldBe PlayPhase.PRE_PLAY
            entity.inPlay shouldBe false

            entity.setPlayPhase(PlayPhase.IN_PLAY)
            entity.inPlay shouldBe true

            entity.setPlayPhase(PlayPhase.POST_PLAY)
            entity.playPhase shouldBe PlayPhase.POST_PLAY
            entity.inPlay shouldBe false
        }

        it("fails to write the phase of an entity without the component") {
            shouldThrow<MissingComponentException> { PhaseEntity().setPlayPhase(PlayPhase.IN_PLAY) }
        }

        it("keeps the phase across a reset of the component") {
            val component = PlayPhaseComponent(PlayPhase.POST_PLAY)

            component.reset()

            component.phase shouldBe PlayPhase.POST_PLAY
        }
    }
})
