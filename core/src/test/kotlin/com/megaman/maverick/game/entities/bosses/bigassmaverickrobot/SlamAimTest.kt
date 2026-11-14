package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val PPM = ConstVals.PPM.toFloat()
private const val DIAGONAL = 0.70710677f

class SlamAimTest : DescribeSpec({

    val arena = GameRectangle(318f * PPM, 55f * PPM, 14f * PPM, 11.75f * PPM)

    fun tiles(x: Float, y: Float) = Vector2(x * PPM, y * PPM)

    fun plan(fist: Vector2, target: Vector2) =
        SlamAim.plan(fist, target, arena, sideReach = 1f * PPM, floorReach = 0.5f * PPM, out = SlamPlan())

    fun Vector2.shouldPoint(x: Float, y: Float) {
        this.x shouldBe (x plusOrMinus 0.001f)
        this.y shouldBe (y plusOrMinus 0.001f)
    }

    fun Vector2.shouldBeAt(x: Float, y: Float) {
        this.x shouldBe (x * PPM plusOrMinus 0.02f * PPM)
        this.y shouldBe (y * PPM plusOrMinus 0.02f * PPM)
    }

    describe("the slam aim") {
        it("dives down from 4 tiles above the target when the fist is above it") {
            val plan = plan(tiles(325f, 64f), tiles(325f, 56f))
            plan.direction.shouldPoint(0f, -1f)
            plan.start.shouldBeAt(325f, 60f)
            plan.stop.shouldBeAt(325f, 55.5f)
            plan.hit.shouldBeAt(325f, 55f)
            plan.surface shouldBe ArenaSide.FLOOR
        }

        it("dives down-left and down-right to the floor") {
            val left = plan(tiles(328f, 59f), tiles(325f, 56f))
            left.direction.x shouldBe (-DIAGONAL plusOrMinus 0.001f)
            left.direction.y shouldBe (-DIAGONAL plusOrMinus 0.001f)
            left.start.shouldBeAt(325f + 4f * DIAGONAL, 56f + 4f * DIAGONAL)
            left.surface shouldBe ArenaSide.FLOOR
            left.stop.shouldBeAt(325f + 4f * DIAGONAL - (0.5f + 4f * DIAGONAL), 55.5f)

            val right = plan(tiles(322f, 59f), tiles(325f, 56f))
            right.direction.x shouldBe (DIAGONAL plusOrMinus 0.001f)
            right.direction.y shouldBe (-DIAGONAL plusOrMinus 0.001f)
            right.start.shouldBeAt(325f - 4f * DIAGONAL, 56f + 4f * DIAGONAL)
            right.surface shouldBe ArenaSide.FLOOR
            right.hit.y shouldBe (55f * PPM plusOrMinus 0.01f)
        }

        it("dives left and right to a wall, with the fist face on the wall") {
            val left = plan(tiles(328f, 60f), tiles(323f, 60f))
            left.direction.shouldPoint(-1f, 0f)
            left.start.shouldBeAt(327f, 60f)
            left.stop.shouldBeAt(319f, 60f)
            left.hit.shouldBeAt(318f, 60f)
            left.surface shouldBe ArenaSide.LEFT_WALL

            val right = plan(tiles(322f, 60f), tiles(327f, 60f))
            right.direction.shouldPoint(1f, 0f)
            right.stop.shouldBeAt(331f, 60f)
            right.hit.shouldBeAt(332f, 60f)
            right.surface shouldBe ArenaSide.RIGHT_WALL
        }

        it("shortens the lead along the line to keep the start 1.5 tiles inside the arena") {
            val plan = plan(tiles(325f, 66f), tiles(325f, 62.75f))
            plan.direction.shouldPoint(0f, -1f)
            plan.surface shouldBe ArenaSide.FLOOR
            plan.start.shouldBeAt(325f, 65.25f)
        }

        it("skips a direction whose start is less than the fist half size plus 1 tile from the target") {
            val plan = plan(tiles(325f, 66f), tiles(325f, 64f))
            withClue("the down and diagonal starts are 1.25 and 1.77 tiles from the target") {
                plan.direction.shouldPoint(-1f, 0f)
            }
            plan.start.shouldBeAt(329f, 64f)
            plan.surface shouldBe ArenaSide.LEFT_WALL
        }

        it("hits the wall when a floor player stands against it, and the floor one step away") {
            withClue("fists on the left-hand orbit") {
                listOf(tiles(322f, 61.5f), tiles(324f, 59.5f)).forEach { fist ->
                    plan(fist, tiles(319.1f, 55.75f)).surface shouldBe ArenaSide.LEFT_WALL
                    plan(fist, tiles(322f, 55.75f)).surface shouldBe ArenaSide.FLOOR
                }
            }
        }

        it("throws when the arena has no room for a dive") {
            val small = GameRectangle(318f * PPM, 55f * PPM, 4f * PPM, 4f * PPM)
            shouldThrow<IllegalStateException> {
                SlamAim.plan(tiles(320f, 58f), tiles(320f, 57f), small, 1f * PPM, 0.5f * PPM, SlamPlan())
            }
        }

        it("keeps every start inside the margin, with an inside target ahead on its line, and every hit on an edge") {
            val fists = Array<Vector2>()
            val targets = Array<Vector2>()
            var x = 318.25f
            while (x < 332f) {
                var y = 55.25f
                while (y < 66.75f) {
                    fists.add(tiles(x, y))
                    targets.add(tiles(x, y))
                    y += 1.5f
                }
                x += 1.25f
            }
            val margin = SlamAim.MARGIN * PPM
            fists.forEach { fist ->
                targets.forEach { target ->
                    val plan = plan(fist, target)
                    withClue("fist $fist target $target start ${plan.start}") {
                        plan.start.x shouldBeGreaterThanOrEqual arena.getX() + margin - 0.01f
                        plan.start.x shouldBeLessThanOrEqual arena.getMaxX() - margin + 0.01f
                        plan.start.y shouldBeGreaterThanOrEqual arena.getY() + margin - 0.01f
                        plan.start.y shouldBeLessThanOrEqual arena.getMaxY() - margin + 0.01f
                        val inside = target.x >= arena.getX() + margin && target.x <= arena.getMaxX() - margin &&
                            target.y >= arena.getY() + margin && target.y <= arena.getMaxY() - margin
                        val toTarget = Vector2(target).sub(plan.start)
                        toTarget.dot(plan.direction) shouldBeGreaterThanOrEqual 2f * PPM - 0.05f
                        if (inside && toTarget.len() > 0.01f) {
                            abs(toTarget.crs(plan.direction)) shouldBeLessThanOrEqual 0.05f * PPM
                            toTarget.dot(plan.direction) shouldBeGreaterThan 0f
                        }
                        when (plan.surface) {
                            ArenaSide.FLOOR -> plan.hit.y shouldBe (arena.getY() plusOrMinus 0.01f)
                            ArenaSide.LEFT_WALL -> plan.hit.x shouldBe (arena.getX() plusOrMinus 0.01f)
                            ArenaSide.RIGHT_WALL -> plan.hit.x shouldBe (arena.getMaxX() plusOrMinus 0.01f)
                        }
                    }
                }
            }
        }

        it("gives the floor at an exact corner") {
            val start = tiles(322f, 58.5f)
            val target = Vector2(start).add(-4f * DIAGONAL * PPM, -4f * DIAGONAL * PPM)
            val plan = plan(start, target)
            plan.direction.x shouldBe (-DIAGONAL plusOrMinus 0.001f)
            plan.start.shouldBeAt(322f, 58.5f)
            plan.stop.shouldBeAt(319f, 55.5f)
            plan.surface shouldBe ArenaSide.FLOOR
            plan.hit.shouldBeAt(319f, 55f)
        }

        it("breaks ties: down first, then the diagonals, then the sides") {
            val target = tiles(325f, 60f)
            val near = 0.92387953f
            val far = 0.38268343f
            withClue("all 5 starts are 4 tiles from a fist on the target") {
                plan(Vector2(target), target).direction.shouldPoint(0f, -1f)
            }
            withClue("down and down-left tie") {
                plan(Vector2(target).add(2f * far * PPM, 2f * near * PPM), target).direction.shouldPoint(0f, -1f)
            }
            withClue("down-left and left tie") {
                plan(Vector2(target).add(2f * near * PPM, 2f * far * PPM), target).direction
                    .shouldPoint(-DIAGONAL, -DIAGONAL)
            }
            withClue("left and right tie") {
                plan(tiles(325f, 57f), target).direction.shouldPoint(-1f, 0f)
            }
        }

        it("skips a direction whose start cannot have the target ahead") {
            val plan = plan(tiles(319.5f, 56.5f), tiles(318.5f, 55.6f))
            withClue("down-right and right start at the margin, to the right of the target") {
                (plan.direction.x <= 0f) shouldBe true
            }
        }
    }

    describe("the arena perimeter") {
        val out = Vector2()

        it("is the left wall, the floor and the right wall in one path") {
            ArenaPerimeter.length(arena) shouldBe (37.5f * PPM plusOrMinus 0.01f)
        }

        it("maps an offset to its point and side, with the floor at the corners") {
            ArenaPerimeter.pointAt(arena, 3f * PPM, out) shouldBe ArenaSide.LEFT_WALL
            out.shouldBeAt(318f, 63.75f)
            ArenaPerimeter.pointAt(arena, 11.75f * PPM, out) shouldBe ArenaSide.FLOOR
            out.shouldBeAt(318.5f, 55f)
            ArenaPerimeter.pointAt(arena, 18.75f * PPM, out) shouldBe ArenaSide.FLOOR
            out.shouldBeAt(325f, 55f)
            ArenaPerimeter.pointAt(arena, 25.75f * PPM, out) shouldBe ArenaSide.FLOOR
            out.shouldBeAt(331.5f, 55f)
            ArenaPerimeter.pointAt(arena, 30f * PPM, out) shouldBe ArenaSide.RIGHT_WALL
            out.shouldBeAt(332f, 59.25f)
        }

        it("keeps a pillar point 0.5 tile from each corner and wall top") {
            ArenaPerimeter.pointAt(arena, 0f, out) shouldBe ArenaSide.LEFT_WALL
            out.shouldBeAt(318f, 66.25f)
            ArenaPerimeter.pointAt(arena, 11.5f * PPM, out) shouldBe ArenaSide.LEFT_WALL
            out.shouldBeAt(318f, 55.5f)
            ArenaPerimeter.pointAt(arena, 26f * PPM, out) shouldBe ArenaSide.RIGHT_WALL
            out.shouldBeAt(332f, 55.5f)
            ArenaPerimeter.pointAt(arena, 37.5f * PPM, out) shouldBe ArenaSide.RIGHT_WALL
            out.shouldBeAt(332f, 66.25f)
        }

        it("gives the offset of a hit point on each side") {
            ArenaPerimeter.offsetOf(arena, tiles(318f, 60f), ArenaSide.LEFT_WALL) shouldBe (6.75f * PPM plusOrMinus 0.01f)
            ArenaPerimeter.offsetOf(arena, tiles(325f, 55f), ArenaSide.FLOOR) shouldBe (18.75f * PPM plusOrMinus 0.01f)
            ArenaPerimeter.offsetOf(arena, tiles(332f, 60f), ArenaSide.RIGHT_WALL) shouldBe (30.75f * PPM plusOrMinus 0.01f)
        }

        fun walk(offset: Float, direction: Float): Array<ArenaSide> {
            val row = ShockWaveRow()
            val sides = Array<ArenaSide>()
            row.start(offset, direction, life = 10f)
            repeat(1000) {
                row.update(0.01f, 0f, ArenaPerimeter.length(arena)) { sides.add(ArenaPerimeter.pointAt(arena, it, out)) }
            }
            return sides
        }

        it("walks a floor wave around the corner and up the wall to the wall top, in both directions") {
            val hit = ArenaPerimeter.offsetOf(arena, tiles(325f, 55f), ArenaSide.FLOOR)
            val left = walk(hit, -1f)
            left.size shouldBe 18
            left.first() shouldBe ArenaSide.FLOOR
            left.last() shouldBe ArenaSide.LEFT_WALL
            left.count { it == ArenaSide.LEFT_WALL } shouldBe 11
            left.count { it == ArenaSide.RIGHT_WALL } shouldBe 0
            val right = walk(hit, 1f)
            right.size shouldBe 18
            right.last() shouldBe ArenaSide.RIGHT_WALL
            right.count { it == ArenaSide.RIGHT_WALL } shouldBe 11
            right.count { it == ArenaSide.LEFT_WALL } shouldBe 0
        }

        it("walks a wall wave up to the wall top, and down onto the floor and up the far wall") {
            val hit = ArenaPerimeter.offsetOf(arena, tiles(318f, 60f), ArenaSide.LEFT_WALL)
            val up = walk(hit, -1f)
            up.size shouldBe 6
            up.all { it == ArenaSide.LEFT_WALL } shouldBe true
            val down = walk(hit, 1f)
            down.size shouldBe 30
            down.count { it == ArenaSide.LEFT_WALL } shouldBe 4
            down.count { it == ArenaSide.FLOOR } shouldBe 15
            down.count { it == ArenaSide.RIGHT_WALL } shouldBe 11
        }
    }
})
