package com.maverick.game.engine.world

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.IContactFilter
import com.maverick.game.engine.world.contacts.IContactListener
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WorldBodiesTest : DescribeSpec({

    val step = 1f / 150f

    fun world() = WorldSystem(
        ppm = 32,
        fixedStep = step,
        worldContainerSupplier = { SimpleGridWorldContainer(32) },
        contactListener = object : IContactListener {
            override fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
            override fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
            override fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
        },
        collisionHandler = object : ICollisionHandler {
            override fun handleCollision(body1: IBody, body2: IBody) = false
        },
        contactFilter = object : IContactFilter {
            override fun shouldProceedFiltering(fixture: IFixture) = true
            override fun filter(fixture1: IFixture, fixture2: IFixture) = true
        }
    )

    fun body(collisionOn: Boolean) = Body(BodyType.STATIC, 0f, 0f, 10f, 10f).apply {
        physics.collisionOn = collisionOn
    }

    fun IWorldBodies.toList() = (0 until bodyCount).map { getBody(it) }

    describe("WorldSystem as IWorldBodies") {

        it("gives the bodies that it put in the world container: the bodies with collision on") {
            val world = world()
            val solid = body(true)
            val ghost = body(false)
            world.add(MockGameEntity().apply { addComponent(BodyComponent(solid)) })
            world.add(MockGameEntity().apply { addComponent(BodyComponent(ghost)) })

            world.update(step)

            world.toList() shouldBe listOf(solid)
        }

        it("replaces the bodies on the next frame with a step, and keeps them on a frame with no step") {
            val world = world()
            val solid = body(true)
            world.add(MockGameEntity().apply { addComponent(BodyComponent(solid)) })
            world.update(step)

            solid.physics.collisionOn = false
            world.update(step * 0.1f)
            world.toList() shouldBe listOf(solid)

            world.update(step)
            world.bodyCount shouldBe 0
        }

        it("gives no bodies after a reset") {
            val world = world()
            world.add(MockGameEntity().apply { addComponent(BodyComponent(body(true))) })
            world.update(step)

            world.reset()

            world.bodyCount shouldBe 0
        }
    }
})
