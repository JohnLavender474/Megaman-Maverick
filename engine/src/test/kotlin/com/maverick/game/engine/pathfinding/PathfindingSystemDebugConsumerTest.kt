package com.maverick.game.engine.pathfinding

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.IntPair
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class PathfindingSystemDebugConsumerTest : DescribeSpec({

    val result = PathfinderResult(gdxArrayOf(IntPair(0, 0), IntPair(1, 0)), true)
    val factory = object : IPathfinderFactory {
        override fun getPathfinder(params: PathfinderParams) = object : IPathfinder {
            override fun call() = result
        }
    }

    lateinit var consumed: ArrayList<PathfinderResult>
    lateinit var debugConsumed: ArrayList<PathfinderResult>

    fun runUntilConsumed(system: AbstractPathfindingSystem) {
        val entity = MockGameEntity()
        entity.addComponent(PathfindingComponent(mockk(relaxed = true), { consumed.add(it) }))

        val engine = GameEngine()
        engine.systems.add(system)
        system.on = true
        engine.spawn(entity)

        var updates = 0
        while (consumed.isEmpty() && updates < 200) {
            engine.update(1f)
            if (consumed.isEmpty()) Thread.sleep(5)
            updates++
        }
        engine.update(1f)

        system.dispose()
    }

    beforeEach {
        consumed = ArrayList()
        debugConsumed = ArrayList()
    }

    listOf<Pair<String, ((PathfinderResult) -> Unit)?.() -> AbstractPathfindingSystem>>(
        "SimplePathfindingSystem" to { SimplePathfindingSystem(factory, pathDebugConsumer = this) },
        "AsyncPathfindingSystem" to { AsyncPathfindingSystem(factory, pathDebugConsumer = this) }
    ).forEach { (name, create) ->
        describe(name) {
            it("calls the path debug consumer with the same result as the component consumer") {
                runUntilConsumed(create { debugConsumed.add(it) })

                consumed.isEmpty() shouldBe false
                debugConsumed shouldContainExactly consumed
                debugConsumed.all { it === result } shouldBe true
            }

            it("calls only the component consumer when the path debug consumer is null") {
                runUntilConsumed(create(null))

                consumed.isEmpty() shouldBe false
                debugConsumed.shouldBeEmpty()
            }
        }
    }
})
