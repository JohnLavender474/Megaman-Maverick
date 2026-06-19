package com.mega.game.engine.world.container

/*
class QuadtreeWorldContainer : IWorldContainer {

    private val ppm: Int
    private val maxDepth: Int
    private var root: QuadtreeNode
    private val maxObjectsPerNode: Int

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
        this.maxDepth = maxDepth
        this.maxObjectsPerNode = maxObjectsPerNode
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
        this.maxDepth = maxDepth
        this.maxObjectsPerNode = maxObjectsPerNode
    }

    override fun addBody(body: IBody): Boolean {
        val bounds = body.getBounds(GameRectangle())
        val rounded = round(bounds)
        return root.insert(body, rounded)
    }

    override fun addFixture(fixture: IFixture): Boolean {
        val bounds = fixture.getShape().getBoundingRectangle(GameRectangle())
        val rounded = round(bounds)
        return root.insert(fixture, rounded)
    }

    private fun round(bounds: GameRectangle): GameRectangle {
        val minX = MathUtils.floor(bounds.getX() / ppm).toFloat()
        val minY = MathUtils.floor(bounds.getY() / ppm).toFloat()
        val maxX = MathUtils.floor(bounds.getMaxX() / ppm).toFloat()
        val maxY = MathUtils.floor(bounds.getMaxY() / ppm).toFloat()
        return bounds.set(minX, minY, maxX - minX, maxY - minY)
    }

    override fun getBodies(x: Int, y: Int): HashSet<IBody> {
        val result = HashSet<IBody>()
        val point = GameRectangle(x.toFloat(), y.toFloat(), 0.1f, 0.1f)
        root.retrieveBodies(result, point)
        return result
    }

    override fun getBodies(minX: Int, minY: Int, maxX: Int, maxY: Int): HashSet<IBody> {
        val result = HashSet<IBody>()
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
        private val level: Int,
        private val x: Int,
        private val y: Int,
        private val width: Int,
        private val height: Int
    ) {

        private val bodies = HashSet<IBody>()
        private val fixtures = HashSet<IFixture>()
        private var subNodes: Array<QuadtreeNode>? = null
        private val bounds = GameRectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

        fun insert(obj: Any, shape: IGameShape2D): Boolean {
            subNodes?.let {
                val index = getIndex(shape)
                if (index != -1) return it[index].insert(obj, shape)
            }

            when (obj) {
                is IBody -> bodies.add(obj)
                is IFixture -> fixtures.add(obj)
                else -> throw IllegalArgumentException("Only bodies and fixtures are accepted")
            }

            if (bodies.size + fixtures.size > maxObjectsPerNode && level < maxDepth) {
                if (subNodes == null) subdivide()
                bodies.forEach { body -> insertIntoSubnodes(body, body.getBounds(GameRectangle())) }
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
            val subNodes = Array<QuadtreeNode>()
            subNodes[0] = QuadtreeNode(nextLevel, x, y, subWidth, subHeight)
            subNodes[1] = QuadtreeNode(nextLevel, x + subWidth, y, subWidth, subHeight)
            subNodes[2] = QuadtreeNode(nextLevel, x, y + subHeight, subWidth, subHeight)
            subNodes[3] = QuadtreeNode(nextLevel, x + subWidth, y + subHeight, subWidth, subHeight)
            this.subNodes = subNodes
        }

        fun retrieveBodies(result: HashSet<IBody>, area: GameRectangle) {
            if (!bounds.overlaps(area)) return

            bodies.forEach { result.add(it) }
            subNodes?.forEach { it.retrieveBodies(result, area) }
        }

        fun retrieveFixtures(result: HashSet<IFixture>, area: GameRectangle) {
            if (!bounds.overlaps(area)) return

            fixtures.forEach { result.add(it) }
            subNodes?.forEach { it.retrieveFixtures(result, area) }
        }

        fun retrieveAllObjects(result: HashSet<Any>, area: GameRectangle) {
            if (!bounds.overlaps(area)) return

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
            return if (index != -1) subNodes!![index].insert(obj, shape) else false
        }
    }
}
*/
