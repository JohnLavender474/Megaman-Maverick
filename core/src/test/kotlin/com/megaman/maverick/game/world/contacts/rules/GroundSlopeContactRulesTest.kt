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
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.isSensing
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GroundSlopeContactRulesTest : DescribeSpec({

    lateinit var slopeFixture: Fixture
    lateinit var wallFixture: Fixture
    lateinit var playerBody: Body
    lateinit var sideFixture: Fixture
    lateinit var headFixture: Fixture

    fun blockFixture(slope: Boolean): Fixture {
        val body = Body(BodyType.STATIC)
        if (slope) body.addBodyLabel(BodyLabel.SLOPE)
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns body
        val fixture = Fixture(body, FixtureType.BLOCK)
        fixture.putProperty(ConstKeys.ENTITY, block)
        return fixture
    }

    beforeEach {
        slopeFixture = blockFixture(slope = true)
        wallFixture = blockFixture(slope = false)

        playerBody = Body(BodyType.DYNAMIC)
        val player = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (player as IBodyEntity).body } returns playerBody

        sideFixture = Fixture(playerBody, FixtureType.SIDE)
        sideFixture.putProperty(ConstKeys.ENTITY, player)
        sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)

        headFixture = Fixture(playerBody, FixtureType.HEAD)
        headFixture.putProperty(ConstKeys.ENTITY, player)
    }

    describe("BlockSideRule") {
        it("does not set the side sense for a slope") {
            BlockSideRule.onBegin(slopeFixture, sideFixture, 0f)
            BlockSideRule.onContinue(slopeFixture, sideFixture, 0f)

            playerBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) shouldBe false
        }

        it("sets the side sense for a wall") {
            BlockSideRule.onBegin(wallFixture, sideFixture, 0f)

            playerBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) shouldBe true
        }

        it("does not clear the side sense of a wall when a slope contact ends") {
            BlockSideRule.onBegin(wallFixture, sideFixture, 0f)
            BlockSideRule.onEnd(slopeFixture, sideFixture, 0f)

            playerBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) shouldBe true
        }
    }

    describe("HeadBlockRule") {
        it("does not bump the head on a slope") {
            playerBody.physics.velocity.y = 5f

            HeadBlockRule.onBegin(headFixture, slopeFixture, 0f)
            HeadBlockRule.onContinue(headFixture, slopeFixture, 0f)

            playerBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe false
            playerBody.physics.velocity.y shouldBe 5f
        }

        it("bumps the head on a ceiling") {
            playerBody.physics.velocity.y = 5f

            HeadBlockRule.onBegin(headFixture, wallFixture, 0f)

            playerBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe true
            playerBody.physics.velocity.y shouldBe 0f
        }

        it("does not clear the head sense of a ceiling when a slope contact ends") {
            HeadBlockRule.onBegin(headFixture, wallFixture, 0f)
            HeadBlockRule.onEnd(headFixture, slopeFixture, 0f)

            playerBody.isSensing(BodySense.HEAD_TOUCHING_BLOCK) shouldBe true
        }
    }
})
