package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DarknessGridTest : DescribeSpec({

    val tileSize = 16f
    val delta = 1f / 60f

    // a 10x10 grid of 16px tiles
    fun bounds() = GameRectangle(0f, 0f, 160f, 160f)

    fun grid() = DarknessGrid().apply { reset(bounds(), tileSize) }

    // covers the whole grid: columns 0..9, rows 0..9
    fun fullCam() = GameRectangle(0f, 0f, 160f, 160f)

    fun DarknessGrid.windowAlphas() =
        (minX..maxX).flatMap { x -> (minY..maxY).map { y -> alphaAt(x, y) } }

    fun DarknessGrid.darkenFully() = repeat(40) { step(fullCam(), darkMode = true, delta) }

    describe("the fade/snap rule") {

        // A tile animates if and only if it was inside the window on the immediately preceding step, and
        // otherwise snaps. Every case below is that one rule seen from a different angle.

        it("snaps straight to black on the very first step when the room is already dark") {
            // there is no previous window to have been inside of, so nothing fades in - this is a level
            // starting, or the player respawning, inside a darkened room
            val grid = grid()

            grid.step(fullCam(), darkMode = true, delta)

            grid.windowAlphas().forEach { it shouldBe DarknessGrid.MAX_ALPHA }
        }

        it("stays clear on the very first step when the room is lit") {
            val grid = grid()

            grid.step(fullCam(), darkMode = false, delta)

            grid.windowAlphas().forEach { it shouldBe DarknessGrid.MIN_ALPHA }
        }

        it("fades in rather than snapping once the window has been established") {
            // the player walking into a darkened room: the window was already being tracked through the
            // transition, so the tiles animate from clear instead of jumping to black
            val grid = grid()
            repeat(3) { grid.step(fullCam(), darkMode = false, delta) }

            grid.step(fullCam(), darkMode = true, delta)

            grid.windowAlphas().forEach { alpha ->
                (alpha > DarknessGrid.MIN_ALPHA && alpha < DarknessGrid.MAX_ALPHA) shouldBe true
            }
        }

        it("fades in uniformly - never part snapped and part fading") {
            // the torn half-instant, half-fading transition: every tile in the window must agree
            val grid = grid()
            repeat(3) { grid.step(fullCam(), darkMode = false, delta) }

            grid.step(fullCam(), darkMode = true, delta)

            grid.windowAlphas().distinct().size shouldBe 1
        }

        it("fades a tile that was in the previous window and snaps one that was not") {
            val grid = grid()

            // columns 0..4 only
            val left = GameRectangle(0f, 0f, 64f, 160f)
            repeat(40) { grid.step(left, darkMode = true, delta) }

            // columns 4..9 - only column 4 overlaps what was just on screen
            val right = GameRectangle(64f, 0f, 96f, 160f)
            grid.step(right, darkMode = false, delta)

            // in the previous window, so it eases off full black
            val carriedOver = grid.alphaAt(4, 0)
            (carriedOver > 0.9f && carriedOver < DarknessGrid.MAX_ALPHA) shouldBe true

            // not in the previous window, so it snaps to the lit terminal value
            grid.alphaAt(5, 0) shouldBe DarknessGrid.MIN_ALPHA
        }
    }

    describe("animation rates") {

        it("reaches full black in about half a second") {
            val grid = grid()
            grid.step(fullCam(), darkMode = false, delta)

            repeat(35) { grid.step(fullCam(), darkMode = true, delta) }

            grid.windowAlphas().forEach { it shouldBe DarknessGrid.MAX_ALPHA }
        }

        it("takes about twice as long to clear as it does to darken") {
            val grid = grid()
            grid.step(fullCam(), darkMode = false, delta)
            repeat(35) { grid.step(fullCam(), darkMode = true, delta) }

            // half a second of clearing leaves it visibly dark, where half a second of darkening was enough
            // to reach full black
            repeat(30) { grid.step(fullCam(), darkMode = false, delta) }
            (grid.alphaAt(0, 0) > 0.4f) shouldBe true

            // a full second clears it
            repeat(35) { grid.step(fullCam(), darkMode = false, delta) }
            grid.windowAlphas().forEach { it shouldBe DarknessGrid.MIN_ALPHA }
        }
    }

    describe("anyLit reporting") {

        it("is false while every tile in the window is clear") {
            val grid = grid()

            grid.step(fullCam(), darkMode = false, delta) shouldBe false
        }

        it("is true as soon as anything starts darkening") {
            val grid = grid()
            grid.step(fullCam(), darkMode = false, delta)

            grid.step(fullCam(), darkMode = true, delta) shouldBe true
        }
    }

    describe("applyLight") {

        it("drives the tile under the light fully clear") {
            val grid = grid()
            grid.darkenFully()

            // centred exactly on tile (0, 0)
            grid.applyLight(Vector2(8f, 8f), radius = 32, radiance = 1f)

            grid.alphaAt(0, 0) shouldBe DarknessGrid.MIN_ALPHA
        }

        it("brightens less the further a tile is from the light") {
            val grid = grid()
            grid.darkenFully()

            grid.applyLight(Vector2(8f, 8f), radius = 64, radiance = 1f)

            (grid.alphaAt(0, 0) < grid.alphaAt(1, 0)) shouldBe true
            (grid.alphaAt(1, 0) < grid.alphaAt(2, 0)) shouldBe true
        }

        it("leaves tiles beyond the radius untouched") {
            val grid = grid()
            grid.darkenFully()

            grid.applyLight(Vector2(8f, 8f), radius = 32, radiance = 1f)

            grid.alphaAt(8, 0) shouldBe DarknessGrid.MAX_ALPHA
        }

        it("never darkens a tile another light already brightened") {
            val grid = grid()
            grid.darkenFully()

            grid.applyLight(Vector2(8f, 8f), radius = 32, radiance = 1f)
            grid.alphaAt(0, 0) shouldBe DarknessGrid.MIN_ALPHA

            // a dimmer, more distant light that on its own would leave this tile part-dark
            grid.applyLight(Vector2(40f, 8f), radius = 64, radiance = 1f)

            grid.alphaAt(0, 0) shouldBe DarknessGrid.MIN_ALPHA
        }

        it("lights a tile whose corner is inside the radius even when its centre is not") {
            // the membership test is closest-point-to-the-rect, not distance-to-the-centre; testing the
            // centre instead would shrink every lit area by up to half a tile
            val grid = grid()
            grid.darkenFully()

            // tile (2, 0) spans x 32..48 with its centre at x 40. The light sits at x 30 with radius 3:
            // the tile's near edge is 2 away, well inside, but its centre is 10 away, well outside.
            grid.applyLight(Vector2(30f, 8f), radius = 3, radiance = 10f)

            (grid.alphaAt(2, 0) < DarknessGrid.MAX_ALPHA) shouldBe true

            // the next tile over is outside the radius by any measure
            grid.alphaAt(3, 0) shouldBe DarknessGrid.MAX_ALPHA
        }
    }

    describe("geometry") {

        it("sizes itself from the bounds and tile size") {
            val grid = grid()

            grid.rows shouldBe 10
            grid.columns shouldBe 10
        }

        it("starts with an empty window") {
            val grid = grid()

            (grid.maxX < grid.minX) shouldBe true
            (grid.maxY < grid.minY) shouldBe true
        }

        it("clamps the window when the camera sits off the grid entirely") {
            val grid = grid()

            grid.step(GameRectangle(1000f, 1000f, 64f, 64f), darkMode = true, delta)

            grid.minX shouldBe 9
            grid.maxX shouldBe 9
            grid.minY shouldBe 9
            grid.maxY shouldBe 9
        }

        it("maps cells back to world positions") {
            val grid = grid()

            grid.tileSize shouldBe tileSize
            grid.worldX(3) shouldBe 48f
            grid.worldY(2) shouldBe 32f
        }

        it("discards all prior state when reset") {
            val grid = grid()
            grid.darkenFully()

            grid.reset(bounds(), tileSize)

            (grid.maxX < grid.minX) shouldBe true
            grid.alphaAt(0, 0) shouldBe DarknessGrid.MIN_ALPHA

            // and the first step after a reset snaps again, rather than carrying the old history
            grid.step(fullCam(), darkMode = true, delta)
            grid.windowAlphas().forEach { it shouldBe DarknessGrid.MAX_ALPHA }
        }
    }
})
