package com.maverick.game.engine.pathfinding

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.IntPair
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PathfindingRespawnTest : DescribeSpec({

    val result = PathfinderResult(gdxArrayOf(IntPair(0, 0), IntPair(1, 0)))

    describe("PathfindingComponent") {
        it("starts a new spawn with no path") {
            val factory = object : IPathfinderFactory {
                override fun getPathfinder(params: PathfinderParams) = object : IPathfinder {
                    override fun call() = result
                }
            }
            var consumed = 0
            val entity = MockGameEntity()
            entity.addComponent(PathfindingComponent(mockk(relaxed = true), { consumed++ }))
            val system = SimplePathfindingSystem(factory)
            val engine = GameEngine()
            engine.systems.add(system)
            system.on = true

            engine.spawn(entity)
            engine.update(1f)
            engine.update(0f)
            consumed shouldBe 1

            engine.destroy(entity)
            engine.update(0f)
            engine.spawn(entity)
            engine.update(0f)
            engine.update(0f)

            consumed shouldBe 1
        }

        it("starts a new spawn with a full search interval") {
            var searches = 0
            val factory = object : IPathfinderFactory {
                override fun getPathfinder(params: PathfinderParams): IPathfinder {
                    searches++
                    return object : IPathfinder {
                        override fun call() = result
                    }
                }
            }
            val entity = MockGameEntity()
            entity.addComponent(PathfindingComponent(mockk(relaxed = true), {}))
            val system = SimplePathfindingSystem(factory)
            val engine = GameEngine()
            engine.systems.add(system)
            system.on = true

            engine.spawn(entity)
            engine.update(1f)
            searches shouldBe 1
            engine.update(0.6f * PathfindingComponent.DEFAULT_UPDATE_INTERVAL)

            engine.destroy(entity)
            engine.update(0f)
            engine.spawn(entity)
            engine.update(0f)
            engine.update(0.6f * PathfindingComponent.DEFAULT_UPDATE_INTERVAL)

            searches shouldBe 1
        }
    }

    describe("AsyncPathfindingSystem") {
        it("drops the search of an entity that leaves, so its next spawn gets no old path") {
            val release = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val factory = object : IPathfinderFactory {
                override fun getPathfinder(params: PathfinderParams) = object : IPathfinder {
                    override fun call(): PathfinderResult {
                        try {
                            release.await(5, TimeUnit.SECONDS)
                        } finally {
                            finished.countDown()
                        }
                        return result
                    }
                }
            }
            var consumed = 0
            val entity = MockGameEntity()
            entity.addComponent(PathfindingComponent(mockk(relaxed = true), { consumed++ }))
            val system = AsyncPathfindingSystem(factory)
            val engine = GameEngine()
            engine.systems.add(system)
            system.on = true

            engine.spawn(entity)
            engine.update(1f)
            engine.destroy(entity)
            engine.update(0f)
            release.countDown()
            finished.await(5, TimeUnit.SECONDS) shouldBe true
            Thread.sleep(20)
            repeat(3) { engine.update(0f) }
            engine.spawn(entity)
            repeat(3) { engine.update(0f) }
            system.dispose()

            consumed shouldBe 0
        }
    }
})
