package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.setReceiver
import com.megaman.maverick.game.world.contacts.ContactValues.ICE_FRICTION
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class IceRulesTest : DescribeSpec({

    lateinit var body: Body

    fun fixture(type: FixtureType): Fixture {
        val entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (entity as IBodyEntity).body } returns body
        val fixture = Fixture(body, type)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    val iceFixture = Fixture(Body(BodyType.STATIC), FixtureType.ICE)

    beforeEach {
        body = Body(BodyType.DYNAMIC)
        body.physics.frictionOnSelf.set(1f, 1f)
    }

    describe("FeetIceRule continue") {
        it("applies the ice friction when the feet have no filter") {
            FeetIceRule.onContinue(fixture(FixtureType.FEET), iceFixture, 0f)

            body.physics.frictionOnSelf.x shouldBe ICE_FRICTION * ConstVals.PPM
        }

        it("skips the ice friction when the filter returns false") {
            val feet = fixture(FixtureType.FEET)
            feet.setReceiver(Receivers.ICE_FRICTION_FILTER) { false }

            FeetIceRule.onContinue(feet, iceFixture, 0f)

            body.physics.frictionOnSelf.x shouldBe 1f
        }
    }

    describe("SideIceRule") {
        it("applies the side ice friction when the predicate returns true, although the body property is false") {
            body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", false)
            val side = fixture(FixtureType.SIDE)
            side.setReceiver(Receivers.SIDE_ICE_FRICTION) { true }

            SideIceRule.onBegin(side, iceFixture, 0f)

            body.physics.frictionOnSelf.y shouldBe ICE_FRICTION * ConstVals.PPM
        }

        it("does not apply the side ice friction when the predicate and the body property are false") {
            body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", false)
            val side = fixture(FixtureType.SIDE)
            side.setReceiver(Receivers.SIDE_ICE_FRICTION) { false }

            SideIceRule.onContinue(side, iceFixture, 0f)

            body.physics.frictionOnSelf.y shouldBe 1f
        }
    }
})
