package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class MovedEntity : GameEntity(mockk(relaxed = true)), IBodyEntity {

    var overGround = true
    var facingBlock = false
    var groundSliding = false
    var maxSpeedReads = 0

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        addComponent(
            PostActionMomentumComponent(
                MegamanValues.POST_ACTION_MOMENTUM_DUR,
                MegamanValues.POST_ACTION_BURST_RESISTANCE,
                maxSpeed = {
                    maxSpeedReads++
                    100f
                },
                overGround = { overGround },
                facingBlock = { facingBlock },
                groundSliding = { groundSliding }
            )
        )
    }

    val momentum: PostActionMomentumComponent
        get() = getComponent(PostActionMomentumComponent::class)!!

    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class PostActionMomentumTest : DescribeSpec({

    lateinit var entity: MovedEntity

    beforeEach { entity = MovedEntity() }

    describe("shouldPreserveMomentum") {
        it("is false on snow") {
            entity.body.setBodySense(BodySense.FEET_ON_SNOW, true)
            entity.body.setBodySense(BodySense.FEET_ON_ICE, true)

            entity.shouldPreserveMomentum(false) shouldBe false
        }

        it("is true on ice") {
            entity.body.setBodySense(BodySense.FEET_ON_ICE, true)

            entity.shouldPreserveMomentum(true) shouldBe true
        }

        it("reads the timer only when asked to") {
            entity.shouldPreserveMomentum(false) shouldBe true
            entity.shouldPreserveMomentum(true) shouldBe false

            entity.preserveMomentum()

            entity.shouldPreserveMomentum(true) shouldBe true
        }
    }

    describe("stopMomentum") {
        it("ends the timer and stops the entity on the axis of its direction") {
            entity.body.physics.velocity.set(5f, 6f)
            entity.preserveMomentum()

            entity.stopMomentum(true)

            entity.momentum.timer.isFinished() shouldBe true
            entity.body.physics.velocity shouldBe Vector2(0f, 6f)

            entity.body.direction = Direction.LEFT
            entity.stopMomentum(true)

            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }

        it("keeps the velocity when it does not stop the entity") {
            entity.body.physics.velocity.set(5f, 6f)

            entity.stopMomentum(false)

            entity.body.physics.velocity shouldBe Vector2(5f, 6f)
        }
    }

    describe("killPostActionMomentum") {
        it("clamps on the axis of the direction and clears the pending kill") {
            entity.body.physics.velocity.set(500f, 500f)
            entity.requestMomentumKill()

            entity.killPostActionMomentum()

            entity.body.physics.velocity shouldBe Vector2(100f, 500f)
            entity.momentum.pendingKill shouldBe false

            entity.body.direction = Direction.RIGHT
            entity.killPostActionMomentum()

            entity.body.physics.velocity shouldBe Vector2(100f, 100f)
        }
    }

    describe("updatePostActionMomentum") {
        it("runs the pending kill only over ground and off ice") {
            entity.body.physics.velocity.set(500f, 0f)
            entity.requestMomentumKill()

            entity.overGround = false
            entity.updatePostActionMomentum(0.01f, true)
            entity.momentum.pendingKill shouldBe true

            entity.overGround = true
            entity.body.setBodySense(BodySense.FEET_ON_ICE, true)
            entity.updatePostActionMomentum(0.01f, true)
            entity.momentum.pendingKill shouldBe true

            entity.body.setBodySense(BodySense.FEET_ON_ICE, false)
            entity.updatePostActionMomentum(0.01f, true)
            entity.momentum.pendingKill shouldBe false
            entity.body.physics.velocity.x shouldBe 100f
        }

        it("reads the max speed only on a kill") {
            entity.preserveMomentum()

            entity.updatePostActionMomentum(0.01f, false)

            entity.maxSpeedReads shouldBe 0
        }

        it("decays only when not running, over ground, with the timer on, not sliding and off ice") {
            fun decays(setup: () -> Unit, running: Boolean = false): Boolean {
                entity = MovedEntity()
                entity.preserveMomentum()
                entity.body.physics.velocity.set(50f, 0f)
                setup()
                entity.updatePostActionMomentum(0.01f, running)
                return entity.body.physics.velocity.x != 50f
            }

            decays({}) shouldBe true
            decays({}, running = true) shouldBe false
            decays({ entity.overGround = false }) shouldBe false
            decays({ entity.stopMomentum(false) }) shouldBe false
            decays({ entity.groundSliding = true }) shouldBe false
            decays({ entity.body.setBodySense(BodySense.FEET_ON_ICE, true) }) shouldBe false
        }

        it("in the air, stops the momentum at a block and stops the entity when not running") {
            entity.overGround = false
            entity.facingBlock = true
            entity.preserveMomentum()
            entity.body.physics.velocity.set(50f, 0f)

            entity.updatePostActionMomentum(0.01f, true)
            entity.momentum.timer.isFinished() shouldBe true
            entity.body.physics.velocity.x shouldBe 50f

            entity.updatePostActionMomentum(0.01f, false)
            entity.body.physics.velocity.x shouldBe 0f
        }
    }

    describe("PostActionMomentumComponent") {
        it("has the same state after reset as a new instance, and keeps its hooks") {
            val maxSpeed = entity.momentum.maxSpeed
            entity.preserveMomentum()
            entity.requestMomentumKill()

            entity.momentum.reset()

            entity.momentum.timer.isFinished() shouldBe true
            entity.momentum.pendingKill shouldBe false
            (entity.momentum.maxSpeed === maxSpeed) shouldBe true
        }
    }
})
