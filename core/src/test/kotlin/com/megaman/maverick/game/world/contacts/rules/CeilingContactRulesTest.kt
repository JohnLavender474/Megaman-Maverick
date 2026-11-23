package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.addFixtureLabel
import com.megaman.maverick.game.world.body.hasAnyFeetBlock
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CeilingContactRulesTest : DescribeSpec({

    lateinit var ceilingFixture: Fixture
    lateinit var wallFixture: Fixture
    lateinit var playerBody: Body
    lateinit var sideFixture: Fixture
    lateinit var headFixture: Fixture
    lateinit var feetFixture: Fixture

    fun blockFixture(ceiling: Boolean): Fixture {
        val body = Body(BodyType.STATIC)
        if (ceiling) body.addBodyLabel(BodyLabel.CEILING)
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns body
        val fixture = Fixture(body, FixtureType.BLOCK)
        fixture.putProperty(ConstKeys.ENTITY, block)
        if (ceiling) fixture.addFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)
        return fixture
    }

    beforeEach {
        ceilingFixture = blockFixture(ceiling = true)
        wallFixture = blockFixture(ceiling = false)

        playerBody = Body(BodyType.DYNAMIC)
        val player = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (player as IBodyEntity).body } returns playerBody

        sideFixture = Fixture(playerBody, FixtureType.SIDE)
        sideFixture.putProperty(ConstKeys.ENTITY, player)
        sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)

        headFixture = Fixture(playerBody, FixtureType.HEAD)
        headFixture.putProperty(ConstKeys.ENTITY, player)

        feetFixture = Fixture(playerBody, FixtureType.FEET)
        feetFixture.putProperty(ConstKeys.ENTITY, player)
    }

    describe("BlockSideRule") {
        it("does not set the side sense for a ceiling polygon") {
            BlockSideRule.onBegin(ceilingFixture, sideFixture, 0f)
            BlockSideRule.onContinue(ceilingFixture, sideFixture, 0f)

            playerBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) shouldBe false
        }

        it("does not clear the side sense of a wall when a ceiling polygon contact ends") {
            BlockSideRule.onBegin(wallFixture, sideFixture, 0f)
            BlockSideRule.onEnd(ceilingFixture, sideFixture, 0f)

            playerBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) shouldBe true
        }
    }

    describe("HeadBlockRule") {
        it("leaves the head contact of a ceiling polygon to its push-out") {
            playerBody.physics.velocity.y = 5f

            HeadBlockRule.onBegin(headFixture, ceilingFixture, 0f)
            HeadBlockRule.onContinue(headFixture, ceilingFixture, 0f)

            playerBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
            playerBody.physics.velocity.y shouldBe 5f
        }

        it("does not clear the head sense when a ceiling polygon contact ends") {
            HeadBlockRule.onBegin(headFixture, wallFixture, 0f)
            HeadBlockRule.onEnd(headFixture, ceilingFixture, 0f)

            playerBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe true
        }
    }

    describe("FeetBlockRule") {
        it("never makes a ceiling polygon a floor") {
            FeetBlockRule.onBegin(feetFixture, ceilingFixture, 0f)
            FeetBlockRule.onContinue(feetFixture, ceilingFixture, 0f)

            playerBody.isSensing(BodySense.FEET_ON_GROUND) shouldBe false
            playerBody.hasAnyFeetBlock() shouldBe false
        }

        it("does not clear the ground sense when a ceiling polygon contact ends") {
            playerBody.setBodySense(BodySense.FEET_ON_GROUND, true)

            FeetBlockRule.onEnd(feetFixture, ceilingFixture, 0f)

            playerBody.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
        }
    }
})
