package com.megaman.maverick.game.world.contacts.rules

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class BlockSideRuleTest : DescribeSpec({

    lateinit var sideBody: Body
    lateinit var sideFixture: Fixture
    lateinit var sideEntity: MegaGameEntity

    fun blockAt(x: Float, y: Float): Fixture {
        val blockBody = Body(BodyType.STATIC)
        blockBody.set(x, y, 32f, 32f)
        blockBody.putProperty(ConstKeys.PRIOR, Vector2(x, y))
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns blockBody
        every { block.existenceState } returns EntityExistenceState.LIVE
        val fixture = Fixture(blockBody, FixtureType.BLOCK, GameRectangle(0f, 0f, 32f, 32f))
        fixture.putProperty(ConstKeys.ENTITY, block)
        return fixture
    }

    fun touchingLeft() = sideBody.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)

    beforeEach {
        sideBody = Body(BodyType.DYNAMIC)
        sideBody.set(32f, 16f, 32f, 48f)
        val entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IBodyEntity::class))
        every { (entity as IBodyEntity).body } returns sideBody
        sideEntity = entity
        sideFixture = Fixture(sideBody, FixtureType.SIDE, GameRectangle(0f, 0f, 2f, 40f))
        sideFixture.offsetFromBodyAttachment.x = -16f
        sideFixture.putProperty(ConstKeys.ENTITY, entity)
        sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    }

    describe("BlockSideRule") {
        it("keeps the side sense when one block of a wall ends and the next block still touches") {
            val lower = blockAt(0f, 16f)
            val upper = blockAt(0f, 48f)
            BlockSideRule.onBegin(lower, sideFixture, 0f)

            BlockSideRule.onBegin(upper, sideFixture, 0f)
            lower.body.set(0f, -64f, 32f, 32f)
            BlockSideRule.onEnd(lower, sideFixture, 0f)

            touchingLeft() shouldBe true
        }

        it("clears the side sense when the last touched block ends") {
            val wall = blockAt(0f, 16f)
            BlockSideRule.onBegin(wall, sideFixture, 0f)

            BlockSideRule.onEnd(wall, sideFixture, 0f)

            touchingLeft() shouldBe false
        }

        it("forgets the touched blocks on a body reset, so a block left from a world reset cannot hold the sense") {
            sideBody.addFixture(sideFixture)
            BodyComponentCreator.create(sideEntity as IBodyEntity, sideBody)
            val ghost = blockAt(0f, 48f)
            val wall = blockAt(0f, 16f)
            BlockSideRule.onBegin(ghost, sideFixture, 0f)

            sideBody.reset()
            BlockSideRule.onBegin(wall, sideFixture, 0f)
            BlockSideRule.onEnd(wall, sideFixture, 0f)

            touchingLeft() shouldBe false
        }

        it("drops a kept block whose entity is no longer live") {
            val ghost = blockAt(0f, 48f)
            val wall = blockAt(0f, 16f)
            BlockSideRule.onBegin(ghost, sideFixture, 0f)
            BlockSideRule.onBegin(wall, sideFixture, 0f)
            every { (ghost.getProperty(ConstKeys.ENTITY) as Block).existenceState } returns
                EntityExistenceState.UNSPAWNED

            BlockSideRule.onEnd(wall, sideFixture, 0f)

            touchingLeft() shouldBe false
        }

        it("clears the side sense when the other kept block no longer overlaps the side") {
            val stale = blockAt(0f, 16f)
            val wall = blockAt(0f, 48f)
            BlockSideRule.onBegin(stale, sideFixture, 0f)
            BlockSideRule.onBegin(wall, sideFixture, 0f)
            stale.body.set(500f, 500f, 32f, 32f)

            BlockSideRule.onEnd(wall, sideFixture, 0f)

            touchingLeft() shouldBe false
        }
    }
})
