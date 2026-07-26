package com.mega.game.engine.world.container

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.MinsAndMaxes
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture

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

    private val bodyCellPool = Array<OrderedSet<IBody>>()
    private val fixtureCellPool = Array<OrderedSet<IFixture>>()

    private constructor(
        ppm: Int,
        bufferOffset: Int,
        bodyMap: LongMap<OrderedSet<IBody>>,
        fixtureMap: LongMap<OrderedSet<IFixture>>,
        adjustForExactGridMatch: Boolean,
        floatRoundingError: Float
    ) : this(ppm, bufferOffset, adjustForExactGridMatch, floatRoundingError) {
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
                cell = if (bodyCellPool.isEmpty) OrderedSet() else bodyCellPool.pop()
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
                cell = if (fixtureCellPool.isEmpty) OrderedSet() else fixtureCellPool.pop()
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
        bodyMap.forEach {
            it.value.clear()
            bodyCellPool.add(it.value)
        }
        bodyMap.clear()

        fixtureMap.forEach {
            it.value.clear()
            fixtureCellPool.add(it.value)
        }
        fixtureMap.clear()
    }

    override fun copy() =
        SimpleGridWorldContainer(ppm, bufferOffset, bodyMap, fixtureMap, adjustForExactGridMatch, floatRoundingError)

    // exposed for tests: the guarantee that occupied-cell count tracks live contents rather than
    // everything ever inserted is the whole point of pooling, and it is invisible from the outside

    internal fun getOccupiedBodyCellCount() = bodyMap.size

    internal fun getOccupiedFixtureCellCount() = fixtureMap.size

    internal fun getPooledBodyCellCount() = bodyCellPool.size

    internal fun getPooledFixtureCellCount() = fixtureCellPool.size

    override fun toString(): String {
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
