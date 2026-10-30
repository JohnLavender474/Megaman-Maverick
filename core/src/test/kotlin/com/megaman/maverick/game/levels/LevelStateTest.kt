package com.megaman.maverick.game.levels

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class LevelStateTest : DescribeSpec({

    describe("LevelState") {
        it("starts with the values that the game gave for a missing key") {
            val state = LevelState()
            state.currentLevel.shouldBeNull()
            state.roomsSupplier.shouldBeNull()
            state.currentRoomSupplier.shouldBeNull()
            state.worldContainer.shouldBeNull()
            state.tiledMapLoadResult.shouldBeNull()
            state.roomTransition shouldBe false
            state.megamanBeam shouldBe false
            state.megamanBeamCenter.shouldBeNull()
            state.coins shouldBe 0
            state.levelPoints shouldBe 0
            state.timeSinceLastFloatingPoints shouldBe 0f
            state.lastFloatingPointsType.shouldBeNull()
        }

        it("keeps the beam center object that was set, not a copy") {
            val state = LevelState()
            val center = Vector2(1f, 2f)
            state.megamanBeamCenter = center
            center.set(3f, 4f)
            state.megamanBeamCenter shouldBeSameInstanceAs center
        }
    }

    describe("the LevelState reads before the first write") {
        val state = LevelState()

        it("currentLevel and tiledMapLoadResult throw when read with !!") {
            shouldThrow<NullPointerException> { state.currentLevel!! }
            shouldThrow<NullPointerException> { state.tiledMapLoadResult!! }
        }

        it("the other reads give the empty values") {
            state.isInExtraLevel shouldBe false
            val out = Array<RectangleMapObject>()
            state.getRooms(out) shouldBeSameInstanceAs out
            out.size shouldBe 0
            state.currentRoom.shouldBeNull()
            state.worldContainer.shouldBeNull()
            state.weaponsAttained.shouldBeNull()
        }
    }

    describe("the LevelState reads after a write") {
        it("call the room suppliers at each read and do not copy the rooms") {
            val state = LevelState()
            val rooms = Array<RectangleMapObject>()
            var current: RectangleMapObject? = null
            state.roomsSupplier = { rooms }
            state.currentRoomSupplier = { current }

            state.getRooms(Array()).size shouldBe 0
            state.currentRoom.shouldBeNull()

            val room = RectangleMapObject()
            rooms.add(room)
            current = room

            state.getRooms(Array()).first() shouldBeSameInstanceAs room
            state.currentRoom shouldBeSameInstanceAs room
        }

        it("keep the level values when a new level is set") {
            val state = LevelState()
            val rooms = Array<RectangleMapObject>().apply { add(RectangleMapObject()) }
            state.currentLevel = LevelDefinition.TEST_1
            state.roomsSupplier = { rooms }

            state.currentLevel = LevelDefinition.GAUNTLET_1

            state.currentLevel shouldBe LevelDefinition.GAUNTLET_1
            state.isInExtraLevel shouldBe true
            state.getRooms(Array()).size shouldBe 1
        }
    }

    describe("the game") {
        it("holds one LevelState for the session") {
            val game = MegamanMaverickGame()
            game.levelState shouldBeSameInstanceAs game.levelState
        }
    }
})
