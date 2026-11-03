package com.megaman.maverick.game.world.collisions

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.GroundSlope
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.MapObjectComponent
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.addFeetBlock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk

class GroundSlopeCollisionTest : DescribeSpec({

    lateinit var slope: GroundSlope
    lateinit var dynamicBody: Body
    val handler = MegaCollisionHandler()

    fun staticBlock(x: Float, y: Float, width: Float, height: Float): Body {
        val body = Body(BodyType.STATIC, x, y, width, height)
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns body
        every { block.getComponent(MapObjectComponent::class) } returns null
        body.putProperty(ConstKeys.ENTITY, block)
        return body
    }

    beforeEach {
        slope = GroundSlope(testServices(engine = mockk<GameEngine>(relaxed = true)))
        slope.init()
        slope.onSpawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 256f, 64f),
                ConstKeys.SIDE pairTo ConstKeys.RIGHT
            )
        )

        dynamicBody = Body(BodyType.DYNAMIC, 112f, 20f, 32f, 48f)
        dynamicBody.putProperty(ConstKeys.ENTITY, mockk<MegaGameEntity>(relaxed = true))
    }

    describe("a body with up gravity against a slope") {
        it("snaps up to the surface under its bottom-centre") {
            handler.handleCollision(slope.body, dynamicBody) shouldBe true
            dynamicBody.getY() shouldBe slope.surfaceAt(128f)
            dynamicBody.getX() shouldBe 112f
        }

        it("does not move a body above the surface down, also with its feet on the slope") {
            dynamicBody.setY(40f)
            dynamicBody.addFeetBlock(slope)

            handler.handleCollision(slope.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 40f
        }

        it("does not move a body above the surface down") {
            dynamicBody.setY(40f)

            handler.handleCollision(slope.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 40f
        }

        it("passes up through the slope while it rises") {
            dynamicBody.physics.velocity.y = 5f

            handler.handleCollision(slope.body, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 20f
        }

        it("is not moved when its bottom-centre is outside the run") {
            dynamicBody.setX(245f)

            handler.handleCollision(slope.body, dynamicBody) shouldBe true

            dynamicBody.getX() shouldBe 245f
            dynamicBody.getY() shouldBe 20f
        }
    }

    describe("a body with down gravity against a slope") {
        it("gets the rectangle push-out") {
            val entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IDirectional::class))
            every { (entity as IDirectional).direction } returns Direction.DOWN
            dynamicBody.putProperty(ConstKeys.ENTITY, entity)

            handler.handleCollision(slope.body, dynamicBody) shouldBe true

            dynamicBody.getX() shouldBe 144f
            dynamicBody.getY() shouldBe 20f
        }
    }

    describe("the flat block at the high end of a run") {
        lateinit var flat: Body

        beforeEach {
            flat = staticBlock(256f, 0f, 256f, 64f)
            dynamicBody.setPosition(230f, 60f)
        }

        it("does not push a body whose feet touch the slope under its bottom-centre") {
            dynamicBody.addFeetBlock(slope)

            handler.handleCollision(flat, dynamicBody) shouldBe true

            dynamicBody.getX() shouldBe 230f
            dynamicBody.getY() shouldBe 60f
        }

        it("pushes a body whose feet do not touch a slope") {
            handler.handleCollision(flat, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 64f
        }

        it("pushes a body whose bottom-centre is past the slope") {
            dynamicBody.addFeetBlock(slope)
            dynamicBody.setX(250f)

            handler.handleCollision(flat, dynamicBody) shouldBe true

            dynamicBody.getY() shouldBe 64f
        }
    }

    describe("a plain block with the slope label") {
        it("fails at once, because only a GroundSlope may carry the label") {
            val block = staticBlock(0f, 0f, 256f, 64f)
            block.addBodyLabel(BodyLabel.SLOPE)

            val e = shouldThrow<InvalidSlopeLabelException> { handler.handleCollision(block, dynamicBody) }
            e.cause.shouldBeInstanceOf<ClassCastException>()
        }
    }

    describe("a wall at the high end of a run") {
        it("still pushes a body whose feet touch the slope") {
            val wall = staticBlock(256f, 0f, 32f, 200f)
            dynamicBody.setPosition(230f, 60f)
            dynamicBody.addFeetBlock(slope)

            handler.handleCollision(wall, dynamicBody) shouldBe true

            dynamicBody.getX() shouldBe 224f
        }
    }
})
