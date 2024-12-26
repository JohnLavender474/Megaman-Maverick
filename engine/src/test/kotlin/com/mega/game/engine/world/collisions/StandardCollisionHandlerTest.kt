package com.mega.game.engine.world.collisions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StandardCollisionHandlerTest :
    DescribeSpec({
        describe("StandardCollisionHandler") {

            val dynamicBody = Body(BodyType.DYNAMIC)
            val staticBody = Body(BodyType.STATIC)

            beforeEach {
                dynamicBody.reset()
                dynamicBody.setPosition(0f, 0f)
                dynamicBody.physics.collisionOn = true

                staticBody.reset()
                staticBody.setPosition(0f, 0f)
                staticBody.physics.frictionToApply = Vector2(1f, 1f)
                staticBody.physics.collisionOn = true
            }

            it("should exit out of collision handling due to [collisionOn] being false") {
                dynamicBody.physics.collisionOn = false
                StandardCollisionHandler.handleCollision(dynamicBody, staticBody) shouldBe false
            }

            it("should exit out of collision handling due to no collision") {
                staticBody.set(0f, 0f, 5f, 5f)
                dynamicBody.set(5f, 5f, 5f, 5f)
                StandardCollisionHandler.handleCollision(dynamicBody, staticBody) shouldBe false
            }

            it("should handle collision by pushing dynamic fixtureBody down") {
                dynamicBody.set(0f, 0f, 5f, 5f)
                staticBody.set(0f, 2f, 5f, 5f)

                StandardCollisionHandler.handleCollision(dynamicBody, staticBody) shouldBe true

                dynamicBody.getX() shouldBe 0f
                dynamicBody.getY() shouldBe -3f
                dynamicBody.physics.frictionOnSelf shouldBe Vector2(2f, 1f)

                staticBody.getX() shouldBe 0f
                staticBody.getY() shouldBe 2f
                staticBody.physics.frictionOnSelf shouldBe Vector2(1f, 1f)
            }

            it("should handle collision by pushing dynamic fixtureBody left") {
                dynamicBody.set(0f, 0f, 5f, 5f)
                staticBody.set(3f, 0f, 5f, 5f)

                StandardCollisionHandler.handleCollision(dynamicBody, staticBody) shouldBe true

                dynamicBody.getX() shouldBe -2f
                dynamicBody.getY() shouldBe 0f
                dynamicBody.physics.frictionOnSelf shouldBe Vector2(1f, 2f)

                staticBody.getX() shouldBe 3f
                staticBody.getY() shouldBe 0f
                staticBody.physics.frictionOnSelf shouldBe Vector2(1f, 1f)
            }
        }
    })
