package com.megaman.maverick.game.world.contacts.rules

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setReceiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class FeetBlockRuleTest : DescribeSpec({

    lateinit var events: MutableList<String>
    lateinit var feetBody: Body
    lateinit var feetFixture: Fixture
    lateinit var blockFixture: Fixture

    beforeEach {
        events = mutableListOf()

        feetBody = Body(BodyType.DYNAMIC)
        val feetEntity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (feetEntity as IBodyEntity).body } returns feetBody
        feetFixture = Fixture(feetBody, FixtureType.FEET)
        feetFixture.putProperty(ConstKeys.ENTITY, feetEntity)

        val blockBody = Body(BodyType.STATIC)
        blockBody.putProperty(ConstKeys.PRIOR, Vector2())
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns blockBody
        every { block.hitByFeet(any(), any()) } answers { events.add("hitByFeet ${firstArg<ProcessState>()}") }
        blockFixture = Fixture(blockBody, FixtureType.BLOCK)
        blockFixture.putProperty(ConstKeys.ENTITY, block)

        feetFixture.setReceiver(Receivers.STICK_TO_BLOCK) { state, _ ->
            events.add("stick $state")
            true
        }
        feetFixture.setReceiver(Receivers.FEET_BLOCK_CONTACT) { state, _ ->
            events.add("contact $state onGround=${feetBody.isSensing(BodySense.FEET_ON_GROUND)}")
        }
    }

    describe("FeetBlockRule") {
        it("calls the feet-block contact after the stick check and before the ground sense and the block hit") {
            FeetBlockRule.onBegin(feetFixture, blockFixture, 0f)

            events shouldBe listOf("stick BEGIN", "contact BEGIN onGround=false", "hitByFeet BEGIN")
            feetBody.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
        }

        it("does the same in continue") {
            FeetBlockRule.onContinue(feetFixture, blockFixture, 0f)

            events shouldBe listOf("stick CONTINUE", "contact CONTINUE onGround=false", "hitByFeet CONTINUE")
        }

        it("calls the feet-block contact after the ground sense is cleared and before the block hit on end") {
            FeetBlockRule.onBegin(feetFixture, blockFixture, 0f)
            events.clear()

            FeetBlockRule.onEnd(feetFixture, blockFixture, 0f)

            events shouldBe listOf("contact END onGround=false", "hitByFeet END")
        }
    }
})
