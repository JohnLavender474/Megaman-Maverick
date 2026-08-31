package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.MinsAndMaxes
import com.megaman.maverick.game.entities.special.DarknessGrid.Companion.MIN_ALPHA
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The tile simulation behind [DarknessV2], split out from the entity so that it can be exercised on its own.
 *
 * This holds no reference to the game, the camera, the entity system or a [com.badlogic.gdx.graphics.g2d.Batch] - it
 * is arithmetic over a grid of alphas, and depends only on pure engine types. [DarknessV2] keeps everything that does
 * touch the game: the ECS lifecycle, the sprite that draws this grid, event handling, and collecting light sources
 * from entities.
 *
 * The rule the whole thing turns on is one line in [step]: **a tile animates if and only if it was inside the camera
 * window on the immediately preceding tick, and otherwise snaps** to whichever terminal value `darkMode` names. That
 * single rule is what makes a room the player walks into fade in, while a room the player spawns into is simply black
 * from the first frame - the difference being only whether there was a previous window to have been inside.
 */
internal class DarknessGrid {

    companion object {
        const val MIN_ALPHA = 0f
        const val MAX_ALPHA = 1f

        private const val DARKEN_STEP_SCALAR = 2f
        private const val LIGHTEN_STEP_SCALAR = 1f
    }

    private class BlackTile(var currentAlpha: Float = MAX_ALPHA) {

        fun update(delta: Float, darken: Boolean) {
            currentAlpha += (if (darken) abs(DARKEN_STEP_SCALAR) else -abs(LIGHTEN_STEP_SCALAR)) * delta
            currentAlpha = currentAlpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }

        fun reset(dark: Boolean) {
            currentAlpha = if (dark) MAX_ALPHA else MIN_ALPHA
        }
    }

    private val bounds = GameRectangle()
    private val reusableMnMs = MinsAndMaxes()

    private lateinit var tiles: Matrix<BlackTile>

    private var dividedPPM = 0f

    // The window the previous [step] visited. This is the entire history the fade/snap rule needs: because the walk
    // only ever visits a rectangle, "was this tile on screen last step" is just "is this cell inside that rectangle",
    // so four ints stand in for what would otherwise be a per-tile flag. Empty (maxX < minX) means there was no
    // previous step, which is what makes a freshly reset grid snap rather than fade.
    private var prevMinX = 0
    private var prevMaxX = -1
    private var prevMinY = 0
    private var prevMaxY = -1

    /** The tile window visited by the most recent [step], for the sprite to draw. Empty until the first [step]. */
    var minX = 0
        private set
    var maxX = -1
        private set
    var minY = 0
        private set
    var maxY = -1
        private set

    val tileSize get() = dividedPPM

    fun worldX(column: Int) = bounds.getX() + column * dividedPPM

    fun worldY(row: Int) = bounds.getY() + row * dividedPPM

    /** Sizes the grid to [bounds] at [dividedPPM] pixels per tile and discards all prior tile state. */
    fun reset(bounds: GameRectangle, dividedPPM: Float) {
        this.bounds.set(bounds)
        this.dividedPPM = dividedPPM

        val rows = (bounds.getHeight() / dividedPPM).toInt()
        val columns = (bounds.getWidth() / dividedPPM).toInt()
        tiles = Matrix(rows, columns)

        prevMinX = 0
        prevMaxX = -1
        prevMinY = 0
        prevMaxY = -1

        minX = 0
        maxX = -1
        minY = 0
        maxY = -1
    }

    fun clear() = tiles.clear()

    val rows get() = tiles.rows

    val columns get() = tiles.columns

    /** The alpha at a cell, or [MIN_ALPHA] for a cell no light or walk has ever touched. */
    fun alphaAt(column: Int, row: Int) = tiles[column, row]?.currentAlpha ?: MIN_ALPHA

    private fun getTile(column: Int, row: Int): BlackTile {
        var tile = tiles[column, row]
        if (tile == null) {
            tile = BlackTile()
            tiles[column, row] = tile
        }
        return tile
    }

    private fun windowFor(rect: GameRectangle): MinsAndMaxes {
        val minX = ((rect.getX() - bounds.getX()) / dividedPPM).toInt().coerceIn(0, tiles.columns - 1)
        val minY = ((rect.getY() - bounds.getY()) / dividedPPM).toInt().coerceIn(0, tiles.rows - 1)
        val maxX = (ceil((rect.getMaxX() - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, tiles.columns - 1)
        val maxY = (ceil((rect.getMaxY() - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, tiles.rows - 1)
        return reusableMnMs.set(minX, minY, maxX, maxY)
    }

    /**
     * Brightens every tile the light reaches, never darkening one: a tile keeps whichever alpha is lower, so lights
     * compose and none of them can undo another.
     *
     * Membership is the closest-point-on-rect-to-circle-centre test that `Intersector.overlaps(Circle, Rectangle)`
     * performs, inlined here so that no shape objects are allocated per tile. Note that the *alpha* is then derived
     * from the distance to the tile's **centre**, which is a different and larger distance than the one the
     * membership test used - that distinction is inherited from the original implementation and is deliberate.
     */
    fun applyLight(center: Vector2, radius: Int, radiance: Float) {
        val radiusF = radius.toFloat()
        val radiusSq = radiusF * radiusF
        val alphaScalar = 1f / (radiusF * radiance)

        val minX = (((center.x - radiusF) - bounds.getX()) / dividedPPM).toInt().coerceIn(0, tiles.columns - 1)
        val minY = (((center.y - radiusF) - bounds.getY()) / dividedPPM).toInt().coerceIn(0, tiles.rows - 1)
        val maxX = (ceil(((center.x + radiusF) - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, tiles.columns - 1)
        val maxY = (ceil(((center.y + radiusF) - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, tiles.rows - 1)

        for (x in minX..maxX) {
            val tileMinX = bounds.getX() + x * dividedPPM
            val tileMaxX = tileMinX + dividedPPM

            // dx only depends on the column, so a column entirely outside the radius skips its whole row range
            val closestX = center.x.coerceIn(tileMinX, tileMaxX)
            val dx = center.x - closestX
            val dxSq = dx * dx
            if (dxSq > radiusSq) continue

            for (y in minY..maxY) {
                val tileMinY = bounds.getY() + y * dividedPPM
                val tileMaxY = tileMinY + dividedPPM

                val closestY = center.y.coerceIn(tileMinY, tileMaxY)
                val dy = center.y - closestY
                if (dxSq + dy * dy > radiusSq) continue

                val tile = getTile(x, y)

                val tileCenterX = tileMinX + dividedPPM * 0.5f
                val tileCenterY = tileMinY + dividedPPM * 0.5f
                val cdx = tileCenterX - center.x
                val cdy = tileCenterY - center.y
                val dist = sqrt(cdx * cdx + cdy * cdy)

                val alpha = (dist * alphaScalar).coerceIn(MIN_ALPHA, MAX_ALPHA)
                tile.currentAlpha = min(alpha, tile.currentAlpha)
            }
        }
    }

    /**
     * Advances every tile in the window derived from [camBounds] toward whichever terminal value [darkMode] names,
     * and returns whether any of them is now above [MIN_ALPHA] - i.e. whether there is anything to draw at all.
     */
    fun step(camBounds: GameRectangle, darkMode: Boolean, delta: Float): Boolean {
        val (minX, minY, maxX, maxY) = windowFor(camBounds)

        var anyLit = false

        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)

            val onScreenLastStep = x in prevMinX..prevMaxX && y in prevMinY..prevMaxY
            if (onScreenLastStep) tile.update(delta, darkMode) else tile.reset(darkMode)

            if (tile.currentAlpha > MIN_ALPHA) anyLit = true
        }

        prevMinX = minX
        prevMaxX = maxX
        prevMinY = minY
        prevMaxY = maxY

        this.minX = minX
        this.maxX = maxX
        this.minY = minY
        this.maxY = maxY

        return anyLit
    }
}
