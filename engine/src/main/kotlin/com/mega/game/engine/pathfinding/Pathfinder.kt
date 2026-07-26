package com.mega.game.engine.pathfinding

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.LongMap
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import java.util.*

class Pathfinder(
    private val startCoordinate: IntPair,
    private val targetCoordinate: IntPair,
    private val filter: (Int, Int) -> Boolean,
    private val allowDiagonal: Boolean,
    private val heuristic: IHeuristic,
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val maxDistance: Int = DEFAULT_MAX_DISTANCE,
    private val returnBestPathOnFailure: Boolean = DEFAULT_RETURN_BEST_PATH_ON_FAILURE
) : IPathfinder {

    companion object {
        const val TAG = "Pathfinder"

        const val DEFAULT_MAX_ITERATIONS = 1000
        const val DEFAULT_MAX_DISTANCE = Integer.MAX_VALUE
        const val DEFAULT_RETURN_BEST_PATH_ON_FAILURE = false

        private val NEIGHBOR_DX = intArrayOf(-1, 1, 0, 0, -1, 1, -1, 1)
        private val NEIGHBOR_DY = intArrayOf(0, 0, -1, 1, -1, 1, 1, -1)
        private const val ORTHOGONAL_NEIGHBORS = 4
    }

    internal class Node(val x: Int, val y: Int) : Comparable<Node> {

        var distance = 0
        var previous: Node? = null
        var discovered = false

        override fun compareTo(other: Node) = distance.compareTo(other.distance)

        override fun toString() =
            "Node{x=$x,y=$y,distance=$distance,discovered=$discovered,previous={${previous?.let { "${it.x},${it.y}" }}}"
    }

    private val out1 = Vector2()
    private val out2 = Vector2()

    private fun key(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

    override fun call(): PathfinderResult {
        val map = LongMap<Node>()

        if (startCoordinate == targetCoordinate ||
            (!returnBestPathOnFailure && startCoordinate.toVector2(out1)
                .dst(targetCoordinate.toVector2(out2)) > maxDistance)
        ) return PathfinderResult(null, true)

        val startNode = Node(startCoordinate.x, startCoordinate.y)
        map.put(key(startNode.x, startNode.y), startNode)

        val open = PriorityQueue<Node>()
        open.add(startNode)

        var iterations = 0

        var bestNode: Node? = null
        var bestNodeCost = 0

        val neighborCount = if (allowDiagonal) NEIGHBOR_DX.size else ORTHOGONAL_NEIGHBORS

        while (open.isNotEmpty()) {
            val currentNode = open.poll()
            if (currentNode.discovered) continue

            if (iterations >= maxIterations) break
            iterations++

            currentNode.discovered = true
            if (currentNode.distance > maxDistance) break

            val currentCost = heuristic.calculate(currentNode.x, currentNode.y, targetCoordinate.x, targetCoordinate.y)
            if (bestNode == null || currentCost < bestNodeCost) {
                bestNode = currentNode
                bestNodeCost = currentCost
            }

            if (currentNode.x == targetCoordinate.x && currentNode.y == targetCoordinate.y) {
                val path = buildPath(currentNode)
                return PathfinderResult(path, false)
            }

            for (i in 0 until neighborCount) {
                val neighborX = currentNode.x + NEIGHBOR_DX[i]
                val neighborY = currentNode.y + NEIGHBOR_DY[i]

                if (!filter.invoke(neighborX, neighborY)) continue

                val neighborKey = key(neighborX, neighborY)
                var neighbor = map.get(neighborKey)
                if (neighbor?.discovered == true) continue

                val totalDistance = currentNode.distance + heuristic.calculate(
                    currentNode.x,
                    currentNode.y,
                    neighborX,
                    neighborY
                )

                if (neighbor == null) {
                    neighbor = Node(neighborX, neighborY)
                    map.put(neighborKey, neighbor)

                    neighbor.distance = totalDistance
                    neighbor.previous = currentNode

                    open.add(neighbor)
                } else if (totalDistance < neighbor.distance) {
                    neighbor.distance = totalDistance
                    neighbor.previous = currentNode

                    open.add(neighbor)
                }
            }
        }

        return if (returnBestPathOnFailure && bestNode != null) {
            val bestPath = buildPath(bestNode)
            PathfinderResult(bestPath, false)
        } else PathfinderResult(null, false)
    }

    private fun buildPath(node: Node): Array<IntPair> {
        val path = Array<IntPair>()
        var currentNode: Node? = node
        while (currentNode != null) {
            path.add(IntPair(currentNode.x, currentNode.y))
            currentNode = currentNode.previous
        }
        path.reverse()
        return path
    }
}
