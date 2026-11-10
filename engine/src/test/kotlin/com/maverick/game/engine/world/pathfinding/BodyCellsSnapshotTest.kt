package com.maverick.game.engine.world.pathfinding

import com.maverick.game.engine.world.IWorldBodies
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeEmpty

class BodyCellsSnapshotTest : DescribeSpec({

    val ppm = 10

    class TestBodies(val list: ArrayList<IBody> = ArrayList()) : IWorldBodies {
        override val bodyCount get() = list.size
        override fun getBody(index: Int) = list[index]
    }

    describe("BodyCellsSnapshot") {

        it("holds each body in the cells that its bounds cover") {
            val body = Body(BodyType.STATIC, 10f, 10f, 10f, 10f)
            val source = TestBodies(arrayListOf(body))
            val snapshot = BodyCellsSnapshot(ppm)

            snapshot.fill(source)

            val out = ArrayList<IBody>()
            snapshot.getBodies(1, 1, out)
            out shouldContainExactly listOf(body)
            out.clear()
            snapshot.getBodies(3, 3, out)
            out.shouldBeEmpty()
        }

        it("keeps its bodies when the source changes, until the next fill") {
            val first = Body(BodyType.STATIC, 10f, 10f, 10f, 10f)
            val second = Body(BodyType.STATIC, 30f, 30f, 10f, 10f)
            val source = TestBodies(arrayListOf(first))
            val snapshot = BodyCellsSnapshot(ppm)
            snapshot.fill(source)

            source.list.clear()
            source.list.add(second)

            val out = ArrayList<IBody>()
            snapshot.getBodies(1, 1, out)
            out shouldContainExactly listOf(first)

            snapshot.fill(source)
            out.clear()
            snapshot.getBodies(1, 1, out)
            out.shouldBeEmpty()
            snapshot.getBodies(3, 3, out)
            out shouldContainExactly listOf(second)
        }
    }
})
