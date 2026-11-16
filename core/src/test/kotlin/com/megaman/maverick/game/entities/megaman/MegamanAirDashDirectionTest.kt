package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.behaviors.airDashBehavior
import com.megaman.maverick.game.entities.megaman.components.getSpriteDirection
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanAirDashDirectionTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var body: Body
    var dashing = false

    beforeEach {
        dashing = false

        megaman = Megaman(testServices(engine = mockk<GameEngine>(relaxed = true)))
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER
        megaman.facing = Facing.RIGHT

        body = Body(BodyType.DYNAMIC)
        megaman.addComponent(BodyComponent(body))

        val behaviors = BehaviorsComponent()
        val dash = mockk<IBehavior>(relaxed = true)
        every { dash.isActive() } answers { dashing }
        behaviors.addBehavior(BehaviorType.AIR_DASHING, dash)
        megaman.addComponent(behaviors)
    }

    afterEach { unmockkAll() }

    describe("the sprite direction") {
        it("is the body direction with no air dash") {
            body.direction = Direction.LEFT

            megaman.getSpriteDirection() shouldBe Direction.LEFT
        }

        it("keeps the direction of the start of the air dash") {
            body.direction = Direction.LEFT
            megaman.airDashBehavior().init()
            dashing = true

            body.direction = Direction.DOWN

            megaman.airDashDirection shouldBe Direction.LEFT
            megaman.getSpriteDirection() shouldBe Direction.LEFT
        }

        it("takes a new direction at the next air dash") {
            body.direction = Direction.LEFT
            megaman.airDashBehavior().init()

            body.direction = Direction.RIGHT
            megaman.airDashBehavior().init()
            dashing = true

            megaman.getSpriteDirection() shouldBe Direction.RIGHT
        }
    }
})
