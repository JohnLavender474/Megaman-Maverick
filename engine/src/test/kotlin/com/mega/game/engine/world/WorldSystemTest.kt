package com.mega.game.engine.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.MockGameEntity
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.*
import com.mega.game.engine.world.collisions.ICollisionHandler
import com.mega.game.engine.world.contacts.IContactFilter
import com.mega.game.engine.world.contacts.IContactListener
import com.mega.game.engine.world.container.IWorldContainer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class WorldSystemTest : DescribeSpec({
    val mockContactListener = mockk<IContactListener>(relaxed = true)
    val mockWorldContainer = mockk<IWorldContainer>(relaxed = true)
    val mockCollisionHandler = mockk<ICollisionHandler>(relaxed = true)
    val fixedStep = 0.02f

    lateinit var body: Body
    lateinit var worldSystem: WorldSystem

    val outVec = Vector2()

    beforeEach {
        body = Body(BodyType.DYNAMIC)
        val bodyComponent = BodyComponent(body)
        val entity = MockGameEntity().apply { addComponent(bodyComponent) }

        worldSystem = WorldSystem(
            ppm = 1,
            fixedStep = fixedStep,
            worldContainerSupplier = { mockWorldContainer },
            contactListener = mockContactListener,
            collisionHandler = mockCollisionHandler,
            contactFilter = object : IContactFilter {
                override fun shouldProceedFiltering(fixture: IFixture): Boolean {
                    return true
                }

                override fun filter(fixture1: IFixture, fixture2: IFixture): Boolean {
                    return true
                }
            }
        )

        worldSystem.add(entity)
    }

    it("entity not qualify or be added") {
        worldSystem.qualifies(MockGameEntity()) shouldBe false
        worldSystem.contains(MockGameEntity()) shouldBe false
    }

    it("should add body and fixtures to world container correctly") {
        val fixture = Fixture(body, "Type", GameRectangle())
        body.addFixture(fixture)

        worldSystem.update(fixedStep)

        verify { mockWorldContainer.addBody(body) }
        verify { mockWorldContainer.addFixture(fixture) }
    }

    it("should process physics correctly - 1") {
        body.physics.gravity.set(-0.5f, -1f)
        worldSystem.update(fixedStep)

        body.getX() shouldBe -0.5f * fixedStep
        body.getY() shouldBe -1f * fixedStep
    }

    it("should process physics correctly - 2") {
        body.physics.velocity.set(5f, 10f)
        body.physics.gravity.set(-0.5f, -1f)
        worldSystem.update(fixedStep)

        body.getPosition(outVec).epsilonEquals(
            Vector2((5f - 0.5f) * fixedStep, (10f - 1f) * fixedStep), 0.01f
        ) shouldBe true
    }

    it("should filter contacts correctly") {
        val fixture1 = Fixture(mockk(), "Type1", mockk(), drawingColor = Color.RED)
        val fixture2 = Fixture(mockk(), "Type2", mockk(), drawingColor = Color.RED)
        val filterMap = ObjectMap<Any, ObjectSet<Any>>().apply {
            put("Type1", objectSetOf("Type2"))
        }

        val filteredSystem = WorldSystem(
            ppm = 1,
            fixedStep = fixedStep,
            worldContainerSupplier = { mockWorldContainer },
            contactListener = mockContactListener,
            collisionHandler = mockCollisionHandler,
            contactFilter = object : IContactFilter {
                override fun shouldProceedFiltering(fixture: IFixture): Boolean {
                    return filterMap.containsKey(fixture.getType())
                }

                override fun filter(fixture1: IFixture, fixture2: IFixture): Boolean {
                    return (filterMap.get(fixture1.getType())?.contains(fixture2.getType()) == true ||
                        filterMap.get(fixture2.getType())?.contains(fixture1.getType()) == true)
                }
            }
        )

        filteredSystem.filterContact(fixture1, fixture2) shouldBe true
        filteredSystem.filterContact(fixture1, fixture1) shouldBe false
    }

    it("should update fixture positions correctly - 1") {
        val fixture = Fixture(body, "Type", GameRectangle()).apply {
            offsetFromBodyAttachment.set(5f, 5f)
        }
        body.addFixture(fixture)
        worldSystem.update(fixedStep)
        fixture.getShape().getCenter(outVec) shouldBe Vector2(5f, 5f)
    }

    it("should update fixture positions correctly - 2") {
        body.setCenter(5f, 5f)
        val fixture = Fixture(body, "Type", GameRectangle()).apply {
            offsetFromBodyAttachment.set(5f, 5f)
        }
        body.addFixture(fixture)
        worldSystem.update(fixedStep)
        fixture.getShape().getCenter(outVec) shouldBe Vector2(10f, 10f)
    }
})
