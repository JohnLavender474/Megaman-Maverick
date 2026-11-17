package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.utils.OwnedArray
import java.util.*

class LaserContacts(origin: () -> Vector2) {

    private val pairs = OwnedArray { GamePair<Vector2, IFixture?>(Vector2(), null) }
    private val queue = PriorityQueue<GamePair<Vector2, IFixture?>> { p1, p2 ->
        val start = origin()
        p1.first.dst2(start).compareTo(p2.first.dst2(start))
    }

    val isEmpty: Boolean
        get() = queue.isEmpty()

    fun add(point: Vector2, fixture: IFixture?) {
        val pair = pairs.add()
        pair.first.set(point)
        pair.second = fixture
        queue.add(pair)
    }

    fun peek(): GamePair<Vector2, IFixture?> = queue.peek()

    fun poll(): GamePair<Vector2, IFixture?> = queue.poll()

    fun clear() {
        queue.clear()
        pairs.clear()
    }
}
