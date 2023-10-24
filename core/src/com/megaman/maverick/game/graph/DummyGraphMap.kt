package com.megaman.maverick.game.graph

import com.badlogic.gdx.utils.OrderedSet
import com.engine.common.shapes.IGameShape2D
import com.engine.graph.IGraphMap

class DummyGraphMap(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
    override val ppm: Int
) : IGraphMap {

  private val set = OrderedSet<Any>()

  override fun add(obj: Any, shape: IGameShape2D): Boolean {
    set.add(obj)
    return true
  }

  override fun get(x: Int, y: Int) = set

  override fun get(minX: Int, minY: Int, maxX: Int, maxY: Int) = set

  override fun reset() = set.clear()
}
