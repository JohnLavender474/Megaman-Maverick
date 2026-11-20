package com.maverick.game.engine.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.BodyInterpolation
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.body.IFixtureType
import com.maverick.game.engine.world.body.TestFixtureType
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.collisions.StandardCollisionHandler
import com.maverick.game.engine.world.contacts.IContactFilter
import com.maverick.game.engine.world.contacts.IContactListener
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe

private class RecordingListener : IContactListener {
    var begins = 0
    var continues = 0
    var ends = 0
    val begun = Array<Pair<IFixture, IFixture>>()

    override fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {
        begins++
        begun.add(fixture1 to fixture2)
    }

    override fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {
        continues++
    }

    override fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {
        ends++
    }
}

class WorldWrapTest : DescribeSpec({
    val ppm = 10
    val step = 0.02f
    val regionWidth = 100f
    val region = GameRectangle(0f, 0f, regionWidth, 100f)

    lateinit var listener: RecordingListener
    lateinit var world: WorldSystem
    val wrapping = ObjectSet<IGameEntity>()
    var caps = WorldWrap.UNLIMITED_WRAPS
    var regionOn = true
    var keyTypes = ObjectSet<IFixtureType>()
    var rejectedPairs = ObjectSet<IFixtureType>()

    val container = SimpleGridWorldContainer(ppm)
    var collide = false

    fun makeWorld(wrap: WorldWrap?) = WorldSystem(
        ppm = ppm,
        fixedStep = step,
        worldContainerSupplier = { container },
        contactListener = listener,
        collisionHandler = object : ICollisionHandler {
            override fun handleCollision(body1: IBody, body2: IBody) =
                collide && StandardCollisionHandler.handleCollision(body1, body2)
        },
        contactFilter = object : IContactFilter {
            override fun shouldProceedFiltering(fixture: IFixture) = keyTypes.contains(fixture.getType())
            override fun filter(fixture1: IFixture, fixture2: IFixture) =
                fixture1 != fixture2 && fixture1.getType() != fixture2.getType() &&
                    !rejectedPairs.contains(fixture1.getType()) && !rejectedPairs.contains(fixture2.getType())
        },
        wrap = wrap
    ).also { it.on = true }

    fun defaultWrap() = WorldWrap(
        region = { out -> if (regionOn) out.set(region) else null },
        wraps = { entity -> wrapping.contains(entity) },
        maxWraps = { _ -> caps }
    )

    fun addBody(
        type: BodyType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fixtureType: IFixtureType? = null,
        wraps: Boolean = false
    ): Body {
        val body = Body(type, x, y, width, height)
        body.physics.gravityOn = false
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        if (fixtureType != null) body.addFixture(Fixture(body, fixtureType, GameRectangle().setSize(width, height)))
        val entity = MockGameEntity().apply { addComponent(BodyComponent(body)) }
        if (wraps) wrapping.add(entity)
        world.add(entity)
        return body
    }

    fun centerX(body: Body) = body.getCenter(Vector2()).x

    beforeEach {
        listener = RecordingListener()
        wrapping.clear()
        caps = WorldWrap.UNLIMITED_WRAPS
        regionOn = true
        collide = false
        keyTypes = ObjectSet<IFixtureType>().apply { add(TestFixtureType.KEEP) }
        rejectedPairs = ObjectSet()
        world = makeWorld(defaultWrap())
    }

    describe("body wrap state") {
        it("wrapBy moves the body and its step start, so the step move stays the small move") {
            val body = Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f)
            body.beginStep(1)
            body.translate(-2f, 0f)
            body.wrapBy(regionWidth)
            body.endStep()
            body.getX() shouldBe 98f
            body.getStepMove(Vector2()).x shouldBe -2f
            body.wrapCount shouldBe 1
        }

        it("reset clears the wrap count") {
            val body = Body(BodyType.DYNAMIC)
            body.wrapBy(regionWidth)
            body.wrapBy(-regionWidth)
            body.wrapCount shouldBe 2
            body.reset()
            body.wrapCount shouldBe 0
        }

        it("the draw offset after a wrap is the small step move, not the wrap width and not zero") {
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            val interpolation = BodyInterpolation(world = world, interpolation = true)
            world.update(step)
            body.wrapCount shouldBe 1
            val move = body.getStepMove(Vector2()).x
            move shouldBe (-50f * step plusOrMinus 0.001f)
            val offset = interpolation.getDrawOffset(body, Vector2()).x
            offset shouldBe (move * (world.alpha - 1f) plusOrMinus 0.001f)
            (offset == 0f) shouldBe false
        }
    }

    describe("the wrap in the step") {
        it("a body whose center crosses the left edge comes in at the right edge") {
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            world.update(step)
            body.getX() shouldBe (-5.5f + regionWidth plusOrMinus 0.001f)
            body.wrapCount shouldBe 1
        }

        it("a body whose center crosses the right edge comes in at the left edge") {
            val body = addBody(BodyType.DYNAMIC, 94.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = 50f
            world.update(step)
            body.getX() shouldBe (95.5f - regionWidth plusOrMinus 0.001f)
            body.wrapCount shouldBe 1
        }

        it("a body that crosses an edge with its center still inside does not wrap") {
            val body = addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            world.update(step)
            body.getX() shouldBe (-4f plusOrMinus 0.001f)
            body.wrapCount shouldBe 0
        }

        it("a body at its wrap cap leaves through the edge") {
            caps = 1
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            world.update(step)
            body.wrapCount shouldBe 1
            repeat(100) { world.update(step) }
            body.wrapCount shouldBe 1
            body.getX() shouldBeLessThan 0f
        }

        it("with no region nothing wraps") {
            regionOn = false
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            world.update(step)
            body.getX() shouldBe (-5.5f plusOrMinus 0.001f)
            body.wrapCount shouldBe 0
        }

        it("with no wrap nothing wraps") {
            world = makeWorld(wrap = null)
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            world.update(step)
            body.getX() shouldBe (-5.5f plusOrMinus 0.001f)
        }

        it("an entity that the filter does not accept does not wrap") {
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = false)
            body.physics.velocity.x = -50f
            world.update(step)
            body.getX() shouldBe (-5.5f plusOrMinus 0.001f)
            body.wrapCount shouldBe 0
        }

        it("a postProcess reader sees the unwrapped position") {
            val body = addBody(BodyType.DYNAMIC, -4.5f, 10f, 10f, 10f, wraps = true)
            body.physics.velocity.x = -50f
            var seenCenterX = Float.NaN
            body.postProcess.put("reader") { seenCenterX = centerX(it) }
            world.update(step)
            seenCenterX shouldBe (-0.5f plusOrMinus 0.001f)
            centerX(body) shouldBe (99.5f plusOrMinus 0.001f)
        }
    }

    describe("seam rule") {
        it("gives the far side of the edge that a box crosses") {
            WorldWrap.seamShift(-3f, 7f, region, 0, 0f, 1) shouldBe regionWidth
            WorldWrap.seamShift(93f, 103f, region, 0, 0f, 1) shouldBe -regionWidth
            WorldWrap.seamShift(10f, 20f, region, 0, 0f, 1) shouldBe 0f
        }

        it("at the cap gives only the edge that the last wrap came in through") {
            WorldWrap.seamShift(93f, 103f, region, 1, regionWidth, 1) shouldBe -regionWidth
            WorldWrap.seamShift(-3f, 7f, region, 1, regionWidth, 1) shouldBe 0f
            WorldWrap.seamShift(-3f, 7f, region, 0, 0f, 0) shouldBe 0f
        }
    }

    describe("seam contacts") {
        it("a fixture across an edge contacts a fixture on the far side") {
            val mover = addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            val far = addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 1
            val (f1, f2) = listener.begun[0]
            mover.hasFixture(f1) shouldBe true
            far.hasFixture(f2) shouldBe true
        }

        it("a fixture across an edge of a body that does not wrap has no seam contact") {
            addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = false)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 0
        }

        it("a body at its wrap cap has no seam contact") {
            caps = 0
            addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 0
        }

        it("a body at its wrap cap keeps its seam contacts at the edge it came in through") {
            caps = 1
            val mover = addBody(BodyType.DYNAMIC, 97f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            mover.wrapBy(-regionWidth)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 1
        }

        it("a body at its wrap cap has no seam contact at the edge it leaves through") {
            caps = 1
            val mover = addBody(BodyType.DYNAMIC, -103f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            mover.wrapBy(regionWidth)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 0
        }

        it("a contact held while the body crosses and wraps begins once and does not end") {
            val mover = addBody(BodyType.DYNAMIC, 1.5f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            mover.physics.velocity.x = -50f
            addBody(BodyType.STATIC, 85f, 10f, 15f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 0
            var steps = 0
            while (mover.wrapCount == 0 && steps < 100) {
                world.update(step)
                steps++
            }
            mover.wrapCount shouldBe 1
            repeat(3) { world.update(step) }
            listener.begins shouldBe 1
            listener.ends shouldBe 0
            mover.getX() shouldBeGreaterThan 85f
        }

        it("a fixture that overlaps a floor both really and across the edge has one contact") {
            addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            addBody(BodyType.STATIC, 0f, 5f, regionWidth, 10f, TestFixtureType.OTHER_TYPE)
            repeat(5) { world.update(step) }
            listener.begins shouldBe 1
            listener.continues shouldBe 4
            listener.ends shouldBe 0
        }

        it("a non-key fixture on a wrapping body is found across the edge") {
            val shot = addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.SKIP, wraps = true)
            val target = addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.KEEP)
            world.update(step)
            listener.begins shouldBe 1
            val (f1, f2) = listener.begun[0]
            shot.hasFixture(f1) shouldBe true
            target.hasFixture(f2) shouldBe true
        }

        it("the seam query of a non-key fixture keeps the contact filter") {
            rejectedPairs.add(TestFixtureType.SKIP)
            addBody(BodyType.DYNAMIC, -3f, 10f, 10f, 10f, TestFixtureType.SKIP, wraps = true)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.KEEP)
            world.update(step)
            listener.begins shouldBe 0
        }

        it("a fixture inside the region has no seam contact") {
            addBody(BodyType.DYNAMIC, 40f, 10f, 10f, 10f, TestFixtureType.KEEP, wraps = true)
            addBody(BodyType.STATIC, 90f, 10f, 10f, 10f, TestFixtureType.OTHER_TYPE)
            world.update(step)
            listener.begins shouldBe 0
        }
    }

    describe("seam collisions") {
        fun standOnFarFloor(wraps: Boolean): Body {
            collide = true
            addBody(BodyType.STATIC, 85f, 0f, 15f, 10f)
            val body = addBody(BodyType.DYNAMIC, -4f, 10f, 10f, 10f, wraps = wraps)
            body.physics.velocityClamp.set(1000f, 50f)
            body.physics.gravityOn = true
            body.physics.gravity.set(0f, -500f)
            repeat(50) { world.update(step) }
            return body
        }

        it("a body that stands across an edge on a floor that exists only on the far side stays on it") {
            val body = standOnFarFloor(wraps = true)
            body.getY() shouldBe (10f plusOrMinus 0.01f)
            body.getX() shouldBe (-4f plusOrMinus 0.001f)
        }

        it("a body that does not wrap falls through the same place") {
            val body = standOnFarFloor(wraps = false)
            body.getY() shouldBeLessThan 0f
        }
    }
})
