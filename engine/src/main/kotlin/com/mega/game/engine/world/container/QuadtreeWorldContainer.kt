package com.mega.game.engine.world.container

/*
class QuadtreeWorldContainer : IWorldContainer {

    private val ppm: Int
    private val maxObjectsPerNode: Int
    private val maxDepth: Int
    private var root: QuadtreeNode
    
    constructor(
        ppm: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        maxObjectsPerNode: Int,
        maxDepth: Int
    ) {
        this.ppm = ppm
        this.maxObjectsPerNode = maxObjectsPerNode
        this.maxDepth = maxDepth
        root = QuadtreeNode(0, x, y, width, height)
    }

    private constructor(
        ppm: Int,
        root: QuadtreeNode,
        maxObjectsPerNode: Int,
        maxDepth: Int
    ) {
        this.ppm = ppm
        this.root = root
        this.maxObjectsPerNode = maxObjectsPerNode
        this.maxDepth = maxDepth
    }

    override fun addBody(body: Body): Boolean {
        val bounds = body.getBounds()
        val scaledBounds = scaleToGrid(bounds)
        return root.insert(body, scaledBounds)
    }

    override fun addFixture(fixture: IFixture): Boolean {
        val bounds = fixture.getShape().getBoundingRectangle()
        val scaledBounds = scaleToGrid(bounds)
        return root.insert(fixture, scaledBounds)
    }

    private fun scaleToGrid(bounds: GameRectangle): GameRectangle {
        val minX = MathUtils.floor(bounds.x / ppm).toFloat()
        val minY = MathUtils.floor(bounds.y / ppm).toFloat()
        val maxX = MathUtils.floor(bounds.getMaxX() / ppm).toFloat()
        val maxY = MathUtils.floor(bounds.getMaxY() / ppm).toFloat()
        return GameRectangle(minX, minY, maxX - minX, maxY - minY)
    }

    override fun getBodies(x: Int, y: Int): HashSet<Body> {
        val result = HashSet<Body>()
        val point = GameRectangle(x.toFloat(), y.toFloat(), 0.1f, 0.1f)
        root.retrieveBodies(result, point)
        return result
    }

    override fun getBodies(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<Body> {
        val result = HashSet<Body>()
        root.retrieveBodies(
            result,
            GameRectangle(
                minX.toFloat(),
                minY.toFloat(),
                (maxX - minX).toFloat(),
                (maxY - minY).toFloat()
            )
        )
        return result
    }

    override fun getFixtures(x: Int, y: Int): HashSet<IFixture> {
        val result = HashSet<IFixture>()
        val point = GameRectangle(x.toFloat(), y.toFloat(), 0.1f, 0.1f)
        root.retrieveFixtures(result, point)
        return result
    }

    override fun getFixtures(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<IFixture> {
        val result = HashSet<IFixture>()
        root.retrieveFixtures(
            result,
            GameRectangle(
                minX.toFloat(),
                minY.toFloat(),
                (maxX - minX).toFloat(),
                (maxY - minY).toFloat()
            )
        )
        return result
    }

    override fun getObjects(x: Int, y: Int): HashSet<Any> {
        val result = HashSet<Any>()
        val point = GameRectangle(x.toFloat(), y.toFloat(), 0.1f, 0.1f)
        root.retrieveAllObjects(result, point)
        return result
    }

    override fun getObjects(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<Any> {
        val result = HashSet<Any>()
        root.retrieveAllObjects(
            result,
            GameRectangle(
                minX.toFloat(),
                minY.toFloat(),
                (maxX - minX).toFloat(),
                (maxY - minY).toFloat()
            )
        )
        return result
    }

    override fun clear() = root.clear()

    override fun copy() = QuadtreeWorldContainer(ppm, root, maxObjectsPerNode, maxDepth)

    private inner class QuadtreeNode(
        private val level: Int, private val x: Int, private val y: Int, private val width: Int, private val height: Int
    ) {

        private val bodies = HashSet<Body>()
        private val fixtures = HashSet<IFixture>()
        private var subNodes: Array<QuadtreeNode>? = null
        private val bounds = GameRectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

        fun insert(obj: Any, shape: IGameShape2D): Boolean {
            subNodes?.let {
                val index = getIndex(shape)
                if (index != -1) return it[index].insert(obj, shape)
            }

            when (obj) {
                is Body -> bodies.add(obj)
                is IFixture -> fixtures.add(obj)
                else -> throw IllegalArgumentException("Only bodies and fixtures are accepted")
            }

            if ((bodies.size + fixtures.size) > maxObjectsPerNode && level < maxDepth) {
                if (subNodes == null) subdivide()
                bodies.forEach { body -> insertIntoSubnodes(body, body.getBounds()) }
                fixtures.forEach { fixture -> insertIntoSubnodes(fixture, fixture.getShape()) }
                bodies.clear()
                fixtures.clear()
            }

            return true
        }

        private fun subdivide() {
            val subWidth = width / 2
            val subHeight = height / 2
            val nextLevel = level + 1
            val subNodes = Array<QuadtreeNode>(4)
            subNodes[0] = QuadtreeNode(nextLevel, x, y, subWidth, subHeight)
            subNodes[1] = QuadtreeNode(nextLevel, x + subWidth, y, subWidth, subHeight)
            subNodes[2] = QuadtreeNode(nextLevel, x, y + subHeight, subWidth, subHeight)
            subNodes[3] = QuadtreeNode(nextLevel, x + subWidth, y + subHeight, subWidth, subHeight)
            this.subNodes = subNodes
        }

        fun retrieveBodies(result: HashSet<Body>, area: GameRectangle) {
            if (!bounds.overlaps(area as Rectangle)) return

            bodies.forEach { result.add(it) }
            subNodes?.forEach { it.retrieveBodies(result, area) }
        }

        fun retrieveFixtures(result: HashSet<IFixture>, area: GameRectangle) {
            if (!bounds.overlaps(area as Rectangle)) return

            fixtures.forEach { result.add(it) }
            subNodes?.forEach { it.retrieveFixtures(result, area) }
        }

        fun retrieveAllObjects(result: HashSet<Any>, area: GameRectangle) {
            if (!bounds.overlaps(area as Rectangle)) return

            bodies.forEach { result.add(it) }
            fixtures.forEach { result.add(it) }
            subNodes?.forEach { it.retrieveAllObjects(result, area) }
        }

        fun clear() {
            bodies.clear()
            fixtures.clear()
            subNodes?.forEach { it.clear() }
            subNodes = null
        }

        private fun getIndex(shape: IGameShape2D): Int {
            val midX = x + width / 2
            val midY = y + height / 2

            val top = shape.getY() >= midY
            val bottom = shape.getMaxY() <= midY
            val left = shape.getMaxX() <= midX
            val right = shape.getX() >= midX

            return when {
                top && right -> 0
                top && left -> 1
                bottom && right -> 2
                bottom && left -> 3
                else -> -1
            }
        }

        private fun insertIntoSubnodes(obj: Any, shape: IGameShape2D): Boolean {
            val index = getIndex(shape)
            return if (index != -1) subNodes!![index].insert(obj, shape)
            else false
        }
    }
}
*/