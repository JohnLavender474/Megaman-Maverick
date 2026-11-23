package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectIntMap
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.CeilingProfile
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class CeilingPolygonBlock(services: MegaServices) : Block(services) {

    companion object {
        const val TAG = "CeilingPolygonBlock"

        private const val ANGLE_TOLERANCE = 0.01f
        private const val FROM_BELOW_TOLERANCE = 0.01f
    }

    private val polygon = GamePolygon()
    private val worldVertices = Array<Float>()
    private var localVertices = FloatArray(0)
    private var lowerChain = FloatArray(0)
    private var chainCount = 0

    private val headContacts = ObjectIntMap<IFixture>()
    private val headFixtures = Array<IFixture>()
    private val addHeadFixture: (IFixture) -> Unit = { if (it.getType() == FixtureType.HEAD) headFixtures.add(it) }
    private val bounds = GameRectangle()
    private val span = Vector2()
    private val stepStart = Vector2()

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        blockFixture.setShape(polygon)
        component.body.preProcess.put("${ConstKeys.BLOCK}_${ConstKeys.FIXTURE}") {
            blockFixture.drawingColor = if (blockFixture.isActive()) Color.RED else Color.GRAY
        }
        component.body.postProcess.put(ConstKeys.HEAD) { checkHeadContacts() }
        return component
    }

    override fun onSpawn(spawnProps: Properties) {
        val gravity = services.gravity.direction
        if (gravity != Direction.UP) throw CeilingPolygonGravityException(spawnProps, gravity)

        super.onSpawn(spawnProps)

        val source = spawnProps.get(ConstKeys.POLYGON) as? GamePolygon
            ?: throw InvalidCeilingPolygonException(spawnProps, "the map object is not a polygon")
        val rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        if (rotation != 0f || source.rotation != 0f)
            throw InvalidCeilingPolygonException(spawnProps, "the polygon is rotated ($rotation degrees)")

        worldVertices.clear()
        source.getTransformedVertices(worldVertices)
        val count = worldVertices.size / 2

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        for (i in 0 until count) {
            minX = minOf(minX, worldVertices[2 * i])
            minY = minOf(minY, worldVertices[2 * i + 1])
        }
        growVertexArrays(worldVertices.size)
        for (i in 0 until count) {
            localVertices[2 * i] = worldVertices[2 * i] - minX
            localVertices[2 * i + 1] = worldVertices[2 * i + 1] - minY
        }

        checkEdges(spawnProps, count)
        checkConvex(spawnProps, count)
        buildLowerChain(count)

        polygon.setLocalVertices(localVertices)
        polygon.setPosition(0f, 0f)
        body.set(polygon.getBoundingRectangle(bounds).setPosition(minX, minY))
        body.addBodyLabel(BodyLabel.CEILING)
        blockFixture.addFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)
        headContacts.clear()
    }

    override fun onDestroy() {
        val entries = headContacts.iterator()
        while (entries.hasNext()) {
            val head = entries.next().key
            if (head.getEntity().existenceState.liveNow) endHeadContact(head)
        }
        headContacts.clear()
        super.onDestroy()
    }

    fun collide(other: IBody) {
        val ceiling = ceilingOver(other)
        if (ceiling == Float.POSITIVE_INFINITY || other.getY() >= ceiling) return

        if (other.getMaxY() > ceiling) {
            if (other.isSensing(BodySense.FEET_ON_GROUND) || !cameFromBelow(other)) pushSideways(other)
            else {
                other.setMaxY(ceiling)
                if (other.physics.velocity.y > 0f) other.physics.velocity.y = 0f
            }
        }

        headFixtures.clear()
        other.forEachFixture(addHeadFixture)
        for (i in 0 until headFixtures.size) {
            val head = headFixtures[i]
            if (headTouches(head, other)) touchHead(head, other)
        }
    }

    private fun ceilingOver(other: IBody) = ceilingOver(other.getX(), other.getMaxX())

    private fun ceilingOver(x0: Float, x1: Float) =
        CeilingProfile.lowestCeiling(lowerChain, chainCount, x0 - body.getX(), x1 - body.getX()) + body.getY()

    private fun cameFromBelow(other: IBody): Boolean {
        other.getStepStart(stepStart)
        val ceiling = ceilingOver(stepStart.x, stepStart.x + other.getWidth())
        return ceiling != Float.POSITIVE_INFINITY && stepStart.y + other.getHeight() <= ceiling + FROM_BELOW_TOLERANCE
    }

    private fun pushSideways(other: IBody) {
        if (!CeilingProfile.spanBelow(lowerChain, chainCount, other.getMaxY() - body.getY(), span)) return
        val left = span.x + body.getX()
        val right = span.y + body.getX()
        val centerX = other.getX() + other.getWidth() / 2f
        if (centerX < (left + right) / 2f) other.setMaxX(left) else other.setX(right)
    }

    private fun headTouches(head: IFixture, other: IBody): Boolean {
        if (!head.isActive()) return false
        val ceiling = ceilingOver(other)
        if (ceiling == Float.POSITIVE_INFINITY || other.getY() >= ceiling) return false
        return head.getShape().getBoundingRectangle(bounds).getMaxY() >= ceiling && cameFromBelow(other)
    }

    private fun touchHead(head: IFixture, other: IBody) {
        val known = headContacts.containsKey(head)
        if (known && headContacts.get(head, Int.MIN_VALUE) == other.stepSerial) return
        headContacts.put(head, other.stepSerial)

        other.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
        if (!known) other.physics.velocity.y = 0f

        val state = if (known) ProcessState.CONTINUE else ProcessState.BEGIN
        hitByHead(state, head)
        head.receiver(Receivers.HIT_BY_BLOCK, state)?.invoke(this, services.settings.performance.fixedStep)
    }

    // TODO: with update_distance on the map object the block body can stop updating, and a head sense it set stays on until the block is destroyed; no map sets it (182a docs/TODO.md)
    private fun checkHeadContacts() {
        val entries = headContacts.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            val head = entry.key
            if (entry.value == body.stepSerial) continue

            if (!head.getEntity().existenceState.liveNow) {
                entries.remove()
                continue
            }

            val other = head.getBody()
            if (directionOf(other) == Direction.UP && headTouches(head, other)) {
                touchHead(head, other)
                continue
            }

            entries.remove()
            endHeadContact(head)
        }
    }

    private fun endHeadContact(head: IFixture) {
        head.getBody().setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
        hitByHead(ProcessState.END, head)
        head.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(this, services.settings.performance.fixedStep)
    }

    private fun directionOf(other: IBody): Direction {
        val entity = other.getEntity()
        return if (entity is IDirectional) entity.direction else Direction.UP
    }

    private fun checkEdges(spawnProps: Properties, count: Int) {
        for (i in 0 until count) {
            val j = (i + 1) % count
            val dx = abs(localVertices[2 * j] - localVertices[2 * i])
            val dy = abs(localVertices[2 * j + 1] - localVertices[2 * i + 1])
            val flat = dy <= ANGLE_TOLERANCE && dx > ANGLE_TOLERANCE
            val diagonal = abs(dx - dy) <= ANGLE_TOLERANCE && dx > ANGLE_TOLERANCE
            if (!flat && !diagonal)
                throw InvalidCeilingPolygonException(spawnProps, "edge $i is not flat or at 45 degrees (dx=$dx, dy=$dy)")
        }
    }

    private fun checkConvex(spawnProps: Properties, count: Int) {
        var sign = 0
        for (i in 0 until count) {
            val j = (i + 1) % count
            val k = (i + 2) % count
            val cross = (localVertices[2 * j] - localVertices[2 * i]) * (localVertices[2 * k + 1] - localVertices[2 * j + 1]) -
                (localVertices[2 * j + 1] - localVertices[2 * i + 1]) * (localVertices[2 * k] - localVertices[2 * j])
            if (abs(cross) <= ANGLE_TOLERANCE) continue
            val crossSign = if (cross > 0f) 1 else -1
            if (sign == 0) sign = crossSign
            else if (sign != crossSign) throw InvalidCeilingPolygonException(spawnProps, "the polygon is not convex")
        }
        if (sign == 0) throw InvalidCeilingPolygonException(spawnProps, "the polygon has no area")
    }

    private fun buildLowerChain(count: Int) {
        var left = 0
        var right = 0
        for (i in 1 until count) {
            if (localVertices[2 * i] < localVertices[2 * left]) left = i
            if (localVertices[2 * i] > localVertices[2 * right]) right = i
        }

        val next = (left + 1) % count
        val prev = (left - 1 + count) % count
        val step = if (localVertices[2 * next + 1] <= localVertices[2 * prev + 1]) 1 else -1

        chainCount = 0
        var i = left
        while (true) {
            lowerChain[2 * chainCount] = localVertices[2 * i]
            lowerChain[2 * chainCount + 1] = localVertices[2 * i + 1]
            chainCount++
            if (i == right) break
            i = (i + step + count) % count
        }
    }

    private fun growVertexArrays(size: Int) {
        if (localVertices.size != size) localVertices = FloatArray(size)
        if (lowerChain.size < size) lowerChain = FloatArray(size)
    }
}
