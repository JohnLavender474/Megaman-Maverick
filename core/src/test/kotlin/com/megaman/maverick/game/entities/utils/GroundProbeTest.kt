package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.BodyLabel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GroundProbeTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()
    val lead = 0.15f * ppm
    val maxDrop = 4f * ppm

    lateinit var world: GroundTestWorld

    beforeEach { world = GroundTestWorld() }

    fun checkAhead(x: Float, y: Float, facing: Facing = Facing.RIGHT, direction: Direction = Direction.UP): GroundAhead {
        val owner = world.owner(x, y)
        return GroundProbe(owner, world.services.levelState).checkAhead(facing, direction, lead, maxDrop)
    }

    describe("checkAhead with gravity down (Direction.UP)") {

        it("gives FLOOR on a long floor") {
            world.block(0f, 0f, 10f, 1f)
            checkAhead(2f, 1f) shouldBe GroundAhead.FLOOR
        }

        it("gives WALL when a solid blocks the next step") {
            world.block(0f, 0f, 10f, 1f)
            world.block(3f, 1f, 1f, 2f)
            checkAhead(2f, 1f) shouldBe GroundAhead.WALL
        }

        it("gives SAFE_DROP onto lower ground within the max drop") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, -2f, 7f, 1f)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.SAFE_DROP
        }

        it("gives PIT when an active DEATH is in the fall column") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, -2f, 7f, 1f)
            world.death(3f, -1f, 7f, 1f)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.PIT
        }

        it("ignores an inactive DEATH") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, -2f, 7f, 1f)
            world.death(3f, -1f, 7f, 1f, active = false)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.SAFE_DROP
        }

        it("gives PIT when no ground is below the step") {
            world.block(0f, 0f, 3f, 1f)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.PIT
        }

        it("gives PIT when the lower ground is deeper than the max drop") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, -6f, 7f, 1f)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.PIT
        }

        it("gives FLOOR while the strip past the front edge plus the lead is over ground") {
            world.block(0f, 0f, 3f, 1f)
            checkAhead(1.8f, 1f) shouldBe GroundAhead.FLOOR
        }

        it("gives PIT once the front edge plus the lead passes the ledge, with the whole body still on it") {
            world.block(0f, 0f, 3f, 1f)
            checkAhead(1.9f, 1f) shouldBe GroundAhead.PIT
        }

        it("takes a longer lead as an earlier stop") {
            world.block(0f, 0f, 3f, 1f)
            val owner = world.owner(1.5f, 1f)
            val probe = GroundProbe(owner, world.services.levelState)
            probe.checkAhead(Facing.RIGHT, Direction.UP, 0.1f * ppm, maxDrop) shouldBe GroundAhead.FLOOR
            probe.checkAhead(Facing.RIGHT, Direction.UP, 0.5f * ppm, maxDrop) shouldBe GroundAhead.PIT
        }

        it("reads the drop below a catch column past the strip, not under the body") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3.2f, -2f, 0.3f, 1f)
            checkAhead(1.9f, 1f) shouldBe GroundAhead.SAFE_DROP
        }

        it("gives PIT when a DEATH lies in the catch column above the lower ground") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, -3f, 7f, 1f)
            world.death(3.5f, -2f, 0.5f, 1f)
            checkAhead(1.9f, 1f) shouldBe GroundAhead.PIT
        }

        it("checks behind the body when facing left") {
            world.block(2f, 0f, 8f, 1f)
            checkAhead(1.4f, 1f, facing = Facing.LEFT) shouldBe GroundAhead.PIT
            checkAhead(1.4f, 1f, facing = Facing.RIGHT) shouldBe GroundAhead.FLOOR
        }

        it("stands on a one-way platform") {
            world.block(0f, 0f, 10f, 1f, label = BodyLabel.COLLIDE_DOWN_ONLY)
            checkAhead(2f, 1f) shouldBe GroundAhead.FLOOR
        }

        it("does not take a one-way platform in the path as a wall") {
            world.block(0f, 0f, 10f, 1f)
            world.block(3f, 1.4f, 2f, 0.2f, label = BodyLabel.COLLIDE_DOWN_ONLY)
            checkAhead(2f, 1f) shouldBe GroundAhead.FLOOR
        }

        it("does not stand on a collide-up-only block") {
            world.block(0f, 0f, 3f, 1f)
            world.block(3f, 0f, 7f, 1f, label = BodyLabel.COLLIDE_UP_ONLY)
            checkAhead(2.6f, 1f) shouldBe GroundAhead.PIT
        }
    }

    describe("checkAhead with turned gravity") {

        it("Direction.DOWN: walks on a ceiling, forward is +x") {
            world.block(0f, 1f, 3f, 1f)
            checkAhead(1f, 0f, direction = Direction.DOWN) shouldBe GroundAhead.FLOOR
            checkAhead(2.6f, 0f, direction = Direction.DOWN) shouldBe GroundAhead.PIT
        }

        it("Direction.DOWN: a lower ledge is above in world space") {
            world.block(0f, 1f, 3f, 1f)
            world.block(3f, 2f, 7f, 1f)
            checkAhead(2.6f, 0f, direction = Direction.DOWN) shouldBe GroundAhead.SAFE_DROP
        }

        it("Direction.LEFT: walks on a wall at +x, forward is +y") {
            world.block(1f, 0f, 1f, 3f)
            checkAhead(0f, 1f, direction = Direction.LEFT) shouldBe GroundAhead.FLOOR
            checkAhead(0f, 2.6f, direction = Direction.LEFT) shouldBe GroundAhead.PIT
        }

        it("Direction.LEFT: a solid ahead on +y is a wall") {
            world.block(1f, 0f, 1f, 10f)
            world.block(-2f, 3f, 3f, 1f)
            checkAhead(0f, 2f, direction = Direction.LEFT) shouldBe GroundAhead.WALL
        }

        it("Direction.RIGHT: walks on a wall at -x, forward is +y") {
            world.block(-1f, 0f, 1f, 3f)
            checkAhead(0f, 1f, direction = Direction.RIGHT) shouldBe GroundAhead.FLOOR
            checkAhead(0f, 2.6f, direction = Direction.RIGHT) shouldBe GroundAhead.PIT
        }

        it("Direction.RIGHT: facing left looks toward -y") {
            world.block(-1f, 2f, 1f, 8f)
            checkAhead(0f, 1.4f, facing = Facing.LEFT, direction = Direction.RIGHT) shouldBe GroundAhead.PIT
            checkAhead(0f, 1.4f, facing = Facing.RIGHT, direction = Direction.RIGHT) shouldBe GroundAhead.FLOOR
        }
    }

    describe("the world container") {

        it("throws when the level has no world container") {
            world.services.levelState.worldContainer = null
            val owner = world.owner(2f, 1f)
            val probe = GroundProbe(owner, world.services.levelState)
            shouldThrow<IllegalStateException> {
                probe.checkAhead(Facing.RIGHT, Direction.UP, lead, maxDrop)
            }
        }
    }
})
