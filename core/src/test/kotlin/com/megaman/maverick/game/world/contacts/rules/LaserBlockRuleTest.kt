package com.megaman.maverick.game.world.contacts.rules

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.ILaserEntity
import com.megaman.maverick.game.entities.contracts.LaserContacts
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class LaserBlockRuleTest : DescribeSpec({

    val origin = Vector2(16f, -50f)
    lateinit var contacts: LaserContacts
    lateinit var laserFixture: Fixture

    fun blockFixture(x: Float, y: Float, width: Float, height: Float, shape: Any?): Fixture {
        val body = Body(BodyType.STATIC, x, y, width, height)
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns body
        val fixture = when (shape) {
            is GamePolygon -> Fixture(body, FixtureType.BLOCK, shape)
            else -> Fixture(body, FixtureType.BLOCK, GameRectangle(x, y, width, height))
        }
        fixture.putProperty(ConstKeys.ENTITY, block)
        body.addFixture(fixture)
        return fixture
    }

    fun firstHit(): Vector2 = Vector2(contacts.peek().first)

    beforeEach {
        contacts = LaserContacts { origin }
        val laserBody = Body(BodyType.ABSTRACT)
        val laser = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(ILaserEntity::class))
        every { (laser as ILaserEntity).isLaserIgnoring(any()) } returns false
        laserFixture = Fixture(laserBody, FixtureType.LASER, GameLine(16f, -50f, 16f, 100f))
        laserFixture.attachedToBody = false
        laserFixture.putProperty(ConstKeys.ENTITY, laser)
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
    }

    describe("a laser that hits a polygon block") {
        it("hits the face of a V, not its bounding box, at begin and at continue") {
            val polygon = GamePolygon(floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f))
            val block = blockFixture(0f, 0f, 64f, 32f, polygon)
            val rule = LaserBlockRule()

            rule.onBegin(laserFixture, block, 0f)
            firstHit() shouldBe Vector2(16f, 16f)

            contacts.clear()
            rule.onContinue(laserFixture, block, 0f)
            firstHit() shouldBe Vector2(16f, 16f)
        }
    }

    describe("a laser that hits a rectangle block") {
        it("hits the bottom edge") {
            val block = blockFixture(0f, 0f, 64f, 32f, null)

            LaserBlockRule().onBegin(laserFixture, block, 0f)

            firstHit() shouldBe Vector2(16f, 0f)
        }
    }
})
