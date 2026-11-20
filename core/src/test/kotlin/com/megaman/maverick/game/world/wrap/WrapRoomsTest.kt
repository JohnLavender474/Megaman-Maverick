package com.megaman.maverick.game.world.wrap

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val VIEW_PX = ConstVals.VIEW_WIDTH * ConstVals.PPM

private fun room(name: String, x: Float, width: Float, wrap: Boolean?) =
    RectangleMapObject(x, 64f, width, 448f).also {
        it.name = name
        if (wrap != null) it.properties.put(ConstKeys.WRAP, wrap)
    }

class WrapRoomsTest : DescribeSpec({

    describe("checkWrapRoomWidths") {
        it("passes a wrap room that is one view wide") {
            checkWrapRoomWidths(Array.with(room("arena", 32f, VIEW_PX, wrap = true)))
        }

        it("throws for a wrap room that is wider than one view") {
            val wide = room("wide", 32f, VIEW_PX + ConstVals.PPM, wrap = true)
            val error = shouldThrow<InvalidWrapRoomWidthException> {
                checkWrapRoomWidths(Array.with(room("arena", 32f, VIEW_PX, wrap = true), wide))
            }
            error.room shouldBeSameInstanceAs wide
        }

        it("passes a room with no wrap or wrap=false of any width") {
            checkWrapRoomWidths(
                Array.with(
                    room("plain", 0f, VIEW_PX * 3f, wrap = null),
                    room("off", 0f, VIEW_PX / 2f, wrap = false)
                )
            )
        }
    }

    describe("wrapRegion") {
        val out = GameRectangle()

        it("gives the bounds of a current wrap room") {
            val arena = room("arena", 32f, VIEW_PX, wrap = true)
            val levelState = LevelState().apply { currentRoomSupplier = { arena } }
            val region = levelState.wrapRegion(out)!!
            region shouldBeSameInstanceAs out
            region.getX() shouldBe 32f
            region.getWidth() shouldBe VIEW_PX
        }

        it("gives null for a room with no wrap, and with no room") {
            val plain = room("plain", 0f, VIEW_PX, wrap = null)
            LevelState().apply { currentRoomSupplier = { plain } }.wrapRegion(out).shouldBeNull()
            LevelState().apply { currentRoomSupplier = { null } }.wrapRegion(out).shouldBeNull()
            LevelState().wrapRegion(out).shouldBeNull()
        }
    }
})
