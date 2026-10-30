package com.megaman.maverick.game.pathfinding

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.pathfinding.PathfinderParams
import com.maverick.game.engine.pathfinding.PathfinderResult
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.maverick.game.engine.world.container.IWorldContainer
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class MegaPathfinderFactoryTest : DescribeSpec({

    var frameId = 1L
    var oldGraphics: Graphics? = null
    val seen = ArrayList<IWorldContainer?>()

    fun mapResult(width: Int) =
        TiledMapLoadResult(mockk<TiledMap>(relaxed = true), ObjectMap<String, MapObjects>(), width, 1, 32, 32)

    fun PathfinderResult.end() = path?.peek()

    fun params() = PathfinderParams(
        startCoordinateSupplier = { IntPair(0, 0) },
        targetCoordinateSupplier = { IntPair(5, 0) },
        allowDiagonal = { false },
        filter = { _, _, container ->
            seen.add(container)
            true
        }
    ).apply { putProperty(ConstKeys.ALLOW_OUT_OF_BOUNDS, false) }

    beforeEach {
        oldGraphics = Gdx.graphics
        frameId = 1L
        val graphics = mockk<Graphics>(relaxed = true)
        every { graphics.frameId } answers { frameId }
        Gdx.graphics = graphics
        seen.clear()
    }

    afterEach {
        Gdx.graphics = oldGraphics
    }

    describe("MegaPathfinderFactory") {
        it("uses the level values that are set after the factory is built") {
            val levelState = LevelState()
            val factory = MegaPathfinderFactory(levelState)
            val container = SimpleGridWorldContainer(ConstVals.PPM)
            levelState.worldContainer = container
            levelState.tiledMapLoadResult = mapResult(10)

            val result = factory.getPathfinder(params()).call()

            result.end() shouldBe IntPair(5, 0)
            seen.first().shouldNotBeNull()
            seen.first() shouldNotBeSameInstanceAs container
        }

        it("uses the world size of the current map load result") {
            val levelState = LevelState()
            val factory = MegaPathfinderFactory(levelState)
            levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
            levelState.tiledMapLoadResult = mapResult(10)
            factory.getPathfinder(params()).call().end() shouldBe IntPair(5, 0)

            levelState.tiledMapLoadResult = mapResult(3)

            factory.getPathfinder(params()).call().end() shouldBe IntPair(2, 0)
        }

        it("keeps one snapshot in a frame, and takes a new one for a new container or a new frame") {
            val levelState = LevelState()
            val factory = MegaPathfinderFactory(levelState)
            levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
            levelState.tiledMapLoadResult = mapResult(10)

            factory.getPathfinder(params()).call()
            val first = seen.last()
            factory.getPathfinder(params()).call()
            seen.last() shouldBeSameInstanceAs first

            levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
            factory.getPathfinder(params()).call()
            val second = seen.last()
            second shouldNotBeSameInstanceAs first

            frameId = 2L
            factory.getPathfinder(params()).call()
            seen.last() shouldNotBeSameInstanceAs second
        }
    }
})
