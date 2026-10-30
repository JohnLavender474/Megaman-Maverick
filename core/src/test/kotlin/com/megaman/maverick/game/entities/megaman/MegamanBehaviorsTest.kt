package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.GameEngine
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.components.defineBehaviorsComponent
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanBehaviorsTest : DescribeSpec({

    fun newMegaman(): Megaman {
        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        return Megaman(services)
    }

    afterEach { unmockkAll() }

    describe("the behaviors component") {
        it("adds the behaviors in the evaluation order") {
            val component = newMegaman().defineBehaviorsComponent()

            component.behaviors.keys().toList() shouldBe listOf(
                BehaviorType.WALL_SLIDING,
                BehaviorType.SWIMMING,
                BehaviorType.JUMPING,
                BehaviorType.AIR_DASHING,
                BehaviorType.GROUND_SLIDING,
                BehaviorType.CROUCHING,
                BehaviorType.CLIMBING,
                BehaviorType.JETPACKING
            )
        }

        it("gives each Megaman its own behavior objects") {
            val first = newMegaman().defineBehaviorsComponent()
            val second = newMegaman().defineBehaviorsComponent()

            first.behaviors.size shouldBe 8
            first.behaviors.keys().forEach {
                second.behaviors.get(it) shouldNotBeSameInstanceAs first.behaviors.get(it)
            }
        }
    }
})
