package com.mega.game.engine.world.body

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class BodyTest :
    DescribeSpec({
        describe("Body") {
            val bodyType = BodyType.ABSTRACT
            val physicsData = PhysicsData()
            val fixtures = orderedMapOf<Any, OrderedSet<IFixture>>()
            val props = Properties()
            val body = Body(bodyType, physics = physicsData, fixtures = fixtures, properties = props)
            val out = Vector2()

            it("should have the correct initial properties") {
                body.type shouldBe bodyType
                body.physics shouldBe physicsData
                body.fixtures shouldBe fixtures
                body.properties shouldBe props
                body.preProcess shouldBe OrderedMap<Any, Updatable>()
                body.postProcess shouldBe OrderedMap<Any, Updatable>()
            }

            it("should have the correct default values for optional properties") {
                body.getX() shouldBe 0f
                body.getY() shouldBe 0f
                body.getWidth() shouldBe 0f
                body.getHeight() shouldBe 0f
                body.hashCode() shouldBe System.identityHashCode(body)
            }

            it("should check if it has a given fixtureBody type correctly") {
                (body.type == bodyType) shouldBe true
            }

            it("should get user data correctly") {
                val key = "key"
                val value = "value"
                body.putProperty(key, value)
                val propsValue = body.getProperty(key, String::class)
                propsValue shouldBe value
            }

            it("should reset correctly") {
                val mockPhysicsData = spyk<PhysicsData>()
                every { mockPhysicsData.reset() } just Runs
                body.physics = mockPhysicsData
                body.reset()
                verify { body.physics.reset() }
            }

            it("should have proper equals and hashCode implementations") {
                val body1 = Body(bodyType, physics = physicsData, fixtures = fixtures, properties = props)
                val body2 = Body(bodyType, physics = physicsData, fixtures = fixtures, properties = props)
                (body1 == body2) shouldBe false
                body1.hashCode() shouldBe System.identityHashCode(body1)
                body2.hashCode() shouldBe System.identityHashCode(body2)
                body1.hashCode() shouldNotBe body2.hashCode()
            }

            it("should set fixtures relative to the body") {
                body.setSize(10f)
                body.setCenter(0f, 0f)

                val bottomFixture = Fixture(body, "bottom", GameRectangle().setSize(1f))
                bottomFixture.offsetFromBodyAttachment.y = -5f
                body.addFixture(bottomFixture)

                val topFixture = Fixture(body, "top", GameRectangle().setSize(1f))
                topFixture.offsetFromBodyAttachment.y = 5f
                body.addFixture(topFixture)

                val leftFixture = Fixture(body, "left", GameRectangle().setSize(1f))
                leftFixture.offsetFromBodyAttachment.x = -5f
                body.addFixture(leftFixture)

                val rightFixture = Fixture(body, "right", GameRectangle().setSize(1f))
                rightFixture.offsetFromBodyAttachment.x = 5f
                body.addFixture(rightFixture)

                body.direction = Direction.UP

                var shape = bottomFixture.getShape()
                println("shape = $shape")
                shape.getCenter(out) shouldBe Vector2(0f, -5f)
                shape = topFixture.getShape()
                shape.getCenter(out) shouldBe Vector2(0f, 5f)
                shape = leftFixture.getShape()
                shape.getCenter(out) shouldBe Vector2(-5f, 0f)
                shape = rightFixture.getShape()
                shape.getCenter(out) shouldBe Vector2(5f, 0f)

                body.direction = Direction.LEFT

                bottomFixture.getShape().getCenter(out) shouldBe Vector2(5f, 0f)
                topFixture.getShape().getCenter(out) shouldBe Vector2(-5f, 0f)
                leftFixture.getShape().getCenter(out) shouldBe Vector2(0f, -5f)
                rightFixture.getShape().getCenter(out) shouldBe Vector2(0f, 5f)

                body.direction = Direction.DOWN

                bottomFixture.getShape().getCenter(out) shouldBe Vector2(0f, 5f)
                topFixture.getShape().getCenter(out) shouldBe Vector2(0f, -5f)
                leftFixture.getShape().getCenter(out) shouldBe Vector2(5f, 0f)
                rightFixture.getShape().getCenter(out) shouldBe Vector2(-5f, 0f)

                body.direction = Direction.RIGHT

                bottomFixture.getShape().getCenter(out) shouldBe Vector2(-5f, 0f)
                topFixture.getShape().getCenter(out) shouldBe Vector2(5f, 0f)
                leftFixture.getShape().getCenter(out) shouldBe Vector2(0f, 5f)
                rightFixture.getShape().getCenter(out) shouldBe Vector2(0f, -5f)
            }
        }
    })
