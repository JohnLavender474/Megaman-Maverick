package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.receiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class HalfDomeShieldTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()
    val radius = 0.6f * ppm

    lateinit var services: MegaServices
    lateinit var shot: Bullet

    beforeEach {
        services = testServices()
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
        shot = Bullet(services).apply { init() }
    }

    afterEach {
        MegaEntityFactory.reset()
        clearAllMocks(answers = false)
    }

    fun domeBody(direction: Direction, passes: (IProjectileEntity) -> Boolean = { false }): Body {
        val body = Body(BodyType.DYNAMIC, 0f, 0f, ppm, ppm)
        body.direction = direction
        HalfDomeShield(body, radius, direction = { direction }, passes = passes)
        body.preProcess()
        return body
    }

    fun dome(body: Body) = body.fixtures.get(FixtureType.SHIELD).first()

    fun domeCenter(body: Body) = (dome(body).getShape() as GameCircle).getCenter(Vector2())

    fun placeShot(direction: Direction, body: Body, topOverCenter: Float) {
        val center = domeCenter(body)
        val localCenter = GravityFrame.toLocal(direction, GameRectangle(center.x, center.y, 0f, 0f), GameRectangle())
        val size = 0.5f * ppm
        val local = GameRectangle(localCenter.getX() - size / 2f, localCenter.getY() + topOverCenter - size, size, size)
        shot.body.set(GravityFrame.toWorld(direction, local, GameRectangle()))
    }

    fun deflects(body: Body) =
        dome(body).receiver(Receivers.NO_PROJECTILE_COLLISION_EXCEPTION)!!.invoke(shot, dome(body))

    describe("the dome position") {
        mapOf(
            Direction.UP to Vector2(0.5f, 0f),
            Direction.DOWN to Vector2(0.5f, 1f),
            Direction.LEFT to Vector2(1f, 0.5f),
            Direction.RIGHT to Vector2(0f, 0.5f)
        ).forEach { (direction, feet) ->
            it("sits on the side of the feet for $direction") {
                val center = domeCenter(domeBody(direction))
                center.x shouldBe (feet.x * ppm plusOrMinus 0.001f)
                center.y shouldBe (feet.y * ppm plusOrMinus 0.001f)
            }
        }

        it("follows a change of the gravity direction at the next pre-process") {
            var direction = Direction.UP
            val body = Body(BodyType.DYNAMIC, 0f, 0f, ppm, ppm)
            HalfDomeShield(body, radius, direction = { direction }, passes = { false })
            body.preProcess()
            direction = Direction.DOWN
            body.preProcess()
            domeCenter(body).y shouldBe (ppm plusOrMinus 0.001f)
        }

        it("is a circle of the given radius") {
            (dome(domeBody(Direction.UP)).getShape() as GameCircle).getRadius() shouldBe radius
        }
    }

    describe("a shot") {
        Direction.entries.forEach { direction ->
            it("passes when its top is below the dome center, for $direction") {
                val body = domeBody(direction)
                placeShot(direction, body, topOverCenter = -0.1f * ppm)
                deflects(body) shouldBe false
            }

            it("deflects when it reaches the dome center, for $direction") {
                val body = domeBody(direction)
                placeShot(direction, body, topOverCenter = 0.1f * ppm)
                deflects(body) shouldBe true
            }
        }

        it("passes when the pass rule takes it, also when it reaches the dome") {
            val body = domeBody(Direction.UP, passes = { it === shot })
            placeShot(Direction.UP, body, topOverCenter = 0.3f * ppm)
            deflects(body) shouldBe false
        }
    }
})
