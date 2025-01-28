package com.mega.game.engine.world.container

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.pairTo
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

    private val bodyMap = ObjectMap<IntPair, HashSet<IBody>>()
    private val fixtureMap = ObjectMap<IntPair, HashSet<IFixture>>()

    private val reusableGameRect = GameRectangle()

    private constructor(
        ppm: Int,
        bufferOffset: Int,
        bodyMap: ObjectMap<IntPair, HashSet<IBody>>,
        fixtureMap: ObjectMap<IntPair, HashSet<IFixture>>,
        adjustForExactGridMatch: Boolean,
        floatRoundingError: Float
    ) : this(ppm, bufferOffset, adjustForExactGridMatch, floatRoundingError) {
        this.bodyMap.putAll(bodyMap)
        this.fixtureMap.putAll(fixtureMap)
    }

    private fun adjustCoordinateIfNeeded(value: Float, isMinValue: Boolean) =
        if (adjustForExactGridMatch && MathUtils.isEqual(value % 1f, 0f, floatRoundingError)) {
            if (isMinValue) value + floatRoundingError
            else value - floatRoundingError
        } else value

    private fun getMinsAndMaxes(bounds: GameRectangle): MinsAndMaxes {
        val adjustedMinX = adjustCoordinateIfNeeded(bounds.getX(), true)
        val adjustedMinY = adjustCoordinateIfNeeded(bounds.getY(), true)
        val adjustedMaxX = adjustCoordinateIfNeeded(bounds.getMaxX(), false)
        val adjustedMaxY = adjustCoordinateIfNeeded(bounds.getMaxY(), false)

        val minX = MathUtils.floor(adjustedMinX / ppm.toFloat()) - bufferOffset
        val minY = MathUtils.floor(adjustedMinY / ppm.toFloat()) - bufferOffset
        val maxX = MathUtils.floor(adjustedMaxX / ppm.toFloat()) + bufferOffset
        val maxY = MathUtils.floor(adjustedMaxY / ppm.toFloat()) + bufferOffset

        return MinsAndMaxes(minX, minY, maxX, maxY)
    }

    override fun addBody(body: IBody): Boolean {
        val bounds = body.getBounds(reusableGameRect)
        val (minX, minY, maxX, maxY) = getMinsAndMaxes(bounds)
        for (column in minX..maxX) for (row in minY..maxY) {
            val set = bodyMap[column pairTo row] ?: HashSet()
            set.add(body)
            bodyMap.put(column pairTo row, set)
        }
        return true
    }

    override fun addFixture(fixture: IFixture): Boolean {
        val bounds = fixture.getShape().getBoundingRectangle(reusableGameRect)
        val (minX, minY, maxX, maxY) = getMinsAndMaxes(bounds)
        for (column in minX..maxX) for (row in minY..maxY) {
            val set = fixtureMap[column pairTo row] ?: HashSet()
            set.add(fixture)
            fixtureMap.put(column pairTo row, set)
        }
        return true
    }

    override fun getBodies(x: Int, y: Int): HashSet<IBody> {
        val set = bodyMap[x pairTo y]
        return set ?: HashSet()
    }

    override fun getBodies(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<IBody> {
        val set = HashSet<IBody>()
        for (column in minX..maxX) for (row in minY..maxY) bodyMap[column pairTo row]?.let { set.addAll(it) }
        return set
    }

    override fun getFixtures(x: Int, y: Int): HashSet<IFixture> {
        val set = fixtureMap[x pairTo y]
        return set ?: HashSet()
    }

    override fun getFixtures(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<IFixture> {
        val set = HashSet<IFixture>()
        for (column in minX..maxX) for (row in minY..maxY) fixtureMap[column pairTo row]?.let { set.addAll(it) }
        return set
    }

    override fun getObjects(x: Int, y: Int): HashSet<Any> {
        val set = HashSet<Any>()
        bodyMap[x pairTo y]?.let { set.addAll(it) }
        fixtureMap[x pairTo y]?.let { set.addAll(it) }
        return set
    }

    override fun getObjects(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<Any> {
        val set = HashSet<Any>()
        for (column in minX..maxX) for (row in minY..maxY) {
            bodyMap[column pairTo row]?.let { set.addAll(it) }
            fixtureMap[column pairTo row]?.let { set.addAll(it) }
        }
        return set
    }

    override fun clear() {
        bodyMap.clear()
        fixtureMap.clear()
    }

    override fun copy() =
        SimpleGridWorldContainer(ppm, bufferOffset, bodyMap, fixtureMap, adjustForExactGridMatch, floatRoundingError)

    override fun toString(): String {
        val nonEmptyBodies = bodyMap.filter { it.value.isNotEmpty() }.map { "${it.key}=${it.value.size} bodies" }

        val nonEmptyFixtures = fixtureMap.filter { it.value.isNotEmpty() }.map { "${it.key}=${it.value.size} fixtures" }

        // Construct the final string with filtered entries
        return "SimpleGridWorldContainer[" + "ppm=$ppm, " +
                "bodies={${nonEmptyBodies.joinToString(separator = ", ")}}, " +
                "fixtures={${
                    nonEmptyFixtures.joinToString(
                        separator = ", "
                    )
                }}]"
    }
}
