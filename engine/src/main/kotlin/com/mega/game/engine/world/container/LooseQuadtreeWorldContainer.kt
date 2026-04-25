package com.mega.game.engine.world.container

import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture

/**
 * A loose quadtree implementation of [IWorldContainer].
 *
 * Unlike a standard quadtree, each node's effective bounds are expanded by [loosenessFactor]
 * around the node's center. An entity is placed in the deepest node whose loose bounds fully
 * contain the entity's bounding box, using the entity's center to pick which child quadrant to
 * try first. This eliminates the boundary-straddling problem of tight quadtrees: entities near a
 * subdivision line are fully contained within one child's loose bounds rather than being forced
 * up to the parent.
 *
 * Queries traverse all nodes whose loose bounds overlap the query region, so results may include
 * entities from neighboring areas. Callers are expected to perform exact overlap tests on the
 * returned candidates (as [com.mega.game.engine.world.WorldSystem] already does).
 *
 * @param ppm pixels per meter; used to convert grid-cell coordinates (used by [IWorldContainer]'s
 *   query API) to world-space coordinates used internally by the tree.
 * @param worldMinX left edge of the root node's tight bounds in world space.
 * @param worldMinY bottom edge of the root node's tight bounds in world space.
 * @param worldMaxX right edge of the root node's tight bounds in world space.
 * @param worldMaxY top edge of the root node's tight bounds in world space.
 * @param maxDepth maximum subdivision depth. Entities that cannot fit in any child at this depth
 *   are stored at the leaf node directly.
 * @param loosenessFactor multiplier applied to each node's half-extents to derive its loose bounds.
 *   Must be >= 1. A value of 2 (default) doubles the effective region of each node.
 */
class LooseQuadtreeWorldContainer(
    var ppm: Int,
    var worldMinX: Float,
    var worldMinY: Float,
    var worldMaxX: Float,
    var worldMaxY: Float,
    var maxDepth: Int = 6,
    var loosenessFactor: Float = 2f
) : IWorldContainer {

    init {
        if (loosenessFactor < 1f) throw IllegalArgumentException("Looseness factor should not be less than 1")
    }

    private inner class Node(
        val tightMinX: Float,
        val tightMinY: Float,
        val tightMaxX: Float,
        val tightMaxY: Float,
        val depth: Int
    ) {
        private val centerX = (tightMinX + tightMaxX) / 2f
        private val centerY = (tightMinY + tightMaxY) / 2f
        private val halfW = (tightMaxX - tightMinX) / 2f
        private val halfH = (tightMaxY - tightMinY) / 2f

        val looseMinX = centerX - halfW * loosenessFactor
        val looseMinY = centerY - halfH * loosenessFactor
        val looseMaxX = centerX + halfW * loosenessFactor
        val looseMaxY = centerY + halfH * loosenessFactor

        val tightMidX = centerX
        val tightMidY = centerY

        val bodies = HashSet<IBody>()
        val fixtures = HashSet<IFixture>()
        var children: Array<Node?>? = null

        fun isLeaf() = children == null

        fun subdivide() {
            children = arrayOfNulls(4)
            children!![0] = Node(tightMinX, tightMidY, tightMidX, tightMaxY, depth + 1)  // NW
            children!![1] = Node(tightMidX, tightMidY, tightMaxX, tightMaxY, depth + 1)  // NE
            children!![2] = Node(tightMinX, tightMinY, tightMidX, tightMidY, depth + 1)  // SW
            children!![3] = Node(tightMidX, tightMinY, tightMaxX, tightMidY, depth + 1)  // SE
        }

        fun looseContains(minX: Float, minY: Float, maxX: Float, maxY: Float) =
            minX >= looseMinX && minY >= looseMinY && maxX <= looseMaxX && maxY <= looseMaxY

        fun looseOverlaps(minX: Float, minY: Float, maxX: Float, maxY: Float) =
            !(minX > looseMaxX || maxX < looseMinX || minY > looseMaxY || maxY < looseMinY)
    }

    private var root = createRoot()
    private val reusableRect = GameRectangle()

    private fun createRoot() = Node(worldMinX, worldMinY, worldMaxX, worldMaxY, 0)

    /**
     * Inserts an entity into the deepest node whose loose bounds fully contain it.
     * The child quadrant is chosen by the entity's center point; if that child's loose
     * bounds cannot contain the entity, it is stored at the current node instead.
     */
    private fun insertInto(
        node: Node,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        action: (Node) -> Unit
    ) {
        if (node.depth >= maxDepth) {
            action(node)
            return
        }

        if (node.isLeaf()) node.subdivide()

        val entityCenterX = (minX + maxX) / 2f
        val entityCenterY = (minY + maxY) / 2f

        val childIdx = when {
            entityCenterX < node.tightMidX && entityCenterY >= node.tightMidY -> 0  // NW
            entityCenterX >= node.tightMidX && entityCenterY >= node.tightMidY -> 1  // NE
            entityCenterX < node.tightMidX && entityCenterY < node.tightMidY -> 2   // SW
            else -> 3                                                                  // SE
        }

        val child = node.children!![childIdx]!!
        if (child.looseContains(minX, minY, maxX, maxY)) {
            insertInto(child, minX, minY, maxX, maxY, action)
        } else {
            action(node)
        }
    }

    private fun queryRegion(
        node: Node,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        action: (Node) -> Unit
    ) {
        if (!node.looseOverlaps(minX, minY, maxX, maxY)) return
        action(node)
        node.children?.forEach { child ->
            if (child != null) queryRegion(child, minX, minY, maxX, maxY, action)
        }
    }

    // Grid cell (x, y) covers world [x*ppm, y*ppm] to [(x+1)*ppm, (y+1)*ppm]
    private fun cellsToWorldMinX(gridX: Int) = gridX.toFloat() * ppm
    private fun cellsToWorldMinY(gridY: Int) = gridY.toFloat() * ppm
    private fun cellsToWorldMaxX(gridX: Int) = (gridX + 1).toFloat() * ppm
    private fun cellsToWorldMaxY(gridY: Int) = (gridY + 1).toFloat() * ppm

    override fun addBody(body: IBody): Boolean {
        val b = body.getBounds(reusableRect)
        insertInto(root, b.getX(), b.getY(), b.getMaxX(), b.getMaxY()) { it.bodies.add(body) }
        return true
    }

    override fun addFixture(fixture: IFixture): Boolean {
        val b = fixture.getShape().getBoundingRectangle(reusableRect)
        insertInto(root, b.getX(), b.getY(), b.getMaxX(), b.getMaxY()) { it.fixtures.add(fixture) }
        return true
    }

    override fun getBodies(x: Int, y: Int, out: MutableCollection<IBody>) =
        getBodies(x, y, x, y, out)

    override fun getBodies(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<IBody>) {
        queryRegion(
            root,
            cellsToWorldMinX(minX), cellsToWorldMinY(minY),
            cellsToWorldMaxX(maxX), cellsToWorldMaxY(maxY)
        ) { node -> out.addAll(node.bodies) }
    }

    override fun getFixtures(x: Int, y: Int, out: MutableCollection<IFixture>) =
        getFixtures(x, y, x, y, out)

    override fun getFixtures(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<IFixture>) {
        queryRegion(
            root,
            cellsToWorldMinX(minX), cellsToWorldMinY(minY),
            cellsToWorldMaxX(maxX), cellsToWorldMaxY(maxY)
        ) { node -> out.addAll(node.fixtures) }
    }

    override fun getObjects(x: Int, y: Int, out: MutableCollection<Any>) =
        getObjects(x, y, x, y, out)

    override fun getObjects(minX: Int, minY: Int, maxX: Int, maxY: Int, out: MutableCollection<Any>) {
        queryRegion(
            root,
            cellsToWorldMinX(minX), cellsToWorldMinY(minY),
            cellsToWorldMaxX(maxX), cellsToWorldMaxY(maxY)
        ) { node ->
            out.addAll(node.bodies)
            out.addAll(node.fixtures)
        }
    }

    override fun clear() {
        root = createRoot()
    }

    override fun copy(): IWorldContainer {
        val copy = LooseQuadtreeWorldContainer(
            ppm, worldMinX, worldMinY, worldMaxX, worldMaxY, maxDepth, loosenessFactor
        )
        copyNode(root, copy.root)
        return copy
    }

    private fun copyNode(src: Node, dst: Node) {
        dst.bodies.addAll(src.bodies)
        dst.fixtures.addAll(src.fixtures)
        val srcChildren = src.children ?: return
        dst.subdivide()
        for (i in 0 until 4) copyNode(srcChildren[i]!!, dst.children!![i]!!)
    }
}
