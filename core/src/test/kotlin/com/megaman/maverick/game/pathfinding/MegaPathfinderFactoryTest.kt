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
import com.maverick.game.engine.world.IWorldBodies
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.pathfinding.IBodyCells
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class MegaPathfinderFactoryTest : DescribeSpec({

    var frameId = 1L
    var oldGraphics: Graphics? = null
    val seen = ArrayList<IBodyCells?>()
    val worldBodies = ArrayList<IBody>()
    val out = ArrayList<IBody>()

    val source = object : IWorldBodies {
        override val bodyCount get() = worldBodies.size
        override fun getBody(index: Int) = worldBodies[index]
    }

    fun mapResult(width: Int) =
        TiledMapLoadResult(mockk<TiledMap>(relaxed = true), ObjectMap<String, MapObjects>(), width, 1, 32, 32)

    fun PathfinderResult.end() = path?.peek()

    fun params() = PathfinderParams(
        startCoordinateSupplier = { IntPair(0, 0) },
        targetCoordinateSupplier = { IntPair(5, 0) },
        allowDiagonal = { false },
        filter = { _, _, cells ->
            seen.add(cells)
            true
        }
    ).apply { putProperty(ConstKeys.ALLOW_OUT_OF_BOUNDS, false) }

    fun level(width: Int = 10) = LevelState().apply {
        worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        tiledMapLoadResult = mapResult(width)
    }

    fun block(x: Int) = Body(BodyType.STATIC, x * ConstVals.PPM.toFloat(), 0f, 30f, 30f)

    fun cellBodies(cells: IBodyCells?, x: Int): List<IBody> {
        out.clear()
        cells!!.getBodies(x, 0, out)
        return out
    }

    beforeEach {
        oldGraphics = Gdx.graphics
        frameId = 1L
        val graphics = mockk<Graphics>(relaxed = true)
        every { graphics.frameId } answers { frameId }
        Gdx.graphics = graphics
        seen.clear()
        worldBodies.clear()
    }

    afterEach {
        Gdx.graphics = oldGraphics
    }

    describe("MegaPathfinderFactory") {
        it("gives the search a snapshot of the world bodies") {
            val body = block(2)
            worldBodies.add(body)
            val factory = MegaPathfinderFactory(level(), source)

            factory.getPathfinder(params()).call().end() shouldBe IntPair(5, 0)

            seen.first().shouldNotBeNull()
            cellBodies(seen.first(), 2) shouldContain body
        }

        it("gives no snapshot when no level is loaded") {
            val factory = MegaPathfinderFactory(LevelState().apply { tiledMapLoadResult = mapResult(10) }, source)
            factory.getPathfinder(params()).call()
            seen.first().shouldBeNull()
        }

        it("uses the world size of the current map load result") {
            val levelState = level()
            val factory = MegaPathfinderFactory(levelState, source)
            factory.getPathfinder(params()).call().end() shouldBe IntPair(5, 0)

            levelState.tiledMapLoadResult = mapResult(3)

            factory.getPathfinder(params()).call().end() shouldBe IntPair(2, 0)
        }

        it("keeps one snapshot in a frame, and fills it again in a new frame") {
            val factory = MegaPathfinderFactory(level(), source)
            val first = block(1)
            worldBodies.add(first)

            factory.getPathfinder(params()).call()
            val snapshot = seen.last()
            worldBodies.clear()
            val second = block(3)
            worldBodies.add(second)
            factory.getPathfinder(params()).call()
            seen.last() shouldBeSameInstanceAs snapshot
            cellBodies(snapshot, 3) shouldNotContain second

            frameId = 2L
            factory.getPathfinder(params()).call()
            seen.last() shouldBeSameInstanceAs snapshot
            cellBodies(snapshot, 3) shouldContain second
            cellBodies(snapshot, 1) shouldNotContain first
        }

        it("does not fill a snapshot again while a search still uses it") {
            val factory = MegaPathfinderFactory(level(), source)
            val first = block(1)
            worldBodies.add(first)

            val held = factory.getPathfinder(params())
            frameId = 2L
            worldBodies.clear()
            factory.getPathfinder(params()).call()
            val second = seen.last()
            held.call()
            val heldCells = seen.last()

            heldCells shouldNotBeSameInstanceAs second
            cellBodies(heldCells, 1) shouldContain first
        }
    }
})
