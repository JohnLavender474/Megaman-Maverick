package com.maverick.game.engine.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.contacts.IContactFilter
import com.maverick.game.engine.world.contacts.IContactListener
import com.maverick.game.engine.world.container.IWorldContainer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlin.math.abs

class WorldDrawOffsetsTest : DescribeSpec({
    val step = 1f / 150f
    val ppm = 32

    lateinit var body: Body
    lateinit var world: WorldSystem
    lateinit var entities: ImmutableCollection<IGameEntity>
    val out = Vector2()

    fun makeWorld(maxIterations: Int = Int.MAX_VALUE, fixedStepScalar: Float = 1f) = WorldSystem(
        ppm = ppm,
        fixedStep = step,
        worldContainerSupplier = { mockk<IWorldContainer>(relaxed = true) },
        contactListener = mockk<IContactListener>(relaxed = true),
        collisionHandler = mockk<ICollisionHandler>(relaxed = true),
        contactFilter = object : IContactFilter {
            override fun shouldProceedFiltering(fixture: IFixture) = true
            override fun filter(fixture1: IFixture, fixture2: IFixture) = true
        },
        maxIterations = maxIterations,
        fixedStepScalar = fixedStepScalar,
        interpolation = true
    )

    fun drawnX() = body.getX() + world.getDrawOffset(body, out).x

    beforeEach {
        body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravityOn = false
        val entity = MockGameEntity().apply { addComponent(BodyComponent(body)) }
        world = makeWorld()
        entities = ImmutableCollection(listOf<IGameEntity>(entity))
    }

    describe("body step state") {
        it("saves the move of the last step and clears it at reset") {
            body.beginStep(7)
            body.setX(3f)
            body.endStep()
            body.stepSerial shouldBe 7
            body.getStepMove(out) shouldBe Vector2(3f, 0f)

            body.reset()
            body.stepSerial shouldBe Body.NO_STEP
            body.getStepMove(out) shouldBe Vector2(0f, 0f)
        }

        it("counts only the move after a restart inside a step") {
            body.beginStep(1)
            body.setX(20f)
            body.restartStep()
            body.setX(21f)
            body.endStep()
            body.getStepMove(out) shouldBe Vector2(1f, 0f)
        }

        it("clears the last step move at a restart outside a step") {
            body.beginStep(1)
            body.setX(2f)
            body.endStep()
            body.restartStep()
            body.getStepMove(out) shouldBe Vector2(0f, 0f)
        }

        it("draws a body that is teleported inside a step at its new position") {
            world.process(true, entities, step)
            body.physics.velocity.x = 10f
            body.preProcess.put("teleport") {
                if (body.getX() < 20f) {
                    body.setX(25f)
                    body.restartStep()
                }
            }
            world.process(true, entities, step * 1.5f)
            world.getDrawOffset(body, out).x shouldBe ((world.alpha - 1f) * 10f * step plusOrMinus 1e-4f)
        }
    }

    describe("alpha") {
        it("is the leftover over the step, in 0..1") {
            world.process(true, entities, step * 2.5f)
            world.alpha shouldBe (0.5f plusOrMinus 1e-3f)
        }

        it("grows on a frame with no step") {
            world.process(true, entities, step * 0.25f)
            world.process(true, entities, step * 0.25f)
            world.alpha shouldBe (0.5f plusOrMinus 1e-3f)
            world.stepSerial shouldBe 0
        }

        it("is 1 at the step cap") {
            world = makeWorld(maxIterations = 2)
            world.process(true, entities, step * 5.5f)
            world.alpha shouldBe 1f
        }

        for (scalar in floatArrayOf(0.5f, 2f)) {
            it("goes evenly from 0 to 1 between 2 steps with a step scalar of $scalar") {
                world = makeWorld(fixedStepScalar = scalar)
                val interval = step / scalar
                val frame = interval / 8f
                world.process(true, entities, step)
                val start = world.stepSerial
                val alphas = Array<Float>()
                while (world.stepSerial == start) {
                    alphas.add(world.alpha)
                    world.process(true, entities, frame)
                }
                for (i in 1 until alphas.size) (alphas[i] - alphas[i - 1]) shouldBe (0.125f plusOrMinus 1e-3f)
            }
        }

        it("does not change while the world is off") {
            world.process(true, entities, step * 2.5f)
            val serial = world.stepSerial
            world.process(false, entities, step * 1.3f)
            world.alpha shouldBe (0.5f plusOrMinus 1e-3f)
            world.stepSerial shouldBe serial
        }
    }

    describe("draw offset") {
        it("is (alpha - 1) times the last step move") {
            body.physics.velocity.x = 150f
            world.process(true, entities, step * 2.5f)
            world.getDrawOffset(body, out).x shouldBe (-0.5f plusOrMinus 1e-3f)
        }

        it("is 0 when the flag is off") {
            world.interpolation = false
            body.physics.velocity.x = 150f
            world.process(true, entities, step * 2.5f)
            world.getDrawOffset(body, out) shouldBe Vector2(0f, 0f)
        }

        it("is 0 when the body was not in the last step") {
            body.physics.velocity.x = 150f
            world.process(true, entities, step * 2.5f)
            body.reset()
            world.getDrawOffset(body, out) shouldBe Vector2(0f, 0f)
        }

        it("is 0 for a body that did not update while other bodies stepped") {
            val skipped = Body(BodyType.DYNAMIC).apply {
                physics.applyFrictionX = false
                physics.gravityOn = false
                physics.velocity.x = 150f
            }
            var update = true
            val other = MockGameEntity().apply { addComponent(BodyComponent(skipped) { update }) }
            entities = ImmutableCollection(listOf(entities.first(), other))
            world.process(true, entities, step * 2.5f)
            update = false
            world.process(true, entities, step * 1f)
            world.getDrawOffset(skipped, out) shouldBe Vector2(0f, 0f)
        }

        it("is 0 when the step move is more than 1 tile") {
            body.preProcess.put("teleport") { it.setX(it.getX() + 2f * ppm) }
            world.process(true, entities, step * 2.5f)
            world.getDrawOffset(body, out) shouldBe Vector2(0f, 0f)
        }
    }

    describe("evenness") {
        it("moves the drawn body by the same amount each frame of 1/60 s") {
            body.physics.velocity.x = 300f
            val frame = 1f / 60f
            var prior = drawnX()
            val moves = Array<Float>()
            repeat(60) {
                world.process(true, entities, frame)
                val drawn = drawnX()
                moves.add(drawn - prior)
                prior = drawn
            }

            val stepped = Array<Float>()
            body.setX(0f)
            world = makeWorld().apply { interpolation = false }
            var priorX = 0f
            repeat(60) {
                world.process(true, entities, frame)
                stepped.add(body.getX() - priorX)
                priorX = body.getX()
            }

            val even = 300f * frame
            for (i in 1 until moves.size) moves[i] shouldBe (even plusOrMinus 1e-2f)
            (stepped.maxOf { it } - stepped.minOf { it } > even * 0.3f) shouldBe true
        }

        it("follows the real frame time with jitter of 1 ms") {
            body.physics.velocity.x = 300f
            val frames = floatArrayOf(0.0157f, 0.0177f, 0.0167f, 0.0162f, 0.0172f)
            var prior = drawnX()
            for (i in 0 until 50) {
                val delta = frames[i % frames.size]
                world.process(true, entities, delta)
                val drawn = drawnX()
                if (i > 0) (abs((drawn - prior) - 300f * delta) < 1e-2f) shouldBe true
                prior = drawn
            }
        }
    }
})
