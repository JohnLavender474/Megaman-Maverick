package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerSlotSpawnsTest : DescribeSpec({

    val tile = ConstVals.PPM.toFloat()
    val x = 100f
    val y = 200f

    fun bounds() = GameRectangle(x, y, tile, tile)

    fun spawnBounds(slot: Int, direction: String?): GameRectangle {
        val spawn = props(ConstKeys.BOUNDS pairTo bounds())
        direction?.let { spawn.put(ConstKeys.DIRECTION, it) }
        return slotSpawnProps(PlayerSlot(slot), spawn).get(ConstKeys.BOUNDS, GameRectangle::class)!!
    }

    fun GameRectangle.position() = getX() to getY()

    describe("a spawn with no reference") {
        it("goes 1 tile right for each direction") {
            spawnBounds(1, null).position() shouldBe ((x + tile) to y)
            spawnBounds(1, "up").position() shouldBe ((x + tile) to y)
            spawnBounds(1, "down").position() shouldBe ((x - tile) to y)
            spawnBounds(1, "left").position() shouldBe (x to (y + tile))
            spawnBounds(1, "right").position() shouldBe (x to (y - tile))
        }

        it("goes one tile further for each later slot") {
            spawnBounds(2, null).position() shouldBe ((x + 2 * tile) to y)
        }
    }

    describe("a spawn with a reference") {
        val target = RectangleMapObject(500f, 600f, tile, tile)

        it("uses the target of its own slot") {
            val spawn = props(ConstKeys.BOUNDS pairTo bounds(), "${ConstKeys.PLAYER}_2" pairTo target)

            val result = slotSpawnProps(PlayerSlot(1), spawn).get(ConstKeys.BOUNDS, GameRectangle::class)!!

            result.position() shouldBe (500f to 600f)
        }

        it("ignores the target of another slot") {
            val spawn = props(ConstKeys.BOUNDS pairTo bounds(), "${ConstKeys.PLAYER}_3" pairTo target)

            val result = slotSpawnProps(PlayerSlot(1), spawn).get(ConstKeys.BOUNDS, GameRectangle::class)!!

            result.position() shouldBe ((x + tile) to y)
        }
    }

    describe("the other props") {
        it("keeps them and leaves the slot 0 props unchanged") {
            val original = bounds()
            val spawn = props(ConstKeys.BOUNDS pairTo original, ConstKeys.FACING pairTo ConstKeys.LEFT)

            val result = slotSpawnProps(PlayerSlot(1), spawn)

            result.get(ConstKeys.FACING) shouldBe ConstKeys.LEFT
            spawn.get(ConstKeys.BOUNDS, GameRectangle::class)!!.position() shouldBe (x to y)
        }
    }
})
