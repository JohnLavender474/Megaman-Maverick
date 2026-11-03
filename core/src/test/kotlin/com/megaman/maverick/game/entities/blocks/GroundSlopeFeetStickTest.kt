package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GroundSlopeFeetStickTest : DescribeSpec({

    lateinit var slope: GroundSlope
    lateinit var body: Body
    lateinit var feet: Fixture
    lateinit var entity: MegaGameEntity

    beforeEach {
        slope = GroundSlope(testServices(engine = mockk<GameEngine>(relaxed = true)))
        slope.init()
        slope.onSpawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 256f, 64f),
                ConstKeys.SIDE pairTo ConstKeys.RIGHT
            )
        )

        body = Body(BodyType.DYNAMIC, 112f, 40f, 32f, 48f)
        body.physics.gravityOn = true
        entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (entity as IBodyEntity).body } returns body
        body.putProperty(ConstKeys.ENTITY, entity)

        feet = Fixture(body, FixtureType.FEET, GameRectangle().setSize(16f, 8f))
        feet.putProperty(ConstKeys.ENTITY, entity)
    }

    describe("a feet contact that continues") {
        it("moves a body above the surface down to the surface under its bottom-centre") {
            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe slope.surfaceAt(128f)
            body.getX() shouldBe 112f
        }

        it("does not move a body below the surface up") {
            body.setY(20f)

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 20f
        }

        it("moves a body just past the low end down from the first step to the base") {
            body.setPosition(-20f, 2f)

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 0f
        }

        it("does not move a body past the low end that is higher than one step") {
            body.setPosition(-20f, 10f)

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 10f
        }

        it("does not move a body past the high end") {
            body.setPosition(244f, 70f)

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 70f
        }
    }

    describe("the gates") {
        it("does not move the body when the contact only begins (a landing)") {
            slope.hitByFeet(ProcessState.BEGIN, feet)

            body.getY() shouldBe 40f
        }

        it("does not move a rising body") {
            body.physics.velocity.y = 5f

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 40f
        }

        it("does not move a body with gravity off") {
            body.physics.gravityOn = false

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 40f
        }

        it("does not move a body with down gravity") {
            val directional = mockk<MegaGameEntity>(
                relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class, IDirectional::class)
            )
            every { (directional as IBodyEntity).body } returns body
            every { (directional as IDirectional).direction } returns Direction.DOWN
            body.putProperty(ConstKeys.ENTITY, directional)
            feet.putProperty(ConstKeys.ENTITY, directional)

            slope.hitByFeet(ProcessState.CONTINUE, feet)

            body.getY() shouldBe 40f
        }
    }
})
