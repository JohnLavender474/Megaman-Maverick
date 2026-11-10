package com.megaman.maverick.game.pathfinding

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.world.IWorldBodies
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.pathfinding.BodyCellsSnapshot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class TypedTestEntity(services: MegaServices, private val type: EntityType) : MegaGameEntity(services) {

    override fun getType() = type
}

class BlockCellsTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()

    lateinit var services: MegaServices
    val bodies = ArrayList<IBody>()
    val source = object : IWorldBodies {
        override val bodyCount get() = bodies.size
        override fun getBody(index: Int) = bodies[index]
    }

    beforeEach {
        services = testServices(engine = GameEngine())
        bodies.clear()
    }

    fun add(type: EntityType, x: Float, y: Float, width: Float, height: Float) {
        val body = Body(BodyType.STATIC, x, y, width, height)
        body.putProperty(ConstKeys.ENTITY, TypedTestEntity(services, type))
        bodies.add(body)
    }

    fun cells() = BodyCellsSnapshot(ConstVals.PPM).apply { fill(source) }

    describe("BlockCells.containsBlock") {

        it("is true for a cell with a block and false for a cell with only other bodies") {
            add(EntityType.BLOCK, 0f, 0f, ppm, ppm)
            add(EntityType.ENEMY, 2 * ppm, 0f, ppm, ppm)
            val cells = cells()

            BlockCells.containsBlock(cells, 0, 0) shouldBe true
            BlockCells.containsBlock(cells, 2, 0) shouldBe false
            BlockCells.containsBlock(cells, 5, 5) shouldBe false
        }
    }

    describe("BlockCells.containsBlockOverlappingCell") {

        it("is true only when a block's bounds overlap the cell, not when they only touch its edge") {
            add(EntityType.BLOCK, 0f, 0f, 2 * ppm, ppm)
            add(EntityType.ENEMY, 4 * ppm, 0f, ppm, ppm)
            val cells = cells()

            BlockCells.containsBlockOverlappingCell(cells, 1, 0) shouldBe true
            BlockCells.containsBlockOverlappingCell(cells, 4, 0) shouldBe false
            BlockCells.containsBlockOverlappingCell(cells, 2, 0) shouldBe false
        }
    }
})
