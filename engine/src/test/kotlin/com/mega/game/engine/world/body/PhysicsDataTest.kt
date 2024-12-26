package com.mega.game.engine.world.body

import com.badlogic.gdx.math.Vector2
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PhysicsDataTest :
    DescribeSpec({
        describe("PhysicsData class") {
            val physicsData = PhysicsData()

            it("should have the correct initial properties") {
                physicsData.gravity shouldBe Vector2()
                physicsData.velocity shouldBe Vector2()
                physicsData.velocityClamp shouldBe Vector2(Float.MAX_VALUE, Float.MAX_VALUE)
                physicsData.frictionToApply shouldBe Vector2()
                physicsData.frictionOnSelf shouldBe Vector2(1f, 1f)
                physicsData.defaultFrictionOnSelf shouldBe Vector2(1f, 1f)
                physicsData.gravityOn shouldBe true
                physicsData.collisionOn shouldBe true
                physicsData.applyFrictionX shouldBe true
                physicsData.applyFrictionY shouldBe true
            }

            it("should reset correctly") {
                physicsData.velocity = Vector2(1f, 2f)
                physicsData.frictionOnSelf = Vector2(0.5f, 0.5f)

                physicsData.reset()

                physicsData.velocity shouldBe Vector2()
                physicsData.frictionOnSelf shouldBe Vector2(1f, 1f)
            }
        }
    })
