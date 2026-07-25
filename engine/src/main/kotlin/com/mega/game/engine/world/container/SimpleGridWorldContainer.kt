package com.mega.game.engine.world.container

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.MinsAndMaxes
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture

/**
 * A uniform grid broad-phase container.
 *
 * The grid is rebuilt from scratch every physics cycle, so the hot path here is [addBody] /
 * [addFixture] followed by [clear]. Both are allocation-free in steady state: cells are keyed by a
 * packed `Long` rather than an object, and [clear] empties each cell in place rather than dropping
 * it, so a cell's backing array is allocated once and then reused for the lifetime of the container.
 *
 * Cells hold [OrderedSet], so a duplicate add collapses. Queries walk the set's backing
 * `orderedItems()` array by index rather than taking an iterator: that allocates nothing, and it is
 * safe to nest, which iterating the set directly is not (libGDX reuses cached iterators and fails
 * once nesting passes two levels deep). Range queries additionally de-duplicate across cells via
 * [tempBodySet] / [tempFixtureSet], since one body can occupy several cells.
 */
class SimpleGridWorldContainer(
    var ppm: Int,
    var bufferOffset: Int = 0,
    var adjustForExactGridMatch: Boolean = true,
    var floatRoundingError: Float = MathUtils.FLOAT_ROUNDING_ERROR
) : IWorldContainer {

    private val bodyMap = LongMap<OrderedSet<IBody>>()
    private val fixtureMap = LongMap<OrderedSet<IFixture>>()

    private val reusableGameRect = GameRectangle()
    private val reusableMnMs = MinsAndMaxes()

    private val tempBodySet = ObjectSet<IBody>()
    private val tempFixtureSet = ObjectSet<IFixture>()

    private constructor(
        ppm: Int,
        bufferOffset: Int,
        bodyMap: LongMap<OrderedSet<IBody>>,
        fixtureMap: LongMap<OrderedSet<IFixture>>,
        adjustForExactGridMatch: Boolean,
        floatRoundingError: Float
    ) : this(ppm, bufferOffset, adjustForExactGridMatch, floatRoundingError) {
        // deep copy: the source reuses and empties its cells every cycle, so sharing them would let it
        // mutate this snapshot out from under whoever holds it -- including the pathfinder threads,
        // which read a `copy()` while the render thread keeps rebuilding the original
        bodyMap.forEach { this.bodyMap.put(it.key, OrderedSet(it.value)) }
        fixtureMap.forEach { this.fixtureMap.put(it.key, OrderedSet(it.value)) }
    }

    // cells are addressed by signed coordinates that can go negative, so both halves of the key must
    // survive round-tripping; a `Long` covers the full `Int` range on each axis with no offset games
    private fun packKey(column: Int, row: Int) = (column.toLong() shl 32) or (row.toLong() and 0xFFFFFFFFL)

    private fun adjustCoordinateIfNeeded(value: Float, isMinValue: Boolean) =
        if (adjustForExactGridMatch && MathUtils.isEqual(value % 1f, 0f, floatRoundingError)) {
            if (isMinValue) value + floatRoundingError
            else value - floatRoundingError
        } else value

    private fun getMinsAndMaxes(bounds: GameRectangle, out: MinsAndMaxes): MinsAndMaxes {
        val adjustedMinX = adjustCoordinateIfNeeded(bounds.getX(), true)
        val adjustedMinY = adjustCoordinateIfNeeded(bounds.getY(), true)
        val adjustedMaxX = adjustCoordinateIfNeeded(bounds.getMaxX(), false)
        val adjustedMaxY = adjustCoordinateIfNeeded(bounds.getMaxY(), false)

        val minX = MathUtils.floor(adjustedMinX / ppm.toFloat()) - bufferOffset
        val minY = MathUtils.floor(adjustedMinY / ppm.toFloat()) - bufferOffset
        val maxX = MathUtils.floor(adjustedMaxX / ppm.toFloat()) + bufferOffset
        val maxY = MathUtils.floor(adjustedMaxY / ppm.toFloat()) + bufferOffset

        return out.set(minX, minY, maxX, maxY)
    }

    override fun addBody(body: IBody): Boolean {
        val bounds = body.getBounds(reusableGameRect)
        val (minX, minY, maxX, maxY) = getMinsAndMaxes(bounds, reusableMnMs)
        for (column in minX..maxX) for (row in minY..maxY) {
            val key = packKey(column, row)
            var cell = bodyMap.get(key)
            if (cell == null) {
                cell = OrderedSet()
                bodyMap.put(key, cell)
            }
            cell.add(body)
        }
        return true
    }

    override fun addFixture(fixture: IFixture): Boolean {
        val bounds = fixture.getShape().getBoundingRectangle(reusableGameRect)
        val (minX, minY, maxX, maxY) = getMinsAndMaxes(bounds, reusableMnMs)
        for (column in minX..maxX) for (row in minY..maxY) {
            val key = packKey(column, row)
            var cell = fixtureMap.get(key)
            if (cell == null) {
                cell = OrderedSet()
                fixtureMap.put(key, cell)
            }
            cell.add(fixture)
        }
        return true
    }

    override fun forEachBody(x: Int, y: Int, action: (IBody, IWorldContainer) -> Boolean): Boolean {
        val bodies = bodyMap.get(packKey(x, y))?.orderedItems() ?: return true
        for (i in 0 until bodies.size) if (!action(bodies[i], this)) return false
        return true
    }

    override fun forEachBody(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        action: (IBody, IWorldContainer) -> Boolean
    ): Boolean {
        tempBodySet.clear()
        for (column in minX..maxX) for (row in minY..maxY) {
            val bodies = bodyMap.get(packKey(column, row))?.orderedItems() ?: continue
            for (i in 0 until bodies.size) {
                val body = bodies[i]
                if (tempBodySet.contains(body)) continue
                if (!action(body, this)) return false
                tempBodySet.add(body)
            }
        }
        return true
    }

    override fun forEachFixture(x: Int, y: Int, action: (IFixture, IWorldContainer) -> Boolean): Boolean {
        val fixtures = fixtureMap.get(packKey(x, y))?.orderedItems() ?: return true
        for (i in 0 until fixtures.size) if (!action(fixtures[i], this)) return false
        return true
    }

    override fun forEachFixture(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        action: (IFixture, IWorldContainer) -> Boolean
    ): Boolean {
        tempFixtureSet.clear()
        for (column in minX..maxX) for (row in minY..maxY) {
            val fixtures = fixtureMap.get(packKey(column, row))?.orderedItems() ?: continue
            for (i in 0 until fixtures.size) {
                val fixture = fixtures[i]
                if (tempFixtureSet.contains(fixture)) continue
                if (!action(fixture, this)) return false
                tempFixtureSet.add(fixture)
            }
        }
        return true
    }

    override fun clear() {
        // empty the cells rather than dropping them, so their backing arrays survive to be refilled
        // next cycle instead of being reallocated
        bodyMap.forEach { it.value.clear() }
        fixtureMap.forEach { it.value.clear() }
    }

    override fun copy() =
        SimpleGridWorldContainer(ppm, bufferOffset, bodyMap, fixtureMap, adjustForExactGridMatch, floatRoundingError)

    override fun toString(): String {
        // built in a single pass rather than via `filter`/`map`: libGDX map iterators hand back the same
        // `Entry` instance on every step, so collecting entries into a list yields N copies of the last one
        val bodies = StringBuilder()
        bodyMap.forEach {
            if (it.value.size == 0) return@forEach
            if (bodies.isNotEmpty()) bodies.append(", ")
            bodies.append(cellToString(it.key)).append('=').append(it.value.size).append(" bodies")
        }

        val fixtures = StringBuilder()
        fixtureMap.forEach {
            if (it.value.size == 0) return@forEach
            if (fixtures.isNotEmpty()) fixtures.append(", ")
            fixtures.append(cellToString(it.key)).append('=').append(it.value.size).append(" fixtures")
        }

        return "SimpleGridWorldContainer{\n" +
            "\tppm=$ppm,\n" +
            "\tbodies=[$bodies],\n" +
            "\tfixtures=[$fixtures]\n" +
            "}"
    }

    private fun cellToString(key: Long) = "(${(key shr 32).toInt()},${key.toInt()})"
}
