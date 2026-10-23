package com.megaman.maverick.game.world.collisions

import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyKeys
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.addFeetBlock
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MegaCollisionHandlerTest : DescribeSpec({

    lateinit var staticBody: Body
    lateinit var dynamicBody: Body
    lateinit var block: Block

    beforeEach {
        staticBody = Body(BodyType.STATIC, 0f, 0f, 10f, 10f)
        block = mockk<Block>(relaxed = true)
        every { block.body } returns staticBody
        staticBody.putProperty(ConstKeys.ENTITY, block)

        dynamicBody = Body(BodyType.DYNAMIC, 0f, 9f, 2f, 2f)
        dynamicBody.putProperty(ConstKeys.ENTITY, mockk<MegaGameEntity>(relaxed = true))
    }

    describe("a collide-down-only block") {
        beforeEach {
            staticBody.addBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)
            dynamicBody.addFeetBlock(block)
        }

        it("puts a body that stands on it on top") {
            MegaCollisionHandler().handleCollision(staticBody, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 10f
        }

        it("lets a body pass whose pass-through key returns true") {
            dynamicBody.putProperty(BodyKeys.PASS_THROUGH_ONE_WAY) { true }

            MegaCollisionHandler().handleCollision(staticBody, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 9f
        }
    }

    describe("a press-up-fall-through block") {
        beforeEach { staticBody.addBodyLabel(BodyLabel.PRESS_UP_FALL_THRU) }

        it("moves a body whose fall-through key returns true to the top of the block") {
            dynamicBody.putProperty(BodyKeys.FALL_THROUGH_PRESS_UP) { true }

            MegaCollisionHandler().handleCollision(staticBody, dynamicBody) shouldBe true

            dynamicBody.getMaxY() shouldBe 10f
        }

        it("leaves a body without the key to the standard collision, which pushes it out on top") {
            MegaCollisionHandler().handleCollision(staticBody, dynamicBody)

            dynamicBody.getY() shouldBe 10f
        }
    }
})
